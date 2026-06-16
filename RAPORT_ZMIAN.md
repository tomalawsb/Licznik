# Raport zmian

Wersja: **3.1 - 1606260712**

## Poprawiono

- Wszystkie bieżące oznaczenia wersji ustawiono na 3.1.
- `versionCode` ustawiono na 30100, czyli wyżej niż 20900 w bazowej wersji 2.9.
- APK podpisano tym samym certyfikatem co dołączony APK 2.9.
- Walidator sprawdza teraz nie tylko obecność certyfikatu, lecz także podpis RSA, skrót całej zawartości APK, pakiet, wersję, CRC oraz zgodność aktualizacyjną z wersją 2.9.
- Dodano `BUDUJ_TYLKO_LOKALNIE.ps1`, który nie wykonuje operacji Git ani GitHub.

## Pliki wynikowe

- `Licznik-v3.1-1606260712.apk`
- `VALIDATION_RESULTS.txt`
- `SHA256SUMS.txt`
