#!/usr/bin/env python3
"""Reject APKs with malformed ELF PT_LOAD alignment (found in v2 APK).

A native ELF PT_LOAD offset must be congruent with its virtual address modulo
p_align. The first 4AIs PTY build had violations in libnode.so and two other
libraries, which can cause loader failures or runtime crashes.
"""
import struct
import sys
from zipfile import ZipFile

def verify_elf(label, blob):
    if not blob.startswith(bytes((127, 69, 76, 70))):
        raise ValueError(f"{label}: not ELF")
    if blob[4] != 2 or blob[5] != 1:
        raise ValueError(f"{label}: expected ELF64 little-endian")
    e_phoff = struct.unpack_from("<Q", blob, 32)[0]
    e_phentsize = struct.unpack_from("<H", blob, 54)[0]
    e_phnum = struct.unpack_from("<H", blob, 56)[0]
    if e_phentsize < 56 or e_phoff + e_phentsize * e_phnum > len(blob):
        raise ValueError(f"{label}: invalid program header table")
    nloads = 0
    for i in range(e_phnum):
        phoff = e_phoff + i * e_phentsize
        p_type, p_flags, p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align = (
            struct.unpack_from("<IIQQQQQQ", blob, phoff)
        )
        if p_type != 1:
            continue
        nloads += 1
        if p_align > 1 and (p_offset - p_vaddr) % p_align:
            raise ValueError(
                f"{label}: PT_LOAD {i} offset {p_offset:#x} "
                f"vaddr {p_vaddr:#x} NOT congruent modulo {p_align:#x}"
            )
    if not nloads:
        raise ValueError(f"{label}: no PT_LOAD segments")
    return nloads

if __name__ == "__main__":
    total = 0
    for apk in sys.argv[1:]:
        with ZipFile(apk) as z:
            libs = sorted(p for p in z.namelist() if p.startswith("lib/") and p.endswith(".so"))
            if not libs:
                raise SystemExit("APK has no native libraries: " + apk)
            for p in libs:
                try:
                    count = verify_elf(p, z.read(p))
                except ValueError as e:
                    raise SystemExit("FAILED: " + str(e))
                total += 1
                print(f"OK {p}: {count} PT_LOAD")
    print(f"PASS: {total} native ELF libraries satisfy alignment")
