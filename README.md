# Licznik jazdy PWA

Wersja: **1.1 - 0906260752**

Pierwsza praktyczna wersja PWA licznika jazdy na telefon.

## Funkcje

- tryb ręczny: Rower / Samochód,
- aktualna prędkość GPS,
- średnia prędkość z dokładnością do 3 miejsc po przecinku,
- dystans,
- czas jazdy,
- prędkość maksymalna,
- liczba punktów GPS,
- lokalny podgląd trasy,
- historia przejazdów,
- statystyki,
- opcje z numerem wersji,
- opcja „Nie wygaszaj ekranu”.

## Zmiany w wersji 1.1

- zmniejszony i zagęszczony ekran jazdy,
- poprawiony licznik prędkości,
- przyciski Start / Pauza / Stop / Reset są stale widoczne nad dolnym menu,
- poprawione odstępy na telefonie,
- doprecyzowany opis działania po zablokowaniu telefonu.

## Ważne ograniczenie

Ta paczka jest PWA uruchamianą przez przeglądarkę / GitHub Pages.
Opcja „Nie wygaszaj ekranu” może utrzymywać aktywny ekran, ale nie daje pewnego działania po zablokowaniu telefonu.
Pełne działanie po blokadzie ekranu wymaga wersji Android APK z usługą działającą w tle.

## Wysyłka na GitHub

Uruchom PowerShell w folderze projektu:

```powershell
.\upload_to_github.ps1
```

Repozytorium ustawione w skrypcie:

```text
https://github.com/tomalawsb/Licznik.git
```
