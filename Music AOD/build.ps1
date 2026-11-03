# Builds Music AOD without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Needs the Android SDK at %LOCALAPPDATA%\Android\Sdk with platforms;android-36 and build-tools;36.0.0.
#   .\build.ps1          full edition  -> build\MusicAOD.apk
#   .\build.ps1 -Lite    Lite edition  -> build-lite\MusicAOD.apk
#                        (no notification listener, so Play Protect allows installing it from WhatsApp/browsers)
param([switch]$Lite)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$versionCode = 8
$versionName = '1.5'

$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$bt  = "$sdk\build-tools\36.0.0"
$jar = "$sdk\platforms\android-36\android.jar"
$out = if ($Lite) { "build-lite" } else { "build" }
if ($Lite) { $versionName += ' Lite' }

if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force "$out\classes", "$out\dex" | Out-Null

$manifest = Get-Content AndroidManifest.xml -Raw
if ($Lite) {
    $manifest = [regex]::Replace($manifest, '(?s)\s*<service\s+android:name="\.MediaListener".*?</service>', '')
    if ($manifest -match 'MediaListener') { throw "failed to strip MediaListener from the manifest" }
}
[IO.File]::WriteAllText("$PSScriptRoot\$out\AndroidManifest.xml", $manifest)

& "$bt\aapt2.exe" compile --dir res -o "$out\res.zip"
if ($LASTEXITCODE) { throw "aapt2 compile failed" }
& "$bt\aapt2.exe" link -o "$out\base.apk" --manifest "$out\AndroidManifest.xml" -I $jar "$out\res.zip" `
    --min-sdk-version 34 --target-sdk-version 36 --version-code $versionCode --version-name $versionName
if ($LASTEXITCODE) { throw "aapt2 link failed" }

$sources = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object FullName
& javac --release 17 -encoding UTF-8 -classpath $jar -d "$out\classes" @sources
if ($LASTEXITCODE) { throw "javac failed" }

& jar cf "$out\classes.jar" -C "$out\classes" .
& "$bt\d8.bat" --release --min-api 34 --lib $jar --output "$out\dex" "$out\classes.jar"
if ($LASTEXITCODE) { throw "d8 failed" }

# Add classes.dex to the APK.
Copy-Item "$out\dex\classes.dex" "$out\classes.dex"
Push-Location $out; & jar uf base.apk classes.dex; Pop-Location

& "$bt\zipalign.exe" -p -f 4 "$out\base.apk" "$out\aligned.apk"
if ($LASTEXITCODE) { throw "zipalign failed" }

$ks = "debug.keystore"
if (-not (Test-Path $ks)) {
    & keytool -genkeypair -keystore $ks -storepass android -keypass android -alias debug `
        -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=MusicAOD Debug"
}
& "$bt\apksigner.bat" sign --ks $ks --ks-pass pass:android --key-pass pass:android `
    --out "$out\MusicAOD.apk" "$out\aligned.apk"
if ($LASTEXITCODE) { throw "apksigner failed" }

Write-Host "Built $PSScriptRoot\$out\MusicAOD.apk ($versionName)"
