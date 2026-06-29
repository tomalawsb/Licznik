# Raport zmian - Licznik 3.7

## Zmieniono

1. Usunięto automatyczne centrowanie dużej mapy przy każdej aktualizacji GPS.
2. Przycisk `Zamknij` zamyka dialog na `MotionEvent.ACTION_DOWN`.
3. `compassDialView` i `compassNeedleView` obracają się razem.
4. `targetCompassView` pozostaje niezależną igłą celu.
5. POI pod kompasem pokazuje skrócony zapis: ikona + dystans + kierunek.
6. POI na mapie dostały rysowane ikony w markerach.

## Zmienione pliki

- `MainActivity.java`
- `RouteMapView.java`
- `app/build.gradle`
- `.github/workflows/android-build.yml`
- `URUCHOM_WSZYSTKO.ps1`
