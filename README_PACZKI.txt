LICZNIK 2.7 — POPRAWIONA PACZKA

Ta paczka naprawia również błąd mechanizmu aktualizacji.

CO NAPRAWIA:
1. Interfejs 2.7 i kompas PNG z prawdziwym kanałem alfa.
2. Większą mapę i centrowanie bieżącej lokalizacji.
3. Usunięcie „Ostatnich jazd” z ekranu głównego.
4. Wersję aplikacji:
   - VERSION_NAME: 2.7 - 1406262027
   - CURRENT_RELEASE_TAG: v2.7-1406262027
   - CURRENT_VERSION_CODE: 20700
5. app/build.gradle:
   - versionCode 20700
   - versionName '2.7 - 1406262027'
6. .github/workflows/android-build.yml:
   - publikuje Release v2.7-1406262027
   - tworzy Licznik-v2.7-1406262027.apk
   - nie publikuje już błędnie wersji 2.6

UŻYCIE:
1. Rozpakuj CAŁĄ zawartość ZIP bezpośrednio do głównego folderu projektu Licznik.
2. Potwierdź zastąpienie plików, jeżeli Windows o to zapyta.
3. Uruchom uruchom_poprawki.ps1.
4. Po komunikacie GOTOWE uruchom swój upload_to_github.ps1.
5. Poczekaj na zielony Build Android APK.
6. W aplikacji 2.6 wybierz „Sprawdź aktualizację”.

Skrypt tworzy kopię bezpieczeństwa zmienianych plików.
