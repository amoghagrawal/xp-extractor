@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot
echo Using Java from: %JAVA_HOME%
echo Building mod for Minecraft 1.21.1...
set GRADLE_OPTS=-Dorg.gradle.daemon=false
call gradlew.bat clean build --info
echo Build complete. Check for any errors above.
pause 