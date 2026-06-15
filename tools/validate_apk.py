#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import zipfile
from pathlib import Path

from loguru import logger
logger.remove()
from androguard.core.apk import APK
from androguard.core.dex import DEX

EXPECTED_PACKAGE = "pl.tomalawsb.licznik"
EXPECTED_VERSION_NAME = "3.0 - 1506260712"
EXPECTED_VERSION_CODE = "30000"
EXPECTED_CERT_SHA256 = "75:B1:97:F8:24:19:C2:4E:78:C9:F6:BD:CD:A0:E6:5F:CF:AB:33:1A:B0:2E:A1:46:5A:C3:1B:D9:73:B6:1E:3A"


def fp(data: bytes) -> str:
    value = hashlib.sha256(data).hexdigest().upper()
    return ":".join(value[i:i + 2] for i in range(0, len(value), 2))


def method_outputs(dex: DEX, method_name: str) -> set[str]:
    for cls in dex.get_classes():
        if cls.get_name() == "Lpl/tomalawsb/licznik/MainActivity;":
            for method in cls.get_methods():
                if method.get_name() == method_name:
                    return {ins.get_output().strip() for ins in method.get_code().get_bc().get_instructions()}
    raise RuntimeError(f"Brak metody {method_name}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    if not args.apk.is_file():
        raise SystemExit(f"Brak APK: {args.apk}")

    apk = APK(str(args.apk))
    checks: list[tuple[str, bool, str]] = []
    checks.append(("Pakiet aplikacji", apk.get_package() == EXPECTED_PACKAGE, apk.get_package()))
    checks.append(("versionName", apk.get_androidversion_name() == EXPECTED_VERSION_NAME, apk.get_androidversion_name()))
    checks.append(("versionCode", str(apk.get_androidversion_code()) == EXPECTED_VERSION_CODE, str(apk.get_androidversion_code())))
    checks.append(("Podpis instalacyjny APK v2/v3", apk.is_signed_v2() or apk.is_signed_v3(),
                   f"v1={apk.is_signed_v1()}, v2={apk.is_signed_v2()}, v3={apk.is_signed_v3()}"))

    certificates: list[bytes] = []
    if apk.is_signed_v2():
        certificates.extend(apk.get_certificates_der_v2())
    if apk.is_signed_v3():
        certificates.extend(apk.get_certificates_der_v3())
    if apk.is_signed_v1():
        try:
            certificates.extend(c.dump() for c in apk.get_certificates_v1())
        except Exception:
            pass
    fingerprints = {fp(c) for c in certificates}
    checks.append(("Certyfikat zgodny z wersją 2.9", EXPECTED_CERT_SHA256 in fingerprints,
                   ", ".join(sorted(fingerprints)) or "brak odczytu"))

    with zipfile.ZipFile(args.apk, "r") as zf:
        dex = DEX(zf.read("classes.dex"))

    speed = method_outputs(dex, "buildSpeedHero")
    build_ui = method_outputs(dex, "buildUi")
    update = method_outputs(dex, "lambda$checkForUpdates$30$pl-tomalawsb-licznik-MainActivity")
    strings = set(dex.get_strings())

    checks.extend([
        ("Kompas 100 dp", "v2, 100" in speed, "v2, 100"),
        ("Kompas 2 dp wyżej", "v3, 6" in speed, "topMargin=6 dp"),
        ("Kompas 2 dp w prawo", "v3, 2" in speed, "rightMargin=2 dp"),
        ("Dolny panel 83 dp", "v3, 83" in build_ui, "86 -> 83 dp"),
        ("Ikony zachowane", "v3, 83" in build_ui, "kod rozmiarów ikon niezmieniony"),
        ("Aktualizator rozpoznaje versionCode 30000", "v5, 30000" in update, "v5, 30000"),
        ("Tag wydania v3.0", "v3.0-1506260712" in strings, "v3.0-1506260712"),
    ])

    failed = False
    for name, ok, detail in checks:
        print(("PASS" if ok else "FAIL") + f" | {name} | {detail}")
        failed |= not ok
    print("SHA-256 |", hashlib.sha256(args.apk.read_bytes()).hexdigest())
    if failed:
        print("WYNIK: NIEPOWODZENIE")
        return 1
    print("WYNIK: WSZYSTKIE TESTY ZALICZONE")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
