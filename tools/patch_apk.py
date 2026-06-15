#!/usr/bin/env python3
"""Tworzy wersję Licznik 3.0 przez bezpieczną modyfikację bazowego APK 2.9.

Zmiany dotyczą wyłącznie wymiarów interfejsu oraz numeru wersji.
Logika GPS, kompasu, trybów jazdy, historii i mapy pozostaje z APK 2.9.
"""
from __future__ import annotations

import argparse
import hashlib
import os
import shutil
import struct
import tempfile
import zipfile
import zlib
from pathlib import Path

from androguard.core.dex import DEX

OLD_VERSION_NAME = "2.9 - 1406262155"
NEW_VERSION_NAME = "3.0 - 1506260712"
OLD_RELEASE_TAG = "v2.9-1406262155"
NEW_RELEASE_TAG = "v3.0-1506260712"
OLD_VERSION_CODE = 20900
NEW_VERSION_CODE = 30000


def patch_const16(raw: bytearray, code_off: int, ins_off: int, expected: int, new_value: int) -> None:
    pos = code_off + 16 + ins_off
    if raw[pos] != 0x13:
        raise RuntimeError(f"Oczekiwano const/16 pod 0x{pos:X}, znaleziono 0x{raw[pos]:02X}")
    current = struct.unpack_from("<h", raw, pos + 2)[0]
    if current != expected:
        raise RuntimeError(f"Oczekiwano wartości {expected} pod 0x{pos:X}, znaleziono {current}")
    struct.pack_into("<h", raw, pos + 2, new_value)


def patch_const4(raw: bytearray, code_off: int, ins_off: int, expected: int, new_value: int) -> None:
    pos = code_off + 16 + ins_off
    if raw[pos] != 0x12:
        raise RuntimeError(f"Oczekiwano const/4 pod 0x{pos:X}, znaleziono 0x{raw[pos]:02X}")
    second = raw[pos + 1]
    current = (second >> 4) & 0x0F
    if current >= 8:
        current -= 16
    if current != expected:
        raise RuntimeError(f"Oczekiwano wartości {expected} pod 0x{pos:X}, znaleziono {current}")
    raw[pos + 1] = ((new_value & 0x0F) << 4) | (second & 0x0F)


def find_method(dex: DEX, name: str):
    for cls in dex.get_classes():
        if cls.get_name() != "Lpl/tomalawsb/licznik/MainActivity;":
            continue
        for method in cls.get_methods():
            if method.get_name() == name:
                return method
    raise RuntimeError(f"Nie znaleziono metody MainActivity.{name}")


def instruction_offsets(method):
    result = []
    off = 0
    for ins in method.get_code().get_bc().get_instructions():
        result.append((off, ins))
        off += ins.get_length()
    return result


def select_instruction(method, ins_name: str, output: str) -> int:
    matches = [off for off, ins in instruction_offsets(method)
               if ins.get_name() == ins_name and ins.get_output().strip() == output]
    if len(matches) != 1:
        raise RuntimeError(
            f"Metoda {method.get_name()}: oczekiwano jednej instrukcji "
            f"{ins_name} {output!r}, znaleziono {len(matches)}"
        )
    return matches[0]


def refresh_dex_integrity(raw: bytearray) -> None:
    raw[12:32] = hashlib.sha1(raw[32:]).digest()
    struct.pack_into("<I", raw, 8, zlib.adler32(raw[12:]) & 0xFFFFFFFF)


def patch_dex(data: bytes) -> bytes:
    dex = DEX(data)
    raw = bytearray(data)

    speed = find_method(dex, "buildSpeedHero")
    build_ui = find_method(dex, "buildUi")
    update_check = find_method(dex, "lambda$checkForUpdates$30$pl-tomalawsb-licznik-MainActivity")

    # Kompas: 92 -> 100 dp. Ten sam rejestr określa szerokość, wysokość
    # i rezerwę po prawej stronie treści, dzięki czemu kompas nie nachodzi na prędkość.
    off = select_instruction(speed, "const/16", "v2, 92")
    patch_const16(raw, speed.code_off, off, 92, 100)

    # Przesunięcie kompasu dokładnie o 2 dp do góry i 2 dp w prawo.
    off = select_instruction(speed, "const/16", "v3, 8")
    patch_const16(raw, speed.code_off, off, 8, 6)
    off = select_instruction(speed, "const/4", "v3, 4")
    patch_const4(raw, speed.code_off, off, 4, 2)

    # Cały dolny panel ma 83 dp zamiast 86 dp. Ikony zachowują 36 sp.
    off = select_instruction(build_ui, "const/16", "v3, 86")
    patch_const16(raw, build_ui.code_off, off, 86, 83)

    # Numer wersji używany przez mechanizm automatycznej aktualizacji.
    off = select_instruction(update_check, "const/16", "v5, 20900")
    patch_const16(raw, update_check.code_off, off, OLD_VERSION_CODE, NEW_VERSION_CODE)

    # Stała CURRENT_VERSION_CODE w encoded static values.
    old_encoded = bytes((0x24, OLD_VERSION_CODE & 0xFF, (OLD_VERSION_CODE >> 8) & 0xFF))
    new_encoded = bytes((0x24, NEW_VERSION_CODE & 0xFF, (NEW_VERSION_CODE >> 8) & 0xFF))
    if raw.count(old_encoded) != 1:
        raise RuntimeError("Nie znaleziono jednoznacznie stałej CURRENT_VERSION_CODE")
    raw = bytearray(bytes(raw).replace(old_encoded, new_encoded, 1))

    # Napisy wersji mają identyczną długość, więc nie zmieniają układu DEX.
    old_name = OLD_VERSION_NAME.encode("utf-8")
    new_name = NEW_VERSION_NAME.encode("utf-8")
    old_tag = OLD_RELEASE_TAG.encode("utf-8")
    new_tag = NEW_RELEASE_TAG.encode("utf-8")
    if len(old_name) != len(new_name) or len(old_tag) != len(new_tag):
        raise RuntimeError("Nowe identyfikatory wersji muszą mieć identyczną długość")
    if bytes(raw).count(old_name) != 3:
        raise RuntimeError("Nieoczekiwana liczba napisów wersji w classes.dex")
    if bytes(raw).count(old_tag) != 1:
        raise RuntimeError("Nieoczekiwana liczba tagów wydania w classes.dex")
    raw = bytearray(bytes(raw).replace(old_name, new_name))
    raw = bytearray(bytes(raw).replace(old_tag, new_tag))

    refresh_dex_integrity(raw)
    return bytes(raw)


def patch_manifest(data: bytes) -> bytes:
    raw = bytearray(data)
    old_utf16 = OLD_VERSION_NAME.encode("utf-16le")
    new_utf16 = NEW_VERSION_NAME.encode("utf-16le")
    if len(old_utf16) != len(new_utf16):
        raise RuntimeError("Nazwa wersji manifestu musi mieć identyczną długość")
    if raw.count(old_utf16) != 1:
        raise RuntimeError("Nie znaleziono jednoznacznie versionName w AndroidManifest.xml")
    raw = bytearray(bytes(raw).replace(old_utf16, new_utf16, 1))

    old_code = struct.pack("<I", OLD_VERSION_CODE)
    new_code = struct.pack("<I", NEW_VERSION_CODE)
    if raw.count(old_code) != 1:
        raise RuntimeError("Nie znaleziono jednoznacznie versionCode w AndroidManifest.xml")
    raw = bytearray(bytes(raw).replace(old_code, new_code, 1))
    return bytes(raw)


def copy_zip_with_changes(source: Path, output: Path, changed: dict[str, bytes]) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(source, "r") as zin, zipfile.ZipFile(output, "w", allowZip64=True) as zout:
        for info in zin.infolist():
            # Stare podpisy v1, jeśli wystąpią, nie mogą przejść do nowego APK.
            upper = info.filename.upper()
            if upper.startswith("META-INF/") and upper.endswith((".RSA", ".DSA", ".EC", ".SF", "MANIFEST.MF")):
                continue
            payload = changed.get(info.filename, zin.read(info.filename))
            new_info = zipfile.ZipInfo(info.filename, info.date_time)
            new_info.compress_type = info.compress_type
            new_info.comment = info.comment
            new_info.extra = info.extra
            new_info.internal_attr = info.internal_attr
            new_info.external_attr = info.external_attr
            new_info.create_system = info.create_system
            new_info.flag_bits = info.flag_bits & ~0x08
            if info.compress_type == zipfile.ZIP_DEFLATED:
                zout.writestr(new_info, payload, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
            else:
                zout.writestr(new_info, payload, compress_type=info.compress_type)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    if not args.input.is_file():
        raise SystemExit(f"Brak bazowego APK: {args.input}")

    with zipfile.ZipFile(args.input, "r") as apk:
        dex = apk.read("classes.dex")
        manifest = apk.read("AndroidManifest.xml")

    patched_dex = patch_dex(dex)
    patched_manifest = patch_manifest(manifest)
    copy_zip_with_changes(args.input, args.output, {
        "classes.dex": patched_dex,
        "AndroidManifest.xml": patched_manifest,
    })

    print(f"OK: utworzono niepodpisany APK: {args.output}")
    print(f"Wersja: {NEW_VERSION_NAME}, versionCode: {NEW_VERSION_CODE}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
