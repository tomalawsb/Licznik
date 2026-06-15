# Licznik jazdy 3.0

Wersja: **3.0 - 1506260712**

Uruchom jeden plik:

```powershell
.\URUCHOM_WSZYSTKO.ps1
```

Skrypt automatycznie:

1. sprawdza Git i Python,
2. instaluje wymagane biblioteki Python,
3. buduje, podpisuje i waliduje APK,
4. tworzy kompletna paczke ZIP,
5. pobiera aktualne repozytorium,
6. kopiuje pliki i tworzy commit,
7. wykonuje `git push`, korzystajac z zapisanych danych Git Credential Manager,
8. czeka na automatyczna publikacje APK przez GitHub Actions.

Skrypt nie uzywa lokalnego GitHub CLI, nie otwiera logowania w przegladarce i nie zadaje pytan.
