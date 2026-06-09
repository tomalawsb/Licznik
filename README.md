# Licznik jazdy Android

Wersja: **1.8 - 0906260938**

To jest natywna aplikacja Android do mierzenia prędkości, dystansu, średniej prędkości i historii jazdy.

## Co robi aplikacja

- ręczny tryb jazdy: **Rower / Samochód**,
- aktualna prędkość GPS,
- średnia prędkość z dokładnością do 3 miejsc po przecinku,
- dystans,
- czas jazdy,
- prędkość maksymalna,
- historia przejazdów,
- statystyki,
- pomiar GPS jako Foreground Service z powiadomieniem,
- ustawienia aplikacji,
- sprawdzanie aktualizacji z GitHub Releases,
- tryb pełnoekranowy ukrywający paski systemowe Androida,
- zegarek aplikacji w nagłówku pokazujący aktualną godzinę,
- płynniejsze odświeżanie czasu jazdy niezależnie od częstotliwości punktów GPS,
- skonsolidowany przycisk **Start / Pauza / Wznów**,
- przycisk **Reset** działający również w trakcie jazdy,
- usunięty kafelek punktów GPS z ekranu jazdy,
- filtr odrzucający absurdalne skoki GPS i zawyżone maksymalne prędkości.

## Działanie po zablokowaniu telefonu

Pomiar działa przez natywną usługę Android `RideTrackingService` z typem:

```xml
android:foregroundServiceType="location"
```

Podczas pomiaru aplikacja pokazuje stałe powiadomienie. To jest wymagane przez Androida, żeby GPS mógł działać po zablokowaniu ekranu.

Jeśli telefon mimo tego przerywa pomiar, wyłącz oszczędzanie baterii dla aplikacji **Licznik jazdy**.

## Budowanie APK

Na GitHubie:

1. Wejdź w **Actions**.
2. Uruchom workflow **Build Android APK**.
3. Po zakończeniu pobierz artifact **Licznik-release-apk**.

Workflow buduje plik:

```text
app-release.apk
```

oraz publikuje go jako GitHub Release:

```text
Licznik-v1.7-0906260920.apk
```

## Aktualizacje aplikacji

Aplikacja może sprawdzać GitHub Releases i otworzyć najnowszy plik APK do pobrania.

Android nie pozwala zwykłej aplikacji instalować aktualizacji całkowicie po cichu. Użytkownik musi potwierdzić instalację APK.

## Ważne o podpisie APK

Ta paczka zawiera stały klucz podpisu `licznik-release.jks`, żeby kolejne wersje mogły instalować się jako aktualizacja, a nie jako nowa aplikacja.

Dla prywatnego projektu testowego to jest wygodne. Dla publicznej dystrybucji klucz podpisu powinien być trzymany poza repozytorium, najlepiej w GitHub Secrets albo w Google Play App Signing.



## Poprawka builda 0906261003

Dodano `gradle.properties` z ustawieniami AndroidX:

```properties
android.useAndroidX=true
android.enableJetifier=true
```

Poprawia to błąd GitHub Actions związany z zależnościami AndroidX/Google Play Services przy budowaniu `assembleRelease`.

## Zmiany 1.8 - 0906260938
- Zmieniono silnik lokalizacji na Fused Location Provider.
- Dodano stabilizację postoju bez blokowania realnej jazdy.
- Prędkość jest brana najpierw z wiarygodnego pola speed GPS, a dopiero potem z odległości między punktami.
- Usunięto zbyt agresywny filtr, który w wersji 1.7 potrafił stale pokazywać 0 km/h.
