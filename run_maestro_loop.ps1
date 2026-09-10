$adbPath = "C:\Users\Krisman\AppData\Local\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v3.3.4\adb.exe"
$env:PATH += ";C:\Users\Krisman\AppData\Local\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v3.3.4\"

$device = "R9RT4079YQL"
$testFlow = ".maestro/flows/test-all.yaml"

Write-Host "Running maestro tests on device $device repeatedly..."
$count = 1
while ($true) {
    Write-Host "`n--- Iteration $count ---"
    
    # Optional: build and install the app first if needed
    # call gradlew.bat assembleDebug
    # & $adbPath -s $device install -r app\build\outputs\apk\debug\app-debug.apk
    
    # Run the maestro test
    # ANDROID_SERIAL is used by maestro to target the device
    $env:ANDROID_SERIAL = $device
    maestro test $testFlow
    
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        Write-Host "Test failed on iteration $count with exit code $exitCode" -ForegroundColor Red
        # Break or continue depending on preference, I will pause to let user see
        break
    } else {
        Write-Host "Test passed on iteration $count" -ForegroundColor Green
    }
    
    $count++
    Start-Sleep -Seconds 2
}
