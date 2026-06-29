LICZNIK 3.10.1 — POPRAWKA ZAKŁADEK I HISTORII

Ta paczka naprawia problem wywalania aplikacji przy przechodzeniu na zakładki Profil / Historia / Postępy.

CO ZMIENIONO:
1. MainActivity.java
   - dodano czyszczenie referencji do widoków ekranu Jazda po przejściu na inne zakładki,
   - zabezpieczono odbiornik aktualizacji GPS przed pracą na usuniętej mini-mapie,
   - odciążono Historię: lista nie tworzy już osobnej mapy dla każdego wpisu,
   - pojedynczy uszkodzony wpis historii nie powinien blokować całej zakładki,
   - pełna mapa aktualnej trasy może aktualizować punkty podczas jazdy.

2. RouteMapView.java
   - poprawiono performClick(), żeby kliknięcie mapy nie odpalało akcji dwa razy.

3. Wersja aplikacji
   - VERSION_NAME: 3.10.1 - 290626-tabs-fix
   - CURRENT_RELEASE_TAG: v3.10.1-290626-tabs-fix
   - CURRENT_VERSION_CODE: 31001

4. GitHub Actions / skrypt publikacji
   - APK po buildzie: Licznik-v3.10.1-290626-tabs-fix.apk
   - Release: v3.10.1-290626-tabs-fix

JAK UŻYĆ:
1. Rozpakuj paczkę.
2. Uruchom URUCHOM_WSZYSTKO.ps1 z głównego katalogu projektu.
3. Skrypt wyśle kod na GitHub.
4. GitHub Actions zbuduje APK.
