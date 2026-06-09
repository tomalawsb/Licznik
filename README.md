# Licznik jazdy Android

Wersja: **1.3 - 0906260810**

To jest natywna wersja Android, a nie zwykła PWA. Została przygotowana po to, żeby pomiar jazdy mógł działać po zablokowaniu telefonu.

## Co robi aplikacja

- ręczny tryb jazdy: **Rower / Samochód**,
- aktualna prędkość,
- średnia prędkość z trzema miejscami po przecinku,
- dystans,
- czas jazdy,
- prędkość maksymalna,
- punkty GPS,
- podgląd trasy,
- historia zakończonych jazd,
- statystyki,
- stałe powiadomienie podczas jazdy,
- pomiar GPS w `Foreground Service` z typem `location`.

## Ważne

Pomiar po zablokowaniu telefonu działa przez natywną usługę Android:

```xml
<service
    android:name=".RideTrackingService"
    android:exported="false"
    android:foregroundServiceType="location" />
```

Aplikacja prosi o:

- dokładną lokalizację,
- powiadomienia,
- usługę pierwszoplanową lokalizacji.

Dla najlepszej stabilności warto w ustawieniach Androida wyłączyć oszczędzanie baterii dla tej aplikacji.

## Jak zbudować APK lokalnie

1. Otwórz folder projektu w Android Studio.
2. Poczekaj, aż Gradle zsynchronizuje projekt.
3. Wybierz: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. APK będzie w:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Jak zbudować APK przez GitHub

W paczce jest workflow:

```text
.github/workflows/android-build.yml
```

Po wysłaniu na GitHub wejdź w repozytorium:

```text
Actions > Build Android APK
```

Po zakończeniu budowania pobierz artifact:

```text
Licznik-debug-apk
```

W środku będzie plik:

```text
app-debug.apk
```

## Wysyłka na GitHub

Uruchom PowerShell w głównym folderze projektu:

```powershell
.\upload_to_github.ps1
```

Skrypt wysyła projekt do:

```text
https://github.com/tomalawsb/Licznik.git
```
