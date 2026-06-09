# Licznik jazdy Android

Wersja: **2.2 - 0906261554**

Natywna aplikacja Android do pomiaru jazdy rowerem albo samochodem.

## Najważniejsze funkcje

- pomiar GPS w tle przez usługę pierwszoplanową,
- tryb ręczny: Rower / Samochód,
- prędkość aktualna, średnia, dystans, czas i maksymalna prędkość,
- historia zapisanych jazd,
- normalne mapy OpenStreetMap przez osmdroid,
- podgląd trasy na ekranie głównym i w historii,
- po kliknięciu mapy otwiera się pełnoekranowy widok trasy,
- pełnoekranowa mapa pozwala przesuwać, przybliżać i dopasować trasę.

## Budowanie APK

Po wysłaniu projektu na GitHub wejdź w:

```text
Actions → Build Android APK
```

Po zielonym buildzie pobierz artefakt:

```text
Licznik-release-apk
```

albo plik z sekcji Releases:

```text
Licznik-v2.2-0906261554.apk
```

## Wysyłka na GitHub

W folderze projektu uruchom:

```powershell
.\upload_to_github.ps1
```

Repozytorium:

```text
https://github.com/tomalawsb/Licznik.git
```
