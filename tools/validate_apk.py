#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import struct
import zipfile
from pathlib import Path

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from loguru import logger

logger.remove()
from androguard.core.apk import APK
from androguard.core.dex import DEX

EXPECTED_PACKAGE = "pl.tomalawsb.licznik"
EXPECTED_VERSION_NAME = "3.1 - 1606260712"
EXPECTED_VERSION_CODE = 30100
EXPECTED_RELEASE_TAG = "v3.1-1606260712"
BASELINE_NAME = "Licznik-v2.9-1406262155.apk"
EXPECTED_CERT_SHA256 = "75:B1:97:F8:24:19:C2:4E:78:C9:F6:BD:CD:A0:E6:5F:CF:AB:33:1A:B0:2E:A1:46:5A:C3:1B:D9:73:B6:1E:3A"

APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871A
APK_SIG_MAGIC = b"APK Sig Block 42"
SIG_ALG_RSA_PKCS1_SHA256 = 0x0103
EOCD_MAGIC = b"PK\x05\x06"
CHUNK_SIZE = 1024 * 1024


def fingerprint(data: bytes) -> str:
    value = hashlib.sha256(data).hexdigest().upper()
    return ":".join(value[i:i + 2] for i in range(0, len(value), 2))


def read_lp(data: bytes, offset: int = 0) -> tuple[bytes, int]:
    if offset + 4 > len(data):
        raise RuntimeError("Niepełne pole długości w bloku podpisu.")
    length = struct.unpack_from("<I", data, offset)[0]
    offset += 4
    end = offset + length
    if end > len(data):
        raise RuntimeError("Pole długości wychodzi poza blok podpisu.")
    return data[offset:end], end


def chunk_digest(chunk: bytes) -> bytes:
    return hashlib.sha256(b"\xA5" + struct.pack("<I", len(chunk)) + chunk).digest()


def split_chunks(data: bytes):
    for start in range(0, len(data), CHUNK_SIZE):
        yield data[start:start + CHUNK_SIZE]


def calculate_content_digest(apk: bytes, block_start: int, cd_offset: int, eocd_offset: int) -> bytes:
    digests: list[bytes] = []
    digests.extend(chunk_digest(chunk) for chunk in split_chunks(apk[:block_start]))
    digests.extend(chunk_digest(chunk) for chunk in split_chunks(apk[cd_offset:eocd_offset]))

    eocd = bytearray(apk[eocd_offset:])
    eocd[16:20] = struct.pack("<I", block_start)
    digests.extend(chunk_digest(chunk) for chunk in split_chunks(bytes(eocd)))

    return hashlib.sha256(
        b"\x5A" + struct.pack("<I", len(digests)) + b"".join(digests)
    ).digest()


def verify_v2_signature(apk_path: Path) -> tuple[bytes, str]:
    apk = apk_path.read_bytes()
    search_start = max(0, len(apk) - 65557)
    eocd_offset = apk.rfind(EOCD_MAGIC, search_start)
    if eocd_offset < 0 or eocd_offset + 22 > len(apk):
        raise RuntimeError("Nie znaleziono prawidłowego EOCD.")

    cd_offset = struct.unpack_from("<I", apk, eocd_offset + 16)[0]
    if cd_offset < 24 or apk[cd_offset - 16:cd_offset] != APK_SIG_MAGIC:
        raise RuntimeError("Brak bloku APK Signature Scheme v2/v3.")

    second_size = struct.unpack_from("<Q", apk, cd_offset - 24)[0]
    block_start = cd_offset - second_size - 8
    if block_start < 0:
        raise RuntimeError("Nieprawidłowy początek bloku podpisu.")

    first_size = struct.unpack_from("<Q", apk, block_start)[0]
    if first_size != second_size:
        raise RuntimeError("Niezgodne rozmiary bloku podpisu.")

    pairs = apk[block_start + 8:cd_offset - 24]
    position = 0
    v2_value: bytes | None = None
    while position < len(pairs):
        if position + 12 > len(pairs):
            raise RuntimeError("Uszkodzona para ID/wartość w bloku podpisu.")
        pair_length = struct.unpack_from("<Q", pairs, position)[0]
        position += 8
        pair_id = struct.unpack_from("<I", pairs, position)[0]
        position += 4
        value_length = pair_length - 4
        if value_length < 0 or position + value_length > len(pairs):
            raise RuntimeError("Nieprawidłowa długość pary w bloku podpisu.")
        value = pairs[position:position + value_length]
        position += value_length
        if pair_id == APK_SIGNATURE_SCHEME_V2_BLOCK_ID:
            v2_value = value

    if v2_value is None:
        raise RuntimeError("Brak podpisu APK Signature Scheme v2.")

    signers, end = read_lp(v2_value)
    if end != len(v2_value):
        raise RuntimeError("Nadmiarowe dane po sekwencji podpisujących.")

    signer, signer_end = read_lp(signers)
    if signer_end != len(signers):
        raise RuntimeError("Obsługiwany jest dokładnie jeden podpisujący.")

    signed_data, offset = read_lp(signer)
    signatures, offset = read_lp(signer, offset)
    public_key_der, offset = read_lp(signer, offset)
    if offset != len(signer):
        raise RuntimeError("Nadmiarowe dane w rekordzie podpisującego.")

    digests, offset = read_lp(signed_data)
    certificates, offset = read_lp(signed_data, offset)
    _attributes, offset = read_lp(signed_data, offset)
    if any(signed_data[offset:]):
        raise RuntimeError("Niezerowe nadmiarowe dane w signed-data.")

    digest_record, digest_end = read_lp(digests)
    if digest_end != len(digests):
        raise RuntimeError("Obsługiwany jest dokładnie jeden algorytm skrótu.")
    digest_algorithm = struct.unpack_from("<I", digest_record, 0)[0]
    embedded_digest, digest_record_end = read_lp(digest_record, 4)
    if digest_record_end != len(digest_record):
        raise RuntimeError("Uszkodzony rekord skrótu.")

    signature_record, signature_end = read_lp(signatures)
    if signature_end != len(signatures):
        raise RuntimeError("Obsługiwany jest dokładnie jeden podpis.")
    signature_algorithm = struct.unpack_from("<I", signature_record, 0)[0]
    signature, signature_record_end = read_lp(signature_record, 4)
    if signature_record_end != len(signature_record):
        raise RuntimeError("Uszkodzony rekord podpisu.")

    certificate_der, certificate_end = read_lp(certificates)
    if certificate_end != len(certificates):
        raise RuntimeError("Obsługiwany jest dokładnie jeden certyfikat.")

    if digest_algorithm != SIG_ALG_RSA_PKCS1_SHA256 or signature_algorithm != SIG_ALG_RSA_PKCS1_SHA256:
        raise RuntimeError("Nieobsługiwany algorytm podpisu APK.")

    certificate = x509.load_der_x509_certificate(certificate_der)
    certificate_public_key_der = certificate.public_key().public_bytes(
        serialization.Encoding.DER,
        serialization.PublicFormat.SubjectPublicKeyInfo,
    )
    if certificate_public_key_der != public_key_der:
        raise RuntimeError("Klucz publiczny nie odpowiada certyfikatowi.")

    certificate.public_key().verify(
        signature,
        signed_data,
        padding.PKCS1v15(),
        hashes.SHA256(),
    )

    calculated_digest = calculate_content_digest(apk, block_start, cd_offset, eocd_offset)
    if calculated_digest != embedded_digest:
        raise RuntimeError("Skrót zawartości APK nie zgadza się z podpisem.")

    return certificate_der, fingerprint(certificate_der)


def method_outputs(dex: DEX, method_name: str) -> set[str]:
    for cls in dex.get_classes():
        if cls.get_name() == "Lpl/tomalawsb/licznik/MainActivity;":
            for method in cls.get_methods():
                if method.get_name() == method_name:
                    return {
                        ins.get_output().strip()
                        for ins in method.get_code().get_bc().get_instructions()
                    }
    raise RuntimeError(f"Brak metody {method_name}")


def find_baseline(apk_path: Path) -> Path:
    candidates = [
        apk_path.parent / "baseline" / BASELINE_NAME,
        apk_path.parent.parent / "baseline" / BASELINE_NAME,
        Path.cwd() / "baseline" / BASELINE_NAME,
    ]
    for candidate in candidates:
        if candidate.is_file():
            return candidate
    raise RuntimeError(f"Brak bazowego APK 2.9: {BASELINE_NAME}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    if not args.apk.is_file():
        raise SystemExit(f"Brak APK: {args.apk}")

    checks: list[tuple[str, bool, str]] = []
    failed = False

    try:
        apk = APK(str(args.apk))
        checks.append(("Pakiet aplikacji", apk.get_package() == EXPECTED_PACKAGE, apk.get_package()))
        checks.append(("versionName", apk.get_androidversion_name() == EXPECTED_VERSION_NAME, apk.get_androidversion_name()))
        checks.append(("versionCode", int(apk.get_androidversion_code()) == EXPECTED_VERSION_CODE, str(apk.get_androidversion_code())))
        checks.append(("Podpis APK v2", apk.is_signed_v2(), f"v1={apk.is_signed_v1()}, v2={apk.is_signed_v2()}, v3={apk.is_signed_v3()}"))

        cert_der, cert_fp = verify_v2_signature(args.apk)
        checks.append(("Kryptograficzna weryfikacja podpisu v2", True, "podpis RSA i skrót zawartości zgodne"))
        checks.append(("Oczekiwany certyfikat", cert_fp == EXPECTED_CERT_SHA256, cert_fp))

        baseline_path = find_baseline(args.apk)
        baseline = APK(str(baseline_path))
        baseline_cert_der, baseline_fp = verify_v2_signature(baseline_path)
        checks.append(("Pakiet zgodny z wersją 2.9", baseline.get_package() == apk.get_package(), baseline.get_package()))
        checks.append(("Certyfikat zgodny z wersją 2.9", baseline_cert_der == cert_der, baseline_fp))
        checks.append(("versionCode wyższy od wersji 2.9", int(apk.get_androidversion_code()) > int(baseline.get_androidversion_code()), f"{baseline.get_androidversion_code()} -> {apk.get_androidversion_code()}"))

        with zipfile.ZipFile(args.apk, "r") as zf:
            bad = zf.testzip()
            checks.append(("Integralność ZIP", bad is None, bad or "bez błędów CRC"))
            dex = DEX(zf.read("classes.dex"))

        speed = method_outputs(dex, "buildSpeedHero")
        build_ui = method_outputs(dex, "buildUi")
        update = method_outputs(dex, "lambda$checkForUpdates$30$pl-tomalawsb-licznik-MainActivity")
        strings = set(dex.get_strings())

        checks.extend([
            ("Kompas 100 dp", "v2, 100" in speed, "v2, 100"),
            ("Kompas 2 dp wyżej", "v3, 6" in speed, "topMargin=6 dp"),
            ("Kompas 2 dp w prawo", "v3, 2" in speed, "rightMargin=2 dp"),
            ("Dolny panel 83 dp", "v3, 83" in build_ui, "83 dp"),
            ("Aktualizator rozpoznaje versionCode 30100", "v5, 30100" in update, "v5, 30100"),
            ("Tag wydania v3.1", EXPECTED_RELEASE_TAG in strings, EXPECTED_RELEASE_TAG),
        ])
    except Exception as exc:
        checks.append(("Walidacja techniczna", False, str(exc)))

    for name, ok, detail in checks:
        print(("PASS" if ok else "FAIL") + f" | {name} | {detail}")
        failed |= not ok

    print("SHA-256 |", hashlib.sha256(args.apk.read_bytes()).hexdigest())
    print("WYNIK:", "NIEPOWODZENIE" if failed else "WSZYSTKIE TESTY ZALICZONE")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
