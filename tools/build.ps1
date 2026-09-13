param([switch]$Test)
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot/..
$env:JAVA_HOME=(Get-ChildItem .tools/java -Directory | Select-Object -First 1).FullName
if (-not $env:JAVA_HOME) { throw 'Install JDK 17 under .tools/java or build using Android Studio.' }
python tools/make-demo.py
if ($LASTEXITCODE -ne 0) { throw 'Demo build failed' }
& "$env:JAVA_HOME/bin/javac.exe" -d .tools/check app/src/main/java/dev/pixelgba/Storage.java tools/check-storage.java
if ($LASTEXITCODE -ne 0) { throw 'Check compilation failed' }
& "$env:JAVA_HOME/bin/java.exe" -cp .tools/check dev.pixelgba.CheckStorage
if ($LASTEXITCODE -ne 0) { throw 'Storage checks failed' }
& .tools/gradle-8.10.2/bin/gradle.bat :app:assembleDebug :app:lintDebug
if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
if ($Test) {
    & .tools/gradle-8.10.2/bin/gradle.bat :app:connectedDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw 'Device checks failed' }
}
New-Item -ItemType Directory -Force dist | Out-Null
Copy-Item app/build/outputs/apk/debug/app-debug.apk dist/PixelGBA-1.1.apk -Force
Get-FileHash dist/PixelGBA-1.1.apk -Algorithm SHA256
