@echo off
cd /d C:\apache\htdocs\mulaisekarang-android
call gradlew.bat assembleDebug > build3.log 2>&1
echo DONE >> build3.log
