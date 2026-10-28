param(
    [string]$JavaHome = '',
    [string]$SdkPath = '',
    [ValidateRange(1,65535)][int]$BackendPort = 8080,
    [ValidateRange(5,600)][int]$TimeoutSeconds = 360,
    [switch]$ServeBackend,
    [switch]$WatchDevice
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$backendRoot = Join-Path $projectRoot 'Backend'
$androidRoot = Join-Path $projectRoot 'Android'
$runtimeRoot = Join-Path $projectRoot '.local'

function Read-Setting([string]$Path, [string]$Name) {
    if (!(Test-Path -LiteralPath $Path)) { return $null }
    foreach ($line in [IO.File]::ReadAllLines($Path)) {
        if ($line -match ('^\s*' + [regex]::Escape($Name) + '\s*=(.*)$')) {
            return $Matches[1].Trim().Trim('"').Trim("'")
        }
    }
    return $null
}
function Start-Helper([string[]]$ExtraArguments) {
    $arguments = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"' + $PSCommandPath + '"')) + $ExtraArguments
    Start-Process -FilePath (Join-Path $PSHOME 'powershell.exe') -ArgumentList $arguments -WindowStyle Hidden -PassThru
}
function Get-Adb {
    $candidates = @($SdkPath, $env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA 'Android\Sdk'))
    foreach ($candidate in $candidates) {
        if ($candidate) {
            $file = Join-Path $candidate 'platform-tools\adb.exe'
            if (Test-Path -LiteralPath $file) { return $file }
        }
    }
    throw 'No se encontro adb. Configura el SDK de Android Studio o ANDROID_HOME.'
}
function Connect-Devices([string]$Adb) {
    $count = 0
    foreach ($line in @(& $Adb devices)) {
        if ($line -match '^(\S+)\s+device\s*$') {
            $serial = $Matches[1]
            & $Adb -s $serial reverse "tcp:$BackendPort" "tcp:$BackendPort" | Out-Null
            if ($LASTEXITCODE -ne 0) { throw "No se pudo configurar adb reverse para $serial" }
            Write-Host "Dispositivo conectado al backend: $serial"
            $count++
        }
    }
    return $count
}
function Test-Backend {
    try {
        $response = Invoke-RestMethod -Uri "http://127.0.0.1:$BackendPort/api/health" -TimeoutSec 2 -UseBasicParsing
        return $response.service -eq 'cursos-api' -and $response.status -eq 'UP'
    } catch { return $false }
}

if ($JavaHome) {
    if (!(Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe'))) { throw 'JDK no valido.' }
    $env:JAVA_HOME = $JavaHome
    $env:PATH = (Join-Path $JavaHome 'bin') + ';' + $env:PATH
}
if ($ServeBackend) {
    # Separate PowerShell process: the server survives the Before Launch task.
    New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
    $env:SERVER_PORT = [string]$BackendPort
    Set-Location -LiteralPath $backendRoot
    & (Join-Path $backendRoot 'gradlew.bat') bootRun --console=plain *> (Join-Path $runtimeRoot 'backend.log')
    exit $LASTEXITCODE
}
if ($WatchDevice) {
    $adb = Get-Adb
    Start-Process -FilePath $adb -ArgumentList 'start-server' -WindowStyle Hidden -Wait
    $until = (Get-Date).AddSeconds(120)
    do {
        if ((Connect-Devices $adb) -gt 0) { exit 0 }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $until)
    exit 0
}

foreach ($component in @($androidRoot, $backendRoot)) {
    $envFile = Join-Path $component '.env'
    if (!(Test-Path -LiteralPath $envFile)) {
        Copy-Item -LiteralPath (Join-Path $component '.env.example') -Destination $envFile
    }
}
$apiUrl = Read-Setting (Join-Path $androidRoot '.env') 'API_BASE_URL'
if (!$apiUrl) { throw 'Falta API_BASE_URL en Android/.env.' }
$api = [Uri]$apiUrl
if (!$api.IsAbsoluteUri -or $api.Scheme -notin @('http','https')) { throw 'API_BASE_URL no es una URL HTTP/HTTPS valida.' }
if ($api.Host -notin @('localhost','127.0.0.1','10.0.2.2')) {
    Write-Host 'API remota/LAN configurada: se conserva su URL y no se inicia un backend local.'
    exit 0
}
$BackendPort = $api.Port
$configuredPort = Read-Setting (Join-Path $backendRoot '.env') 'SERVER_PORT'
if ($configuredPort -and [int]$configuredPort -ne $BackendPort) {
    throw 'El puerto de API_BASE_URL no coincide con SERVER_PORT de Backend/.env.'
}
$adb = Get-Adb
Start-Process -FilePath $adb -ArgumentList 'start-server' -WindowStyle Hidden -Wait
New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
$mutex = New-Object System.Threading.Mutex($false, ('Local\SyncPass.LocalRun.' + $BackendPort))
$acquired = $false
try {
    $acquired = $mutex.WaitOne([TimeSpan]::FromSeconds($TimeoutSeconds))
    if (!$acquired) { throw 'Otra preparacion de SyncPass no termino a tiempo.' }
    if (Test-Backend) {
        Write-Host "Backend SyncPass disponible en $BackendPort; se reutiliza."
    } else {
        # Refuse to replace an unrelated process that owns the configured port.
        $listener = Get-NetTCPConnection -LocalPort $BackendPort -State Listen -ErrorAction SilentlyContinue
        if ($listener) { throw "El puerto $BackendPort esta ocupado por un servicio que no responde como SyncPass. Revisa ese proceso." }
        $runningHelper = $false
        $pidFile = Join-Path $runtimeRoot 'backend-process.json'
        if (Test-Path -LiteralPath $pidFile) {
            try {
                $saved = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
                $process = Get-Process -Id $saved.pid -ErrorAction Stop
                $runningHelper = $process.StartTime.ToUniversalTime().Ticks -eq $saved.started
            } catch { $runningHelper = $false }
        }
        if (!$runningHelper) {
            $extra = @('-ServeBackend','-BackendPort',[string]$BackendPort)
            if ($env:JAVA_HOME) { $extra += @('-JavaHome',('"' + $env:JAVA_HOME + '"')) }
            $helper = Start-Helper $extra
            @{pid=$helper.Id; started=$helper.StartTime.ToUniversalTime().Ticks} | ConvertTo-Json | Set-Content -LiteralPath $pidFile
            Write-Host 'Iniciando backend en segundo plano. La primera compilacion puede tardar.'
        } else { Write-Host 'Esperando al backend que ya se esta iniciando.' }
        $until = (Get-Date).AddSeconds($TimeoutSeconds)
        while (!(Test-Backend)) {
            if ((Get-Date) -ge $until) { throw "El backend no inicio a tiempo. Revisa $runtimeRoot\backend.log" }
            if ($helper -and $helper.HasExited) { throw "El backend termino sin iniciar. Revisa $runtimeRoot\backend.log" }
            Start-Sleep -Seconds 2
        }
        Write-Host "Backend listo en $BackendPort."
    }
    if ((Connect-Devices $adb) -eq 0) {
        # Studio may boot the selected emulator after Before Launch; wait asynchronously.
        $extra = @('-WatchDevice','-BackendPort',[string]$BackendPort,'-SdkPath',('"' + (Split-Path (Split-Path $adb -Parent) -Parent) + '"'))
        $null = Start-Helper $extra
        Write-Host 'Se conectara automaticamente el dispositivo que Android Studio abra en los proximos 120 segundos.'
    }
    Write-Host 'Preparacion completa. Android Studio puede compilar y abrir la app.'
} finally {
    if ($acquired) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
