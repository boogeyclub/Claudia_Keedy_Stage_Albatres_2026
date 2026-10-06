@echo off
setlocal
title CLAUDIA - demarrer_all

rem ---------------------------------------------------------------------------
rem  Lance le Back-End (Spring Boot) et le Front-End (Angular) dans deux
rem  consoles distinctes et lie leur cycle de vie :
rem    - fermer l'une des deux consoles arrete aussi l'autre et ce script ;
rem    - fermer cette fenetre arrete les deux consoles.
rem
rem  La logique se trouve dans demarrer_all.ps1 (objet Job Windows).
rem ---------------------------------------------------------------------------

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demarrer_all.ps1"

endlocal
