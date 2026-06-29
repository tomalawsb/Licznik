# Licznik jazdy 3.4

Wersja: **3.4 - 2906261015**  
`versionCode`: **30400**

## Nowe POI pod kompasem

Dodano mały kafelek POI pod kompasem:

- pokazuje najbliższy punkt orientacyjny z OpenStreetMap/Overpass,
- obsługuje m.in. stacje paliw, sklepy, schroniska, kempingi, parkingi, apteki, restauracje, kawiarnie, szpitale, policję i punkty widokowe,
- po kliknięciu kafelek przełącza się na kolejny punkt,
- bez dotykania przełącza się automatycznie co 3 minuty,
- po dłuższym przytrzymaniu kafelka POI ustawia się jako cel kompasu,
- przy punkcie wyświetlana jest odległość i strzałka kierunku.

## APK

Po wysłaniu na GitHub Actions buduje:

`Licznik-v3.4-2906261015.apk`

## Uwaga techniczna

POI są pobierane dynamicznie przez internet z Overpass API. Gdy nie ma internetu albo serwer Overpass chwilowo nie odpowiada, kafelek pokaże informację `POI: brak internetu`.
