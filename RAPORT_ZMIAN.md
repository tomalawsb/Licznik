# Raport zmian - Licznik 3.6 FIX

## Naprawiono

- `MainActivity.java` miał wersję 3.5, a skrypt wymagał 3.6. Poprawiono na 3.6.
- Dodano realną obsługę czujników kompasu:
  - `Sensor.TYPE_ROTATION_VECTOR`,
  - fallback: `TYPE_ACCELEROMETER` + `TYPE_MAGNETIC_FIELD`,
  - `GeomagneticField` dla deklinacji magnetycznej.

## Zmienione pliki

- `app/src/main/java/pl/tomalawsb/licznik/MainActivity.java`
- `app/build.gradle`
- `.github/workflows/android-build.yml`
- `URUCHOM_WSZYSTKO.ps1`
