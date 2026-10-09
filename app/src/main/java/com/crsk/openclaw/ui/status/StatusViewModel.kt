package com.crsk.openclaw.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crsk.openclaw.accessibility.A11yStatus
import com.crsk.openclaw.accessibility.AccessibilityServiceHolder
import com.crsk.openclaw.bootstrap.GatewayProcess
import com.crsk.openclaw.bootstrap.GatewayStatus
import com.crsk.openclaw.data.composio.ComposioRepository
import com.crsk.openclaw.data.composio.Connection
import com.crsk.openclaw.data.composio.ConnectionStatus
import com.crsk.openclaw.data.composio.Toolkit
import com.crsk.openclaw.data.preferences.AppPreferences
import com.crsk.openclaw.service.GatewayService
import com.crsk.openclaw.shizuku.ShizukuManager
import com.crsk.openclaw.shizuku.ShizukuStatus
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.crsk.openclaw.data.network.ws.WsRpcClient
import org.json.JSONObject
import javax.inject.Inject

/** Which on-device capabilities are wired up. Each is shown as a logo bubble. */
data class ConnectionsUi(
    val gateway: Boolean = false,
    val model: Boolean = false,
    val uiAutomation: Boolean = false,
    val shell: Boolean = false,
    val plugins: Boolean = false,
)

/** A toolkit row on the Status page — combines the catalog entry with whether this device
 *  is currently connected to it. */
data class ToolkitChip(
    val toolkit: Toolkit,
    val isConnected: Boolean,
    val isPending: Boolean,
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val gatewayProcess: GatewayProcess,
    accessibilityHolder: AccessibilityServiceHolder,
    shizukuManager: ShizukuManager,
    private val preferences: AppPreferences,
    private val composio: ComposioRepository,
    private val nodeProcess: com.crsk.openclaw.node.NodeProcess,
    private val rpc: WsRpcClient,
) : ViewModel() {

    // Memory only: never persist account usage or retain it after a provider change.
    private val _usage = MutableStateFlow(UsageUiState())
    val usage: StateFlow<UsageUiState> = _usage.asStateFlow()
    private var usageJob: Job? = null

    fun refreshUsage() {
        val running = gatewayProcess.status.value as? GatewayStatus.Running
        val unavailable = when {
            preferences.apiProvider.value != "openai-codex" -> "Usage and billing follow your provider’s plan."
            !nodeProcess.hasCodexAuthProfile() -> "Sign in with ChatGPT in Settings to see your usage."
            running == null -> "Start the gateway to see your usage."
            else -> null
        }
        if (unavailable != null) {
            usageJob?.cancel()
            _usage.value = UsageUiState(message = unavailable)
            return
        }
        if (usageJob?.isActive == true) return
        usageJob = viewModelScope.launch {
            _usage.value = UsageUiState(loading = true)
            try {
                rpc.ensureConnected(running!!.port)
                val response = rpc.call("usage.status", JSONObject(), 15_000)
                val snapshot = if (response.ok) response.payload?.let(PlanUsage::parse) else null
                _usage.value = if (snapshot != null) UsageUiState(usage = snapshot)
                    else UsageUiState(message = "Usage isn't available right now. Try again.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // No raw server error: it may contain account or request details.
                _usage.value = UsageUiState(message = "Couldn't load usage. Try again.")
            }
        }
    }

    fun stopUsageRefresh() {
        usageJob?.cancel()
        _usage.value = _usage.value.copy(loading = false)
    }

    // Read-only local credential presence. Refresh on opening Status;
    // never show credentials or OAuth secrets in diagnostic output.
    private val codexProfilePresent = MutableStateFlow(nodeProcess.hasCodexAuthProfile())
    val codexSelected = preferences.apiProvider
        .map { it == "openai-codex" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private val modelConfigured = combine(
        preferences.apiProvider, preferences.selectedProviderHasKey, codexProfilePresent
    ) { provider, hasKey, hasProfile ->
        if (provider == "openai-codex") hasProfile else hasKey
    }

    val connections: StateFlow<ConnectionsUi> = combine(
        gatewayProcess.status,
        gatewayProcess.mcpReady,
        accessibilityHolder.status,
        shizukuManager.status,
        modelConfigured,
    ) { gateway, mcpReady, a11y, shizuku, hasKey ->
        ConnectionsUi(
            gateway = gateway is GatewayStatus.Running,
            model = hasKey,
            uiAutomation = a11y == A11yStatus.CONNECTED,
            shell = shizuku is ShizukuStatus.Ready,
            plugins = mcpReady,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionsUi())

    /** Curated short list of popular integrations to show inline on Status. Composio
     *  exposes hundreds, but we don't want to render 300 chips here. The full list is
     *  one tap away via the "Browse all" tile. */
    private val featuredSlugs = listOf(
        "gmail", "googlecalendar", "googledrive", "notion", "slack",
        "github", "linear", "discord", "twitter", "linkedin", "spotify", "youtube",
    )

    val featuredToolkits: StateFlow<List<ToolkitChip>> = combine(
        composio.catalog,
        composio.connections,
    ) { catalog, connections ->
        // Slugs come from Composio in mixed case (catalog: lowercase, connections: uppercase
        // pre-fix). Lowercase everything at comparison time so the match is bulletproof
        // regardless of what casing arrives.
        val active = connections.filter { it.status == ConnectionStatus.ACTIVE }
            .map { it.toolkitSlug.lowercase() }.toSet()
        val pending = connections.filter { it.status == ConnectionStatus.INITIATED }
            .map { it.toolkitSlug.lowercase() }.toSet()
        val bySlug = catalog.associateBy { it.slug.lowercase() }
        featuredSlugs.mapNotNull { slug ->
            val tk = bySlug[slug] ?: return@mapNotNull null
            val key = tk.slug.lowercase()
            ToolkitChip(
                toolkit = tk,
                isConnected = active.contains(key),
                isPending = pending.contains(key),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Emits a redirect URL the screen should open in Chrome Custom Tab. */
    private val _pendingRedirect = MutableStateFlow<String?>(null)
    val pendingRedirect: StateFlow<String?> = _pendingRedirect.asStateFlow()
    fun consumeRedirect() { _pendingRedirect.value = null }

    /** User-facing error from a connect attempt. Screen shows as Toast and clears it. */
    private val _connectError = MutableStateFlow<String?>(null)
    val connectError: StateFlow<String?> = _connectError.asStateFlow()
    fun consumeConnectError() { _connectError.value = null }

    /** Kick off an OAuth connection for one of the featured chips. Surface failures
     *  through `connectError` so the user sees feedback instead of a silent no-op. */
    fun connect(toolkitSlug: String) {
        viewModelScope.launch {
            val init = runCatching { composio.startConnect(toolkitSlug) }
                .onFailure { _connectError.value = "Couldn't reach Composio: ${it.message ?: it.javaClass.simpleName}" }
                .getOrNull()
            if (init != null) {
                _pendingRedirect.value = init.redirectUrl
            } else if (_connectError.value == null) {
                _connectError.value = "Couldn't start $toolkitSlug. Composio rejected the request — check your Composio key in Settings."
            }
        }
    }

    /** Called by the screen ON_RESUME after the OAuth tab. Pulls fresh statuses + restarts
     *  the gateway if a new connection went ACTIVE. */
    fun onResumeFromOAuth() {
        codexProfilePresent.value = nodeProcess.hasCodexAuthProfile()
        viewModelScope.launch {
            runCatching { composio.syncConnections() }
            val url = runCatching { composio.refreshMcpUrl() }.getOrNull()
            if (!url.isNullOrBlank()) {
                runCatching {
                    GatewayService.stop(appContext)
                    GatewayService.start(appContext)
                }
            }
        }
    }

    /** Pull a fresh toolkit catalog on demand. */
    fun refreshCatalog() = composio.refreshCatalog()

    init {
        composio.refreshCatalog()
        viewModelScope.launch { runCatching { composio.syncConnections() } }
    }
}
