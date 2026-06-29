# Licznik jazdy 3.6

Wersja: **3.6 - 2906261055**  
`versionCode`: **30600**

## Poprawiono

- `MainActivity.java` ma teraz właściwą wersję 3.6.
- Kompas korzysta z czujnika `rotation vector`.
- Gdy telefon nie ma `rotation vector`, aplikacja używa akcelerometru i magnetometru.
- Dodano korektę deklinacji magnetycznej z GPS.
- Ruch igły jest wygładzany i odświeżany płynnie.
- Igła celu obraca się względem aktualnego kierunku telefonu.
- Kierunek jazdy GPS jest fallbackiem, gdy czujnik kompasu nie daje danych.

APK po GitHub Actions:

`Licznik-v3.6-2906261055.apk`
