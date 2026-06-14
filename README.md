# Licznik jazdy Android

Wersja: **2.7 - 1406262027**

Przebudowany interfejs według przesłanego wzoru:
- zielona karta aktualnej prędkości,
- dwa duże kafelki: dystans i czas,
- karta GPS z mapą OSM,
- wzniesienie / kalorie / tempo,
- przycisk Rozpocznij jazdę + Stop + Reset,
- sekcja Ostatnie jazdy,
- dolne menu: Jazda, Historia, Postępy, Profil.

Aplikacja nadal używa natywnej usługi GPS w tle.


## Zmiany 2.5 - 0906261705
- Poprawiono reset licznika bez niepotrzebnego uruchamiania serwisu.
- Zapytanie o snapshot nie zostawia nieaktywnego serwisu w tle.
- Komunikat po Stop zależy od faktycznego wyniku zapisu historii.
- Poprawiono porównywanie wersji aktualizacji.
- Poprawiono obsługę zbyt gęstych odczytów GPS.
- W trybie samochodu kalorie pokazują `--`.
- Poprawiono wysokość listy ostatnich jazd.
- Status GPS pokazuje jakość sygnału.
- Usunięto `usesCleartextTraffic=true`.
- Usunięto martwe pliki `SpeedGaugeView.java` i `RouteView.java`.


## Zmiany 2.6 - 1406261822
- Historia zachowuje trasę od rzeczywistego punktu startu; usunięto limit przesuwający początek po 800 punktach GPS.
- Ikona w lewym górnym rogu zmienia się natychmiast między rowerem i samochodem.
- Średnia prędkość jest odświeżana raz na sekundę.


## Zmiany 2.7 - 1406262027
- Przebudowano kartę prędkości: prędkość jest wyżej, a średnia i maksymalna są większe.
- Dodano kompas z rzeczywistą przezroczystością PNG, wskazujący północ.
- Usunięto Ostatnie jazdy z ekranu głównego.
- Powiększono mapę i obniżono panel sterowania.
- Powiększono ikony przycisków jazdy, Stop i Reset.
- Po otwarciu aktualnej mapy ostatnia pozycja użytkownika jest wyśrodkowana.
