Licznik 3.10 - FIX WORKFLOW

Poprawka usuwa błąd:
Invalid workflow file: .github/workflows/android-build.yml#L63
You have an error in your yaml syntax on line 63

Przyczyna:
W poprzednim workflow tekst printf został zapisany jako kilka fizycznych linii w YAML i rozwalił składnię.

Co zmieniono:
- android-build.yml nie używa już wadliwego printf.
- publikowanie Release robi akcja softprops/action-gh-release@v2.
- URUCHOM_WSZYSTKO.ps1 sprawdza, czy workflow nie zawiera starego błędu.

Uruchom:
./URUCHOM_WSZYSTKO.ps1

Po udanym runie Actions powstanie:
Licznik-v3.10.1-290626-tabs-fix.apk
