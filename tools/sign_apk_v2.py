#!/usr/bin/env python3
"""Podpisuje APK zgodnie z Android APK Signature Scheme v2.

Plik podpisu ma rozszerzenie .jks, ale jest kontenerem PKCS#12.
Klucz i certyfikat sa odczytywane bezposrednio przez cryptography,
bez JDK, keytool ani jarsigner.
"""
from __future__ import annotations

import argparse
import hashlib
import struct
from pathlib import Path

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.serialization import pkcs12

APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871A
APK_SIG_MAGIC = b"APK Sig Block 42"
SIG_ALG_RSA_PKCS1_SHA256 = 0x0103
CHUNK_SIZE = 1024 * 1024
EOCD_MAGIC = b"PK\x05\x06"


def lp(data: bytes) -> bytes:
    return struct.pack("<I", len(data)) + data


def find_zip_offsets(apk: bytes) -> tuple[int, int]:
    search_start = max(0, len(apk) - 65557)
    eocd_offset = apk.rfind(EOCD_MAGIC, search_start)
    if eocd_offset < 0 or eocd_offset + 22 > len(apk):
        raise RuntimeError("Nie znaleziono prawidlowego rekordu EOCD w APK.")
    cd_offset = struct.unpack_from("<I", apk, eocd_offset + 16)[0]
    if cd_offset > eocd_offset:
        raise RuntimeError("Nieprawidlowy offset katalogu centralnego ZIP.")
    return cd_offset, eocd_offset


def chunk_digest(chunk: bytes) -> bytes:
    payload = b"\xA5" + struct.pack("<I", len(chunk)) + chunk
    return hashlib.sha256(payload).digest()


def split_chunks(data: bytes):
    for start in range(0, len(data), CHUNK_SIZE):
        yield data[start:start + CHUNK_SIZE]


def content_digest(apk: bytes, cd_offset: int, eocd_offset: int) -> bytes:
    digests: list[bytes] = []
    digests.extend(chunk_digest(chunk) for chunk in split_chunks(apk[:cd_offset]))
    digests.extend(chunk_digest(chunk) for chunk in split_chunks(apk[cd_offset:eocd_offset]))

    eocd = bytearray(apk[eocd_offset:])
    eocd[16:20] = struct.pack("<I", cd_offset)
    digests.extend(chunk_digest(chunk) for chunk in split_chunks(bytes(eocd)))

    top_level = b"\x5A" + struct.pack("<I", len(digests)) + b"".join(digests)
    return hashlib.sha256(top_level).digest()


def build_signing_block(digest: bytes, certificate_der: bytes, private_key) -> bytes:
    digest_record = struct.pack("<I", SIG_ALG_RSA_PKCS1_SHA256) + lp(digest)
    digests = lp(digest_record)
    certificates = lp(certificate_der)
    attributes = b""
    signed_data = lp(digests) + lp(certificates) + lp(attributes)

    signature = private_key.sign(signed_data, padding.PKCS1v15(), hashes.SHA256())
    signature_record = struct.pack("<I", SIG_ALG_RSA_PKCS1_SHA256) + lp(signature)
    signatures = lp(signature_record)

    public_key_der = private_key.public_key().public_bytes(
        serialization.Encoding.DER,
        serialization.PublicFormat.SubjectPublicKeyInfo,
    )

    signer = lp(signed_data) + lp(signatures) + lp(public_key_der)
    signers = lp(signer)
    scheme_value = lp(signers)

    pair = (
        struct.pack("<Q", 4 + len(scheme_value))
        + struct.pack("<I", APK_SIGNATURE_SCHEME_V2_BLOCK_ID)
        + scheme_value
    )
    block_size = len(pair) + 24
    return (
        struct.pack("<Q", block_size)
        + pair
        + struct.pack("<Q", block_size)
        + APK_SIG_MAGIC
    )


def load_signing_material(keystore: Path, password: str):
    try:
        private_key, certificate, _ = pkcs12.load_key_and_certificates(
            keystore.read_bytes(),
            password.encode("utf-8"),
        )
    except Exception as exc:
        raise RuntimeError(
            "Nie udalo sie odczytac kontenera PKCS#12 z pliku podpisu: " + str(exc)
        ) from exc

    if private_key is None or certificate is None:
        raise RuntimeError("Nie odczytano klucza prywatnego lub certyfikatu.")
    return private_key, certificate


def sign_apk(input_apk: Path, output_apk: Path, keystore: Path, password: str) -> None:
    original = input_apk.read_bytes()
    cd_offset, eocd_offset = find_zip_offsets(original)

    if original[cd_offset - 16:cd_offset] == APK_SIG_MAGIC:
        raise RuntimeError("Wejsciowy APK ma juz blok podpisu v2/v3.")

    private_key, certificate = load_signing_material(keystore, password)
    digest = content_digest(original, cd_offset, eocd_offset)
    signing_block = build_signing_block(
        digest,
        certificate.public_bytes(serialization.Encoding.DER),
        private_key,
    )

    eocd = bytearray(original[eocd_offset:])
    eocd[16:20] = struct.pack("<I", cd_offset + len(signing_block))
    signed = original[:cd_offset] + signing_block + original[cd_offset:eocd_offset] + eocd

    output_apk.parent.mkdir(parents=True, exist_ok=True)
    output_apk.write_bytes(signed)
    print(f"OK: utworzono APK z podpisem v2: {output_apk}")
    print(f"Rozmiar bloku podpisu: {len(signing_block)} bajtow")
    print(f"SHA-256: {hashlib.sha256(signed).hexdigest()}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--keystore", required=True, type=Path)
    parser.add_argument("--password", required=True)
    parser.add_argument("--alias", required=False, default="licznik")
    args = parser.parse_args()

    for path, label in ((args.input, "APK"), (args.keystore, "plik podpisu")):
        if not path.is_file():
            raise SystemExit(f"Brak pliku {label}: {path}")

    sign_apk(args.input, args.output, args.keystore, args.password)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
