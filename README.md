# Licznik jazdy Android

Wersja: **2.5 - 0906261705**

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
