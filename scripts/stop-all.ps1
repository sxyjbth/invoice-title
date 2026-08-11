[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtimeRoot = Join-Path $projectRoot '.runtime'
$pidRoot = Join-Path $runtimeRoot 'pids'

function Stop-OwnedProcess([string]$Name) {
    $pidFile = Join-Path $pidRoot "$Name.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) { return }
    $processId = [int](Get-Content -LiteralPath $pidFile -Raw)
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        $path = $process.Path
        if (-not $path -or -not $path.StartsWith($runtimeRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to stop ${Name}: PID $processId is outside this project's .runtime directory."
        }
        Stop-Process -Id $processId
        Write-Host "[stopped] $Name"
    }
    Remove-Item -LiteralPath $pidFile -Force
}

Stop-OwnedProcess 'employee'
Stop-OwnedProcess 'admin'
Stop-OwnedProcess 'backend'

$redisPidFile = Join-Path $pidRoot 'redis.pid'
$redisCli = Join-Path $runtimeRoot 'redis\redis-8.8.0\Redis-8.8.0-Windows-x64-msys2\redis-cli.exe'
if ((Test-Path -LiteralPath $redisPidFile) -and (Test-Path -LiteralPath $redisCli)) {
    & $redisCli -h 127.0.0.1 -p 26379 shutdown 2>$null
}

$mysqlPidFile = Join-Path $pidRoot 'mysql.pid'
$mysqlAdmin = Join-Path $runtimeRoot 'mysql\mysql-8.4.10-winx64\bin\mysqladmin.exe'
if ((Test-Path -LiteralPath $mysqlPidFile) -and (Test-Path -LiteralPath $mysqlAdmin)) {
    & $mysqlAdmin --protocol=TCP -h127.0.0.1 -P23306 -uroot shutdown 2>$null
}

$env:JAVA_HOME = Join-Path $runtimeRoot 'jdk\jdk-21.0.11+10'
$env:NACOS_SERVER_PORT = '28848'
$nacosShutdown = Join-Path $runtimeRoot 'nacos\nacos\bin\shutdown.cmd'
$nacosPidFile = Join-Path $pidRoot 'nacos.pid'
if ((Test-Path -LiteralPath $nacosPidFile) -and (Test-Path -LiteralPath $nacosShutdown)) {
    Start-Process -FilePath 'cmd.exe' -WindowStyle Hidden -Wait -WorkingDirectory (Split-Path $nacosShutdown) -ArgumentList @('/c', 'shutdown.cmd')
}

foreach ($name in @('mysql', 'redis', 'nacos')) {
    $pidFile = Join-Path $pidRoot "$name.pid"
    if (Test-Path -LiteralPath $pidFile) { Remove-Item -LiteralPath $pidFile -Force }
}
Write-Host 'Processes started by this project were stopped.' -ForegroundColor Green
