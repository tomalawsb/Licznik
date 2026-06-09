# Licznik jazdy PWA

Wersja: **1.0 - 0906260737**

Pierwsza działająca wersja aplikacji PWA do pomiaru jazdy telefonem.

## Funkcje

- ręczny wybór trybu: **Rower / Samochód**,
- pomiar aktualnej prędkości z GPS,
- średnia prędkość z dokładnością do **3 miejsc po przecinku**,
- dystans, czas jazdy, prędkość maksymalna,
- lokalna historia przejazdów,
- lokalne statystyki,
- ustawienia z numerem wersji,
- opcja „Nie wygaszaj ekranu”, jeśli przeglądarka obsługuje Screen Wake Lock,
- gotowy skrypt `upload_to_github.ps1` do wysłania na GitHub.

## Ważne ograniczenie

Ta paczka to **PWA pod GitHub Pages**, a nie natywna aplikacja Android z usługą w tle. Pomiar GPS działa najlepiej, gdy aplikacja jest widoczna i ekran jest aktywny. Po zablokowaniu telefonu Android/przeglądarka może zatrzymać albo ograniczyć pomiar.

Do pełnego działania po zablokowaniu ekranu trzeba później zrobić wersję Android, np. Capacitor + Foreground Service.

## Repozytorium

```text
https://github.com/tomalawsb/Licznik
```

Adres strony po włączeniu GitHub Pages:

```text
https://tomalawsb.github.io/Licznik/
```

## Wysyłka na GitHub przez PowerShell

Uruchom PowerShell w katalogu tej paczki i wpisz:

```powershell
.\upload_to_github.ps1
```
