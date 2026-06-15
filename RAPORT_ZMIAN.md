# Raport zmian - automatyczna publikacja bez pytan

Wersja: **3.0 - 1506260712**

## Poprawiono

- Usunieto lokalne uzycie GitHub CLI (`gh`).
- Usunieto logowanie uruchamiane w przegladarce i kod jednorazowy.
- Skrypt korzysta z normalnego `git push`, tak jak w przekazanym pliku wzorcowym.
- Dane logowania pobiera Git Credential Manager zapisany przy Git for Windows.
- Jeden plik `URUCHOM_WSZYSTKO.ps1` buduje, podpisuje i sprawdza APK, tworzy ZIP, wykonuje commit oraz push.
- GitHub Actions po pushu automatycznie tworzy albo aktualizuje GitHub Release i podmienia APK.
- Skrypt sam czeka na zakonczenie publikacji; nie zadaje pytan ani nie wymaga naciskania Enter.
- Opis commita jednoznacznie zawiera zmiany interfejsu: przyciski nizsze o 3 dp, kompas 100 dp, przesuniecie o 2 dp w gore i w prawo.

## Uruchomienie

```powershell
.\URUCHOM_WSZYSTKO.ps1
```
