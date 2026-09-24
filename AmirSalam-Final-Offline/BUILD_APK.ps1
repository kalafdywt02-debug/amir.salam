param(
  [switch]$Install,
  [switch]$Release
)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$gradleVersion = '8.9'
$toolsDir = Join-Path $PSScriptRoot '.build-tools'
$gradleExe = Join-Path $toolsDir "gradle-$gradleVersion\bin\gradle.bat"

if (-not (Test-Path $gradleExe)) {
  throw "Gradle launcher not found: $gradleExe"
}

# Bug fix: the previous version of this script always ran assembleDebug,
# with no way to build a release APK locally. Debug builds are not
# R8/ProGuard-optimized and carry extra Compose debug/tracing overhead,
# which can feel like real app lag on low-RAM devices even though it's
# just the build type. Pass -Release to build/install the optimized
# release variant instead.
if ($Release) {
  $buildType = 'release'
  $gradleTask = 'assembleRelease'
} else {
  $buildType = 'debug'
  $gradleTask = 'assembleDebug'
}

Write-Host "در حال Build کردن APK ($buildType)..." -ForegroundColor Cyan
& $gradleExe --no-daemon --stacktrace $gradleTask
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = Join-Path $PSScriptRoot "app\build\outputs\apk\$buildType\app-$buildType.apk"
if (-not (Test-Path $apk)) { throw 'APK ساخته نشد.' }
Write-Host "`nAPK آماده است: $apk" -ForegroundColor Green

if ($Release) {
  Write-Host 'توجه: اگر app/build.gradle.kts برای release یک signingConfig معتبر نداشته باشد، نصب مستقیم این APK با adb ممکن است رد شود (باید امضا شده باشد).' -ForegroundColor Yellow
}

if ($Install) {
  $adbCandidates = @(
    $env:ANDROID_HOME,
    $env:ANDROID_SDK_ROOT,
    "$env:LOCALAPPDATA\Android\Sdk"
  ) | Where-Object { $_ -and (Test-Path $_) } | ForEach-Object { Join-Path $_ 'platform-tools\adb.exe' }
  $adb = $adbCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
  if (-not $adb) {
    Write-Host 'adb پیدا نشد؛ APK ساخته شده ولی نصب خودکار انجام نشد.' -ForegroundColor Yellow
    exit 0
  }
  & $adb start-server | Out-Null
  $devices = & $adb devices
  if ($devices -notmatch "\n[^\s]+\s+device") {
    Write-Host 'هیچ گوشی با ADB مجاز پیدا نشد.' -ForegroundColor Yellow
    Write-Host 'USB Debugging را فعال کنید، گوشی را وصل کنید و اجازه RSA را تأیید کنید.'
    exit 0
  }
  Write-Host 'در حال نصب APK...' -ForegroundColor Cyan
  & $adb install -r $apk
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  Write-Host 'نصب با موفقیت انجام شد.' -ForegroundColor Green
}
