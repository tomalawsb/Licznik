# Licznik jazdy 3.2

Wersja: **3.2 - 2906260712**  
`versionCode`: **30200**

## Najważniejsze zmiany

- Kompas przeniesiony na mapę na ekranie głównym.
- Kompas składa się z osobnych warstw PNG:
  - `kompas_tarcza.png`,
  - `kompas_igla_glowna.png`,
  - `kompas_wskaznik_celu.png`.
- Wskaźnik celu obraca się niezależnie od głównej igły.
- Dłuższe przytrzymanie palcem na dużej mapie ustawia punkt docelowy.
- Pod kompasem pokazuje się odległość do celu w linii prostej.
- Duża mapa aktualizuje bieżącą pozycję podczas jazdy.
- Okno dużej mapy zamyka się jednym kliknięciem.
- Tekst średniej i maksymalnej prędkości na zielonym panelu zwiększony o 2 punkty.

## Budowanie APK

Po wrzuceniu projektu na GitHub uruchomi się GitHub Actions i zbuduje:

`Licznik-v3.2-2906260712.apk`

Możesz użyć:

`URUCHOM_WSZYSTKO.ps1`

Skrypt robi commit i push do `main`, a APK buduje GitHub Actions.
