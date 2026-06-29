# Raport zmian - Licznik 3.8

1. `showRouteMapDialog()` przebudowany na własną pełnoekranową nakładkę.
2. Dodano `closeFullMapOverlay()` — zamknięcie jednym dotknięciem.
3. `RouteMapView` dostał `setAutoFitOnRedraw(false)`, żeby duża mapa nie wracała automatycznie do pozycji użytkownika.
4. Usunięto przycisk `Szukaj`; dodano automatyczne wyszukiwanie z opóźnieniem 350 ms.
5. Zmieniono ikonę launcher na `launcher_icon.png`.
6. Kalorie zmienione z prostego `28 kcal/km` na szacunek MET zależny od czasu i prędkości.
