Licznik 3.2 - FIX-DROPBOX

Poprawka dotyczy bledu:
unable to write file ... .git/objects/pack/... Permission denied

Przyczyna:
projekt byl rozpakowany w Dropboxie, a Dropbox/antywirus potrafil zablokowac pliki .git podczas klonowania.

Co zmieniono:
URUCHOM_WSZYSTKO.ps1 klonuje repozytorium do folderu TEMP poza Dropboxem:
%LOCALAPPDATA%\Temp\Licznik_publish_v32_...

Uruchom:
./URUCHOM_WSZYSTKO.ps1

Po wyslaniu zmian GitHub Actions zbuduje:
Licznik-v3.2-2906260712.apk
