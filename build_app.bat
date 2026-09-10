@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d C:\Users\frank\Downloads\ESPBridge
java -version
call gradlew.bat assembleDebug
