# Raport zmian - Licznik 3.4

## Wykonane

1. Dodano pobieranie POI z Overpass API.
2. Dodano listę POI w pamięci aplikacji.
3. Dodano kafelek POI pod kompasem.
4. Kliknięcie kafelka przełącza na kolejny punkt.
5. Automatyczne przełączenie punktu co 3 minuty.
6. Długie przytrzymanie kafelka ustawia punkt jako cel kompasu.
7. Dodano sortowanie POI według odległości.
8. Dodano deduplikację zbliżonych lub identycznie nazwanych punktów.
9. Dodano typy: stacja, sklep, schronisko, kemping, parking, apteka, restauracja, kawiarnia, szpital, policja, punkt widokowy.

## Zmienione pliki

- `app/src/main/java/pl/tomalawsb/licznik/MainActivity.java`
- `app/build.gradle`
- `.github/workflows/android-build.yml`
- `URUCHOM_WSZYSTKO.ps1`
