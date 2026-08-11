[CmdletBinding()]
param(
    [switch]$UseLocalMySql
)

$ErrorActionPreference = 'Stop'

function Normalize-ProcessPathEnvironment {
    $processPath = [Environment]::GetEnvironmentVariable('Path', 'Process')
    if (-not $processPath) { $processPath = [Environment]::GetEnvironmentVariable('PATH', 'Process') }
    [Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
    [Environment]::SetEnvironmentVariable('Path', $processPath, 'Process')
}

function Set-NacosProperty([string]$Path, [string]$Key, [string]$Value) {
    $content = [System.IO.File]::ReadAllText($Path)
    $pattern = '(?m)^' + [regex]::Escape($Key) + '=.*$'
    $line = "$Key=$Value"
    if ([regex]::IsMatch($content, $pattern)) {
        $content = [regex]::Replace($content, $pattern, $line)
    } else {
        $content = $content.TrimEnd() + [Environment]::NewLine + $line + [Environment]::NewLine
    }
    [System.IO.File]::WriteAllText($Path, $content, [System.Text.UTF8Encoding]::new($false))
}

Normalize-ProcessPathEnvironment
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtimeRoot = Join-Path $projectRoot '.runtime'
$dataRoot = Join-Path $runtimeRoot 'data'
$logRoot = Join-Path $runtimeRoot 'logs'
$pidRoot = Join-Path $runtimeRoot 'pids'
New-Item -ItemType Directory -Force -Path $dataRoot, $logRoot, $pidRoot | Out-Null

function Assert-PortFree([int]$Port, [string]$Name) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    if ($listener) {
        throw "$Name requires port $Port, currently owned by PID $($listener[0].OwningProcess). No foreign process was stopped."
    }
}

function Wait-Port([int]$Port, [string]$Name) {
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) { return }
        Start-Sleep -Milliseconds 500
    }
    throw "$Name did not start on port $Port. Check .runtime/logs."
}

$javaHome = Join-Path $runtimeRoot 'jdk\jdk-21.0.11+10'
$mysqlHome = Join-Path $runtimeRoot 'mysql\mysql-8.4.10-winx64'
$redisHome = Join-Path $runtimeRoot 'redis\redis-8.8.0\Redis-8.8.0-Windows-x64-msys2'
$nacosHome = Join-Path $runtimeRoot 'nacos\nacos'

$required = @((Join-Path $javaHome 'bin\java.exe'), (Join-Path $redisHome 'redis-server.exe'), (Join-Path $nacosHome 'bin\startup.cmd'))
if (-not $UseLocalMySql) { $required += (Join-Path $mysqlHome 'bin\mysqld.exe') }
foreach ($file in $required) {
    if (-not (Test-Path -LiteralPath $file)) { throw "Missing $file. Run scripts\bootstrap.ps1 first." }
}

if (-not $UseLocalMySql) { Assert-PortFree 23306 'MySQL' }
Assert-PortFree 26379 'Redis'
Assert-PortFree 28848 'Nacos'
Assert-PortFree 28081 'Nacos Console'
Assert-PortFree 27848 'Nacos JRaft'
Assert-PortFree 29848 'Nacos gRPC'
Assert-PortFree 29849 'Nacos gRPC TLS'

if (-not $UseLocalMySql) {
    $mysqlData = Join-Path $dataRoot 'mysql'
    New-Item -ItemType Directory -Force -Path $mysqlData | Out-Null
    if (-not (Test-Path -LiteralPath (Join-Path $mysqlData 'mysql'))) {
        Write-Host '[initialize] MySQL data directory'
        & (Join-Path $mysqlHome 'bin\mysqld.exe') --initialize-insecure "--basedir=$mysqlHome" "--datadir=$mysqlData"
    }

    $mysqlProcess = Start-Process -FilePath (Join-Path $mysqlHome 'bin\mysqld.exe') -WindowStyle Hidden -PassThru `
        -ArgumentList @("--basedir=$mysqlHome", "--datadir=$mysqlData", '--port=23306', '--bind-address=127.0.0.1', '--character-set-server=utf8mb4', '--collation-server=utf8mb4_0900_ai_ci') `
        -RedirectStandardOutput (Join-Path $logRoot 'mysql.out.log') -RedirectStandardError (Join-Path $logRoot 'mysql.error.log')
    $mysqlProcess.Id | Set-Content -LiteralPath (Join-Path $pidRoot 'mysql.pid') -Encoding ascii
    Wait-Port 23306 'MySQL'

    $mysqlClient = Join-Path $mysqlHome 'bin\mysql.exe'
    & $mysqlClient --protocol=TCP -h127.0.0.1 -P23306 -uroot -e "CREATE DATABASE IF NOT EXISTS invoice_title CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER IF NOT EXISTS 'invoice_title'@'127.0.0.1' IDENTIFIED BY 'invoice_title_local'; GRANT ALL PRIVILEGES ON invoice_title.* TO 'invoice_title'@'127.0.0.1'; FLUSH PRIVILEGES;"
} else {
    Write-Host '[external] MySQL is managed outside this project; no local MySQL process or configuration was changed.'
}

$redisData = Join-Path $dataRoot 'redis'
New-Item -ItemType Directory -Force -Path $redisData | Out-Null
$redisProcess = Start-Process -FilePath (Join-Path $redisHome 'redis-server.exe') -WindowStyle Hidden -PassThru `
    -ArgumentList @('--port', '26379', '--bind', '127.0.0.1', '--dir', $redisData, '--dbfilename', 'dump.rdb', '--appendonly', 'yes') `
    -RedirectStandardOutput (Join-Path $logRoot 'redis.out.log') -RedirectStandardError (Join-Path $logRoot 'redis.error.log')
$redisProcess.Id | Set-Content -LiteralPath (Join-Path $pidRoot 'redis.pid') -Encoding ascii
Wait-Port 26379 'Redis'

$env:JAVA_HOME = $javaHome
$env:MODE = 'standalone'
$env:NACOS_SERVER_PORT = '28848'
$env:NACOS_AUTH_ENABLE = 'false'
$env:NACOS_AUTH_IDENTITY_KEY = 'invoice-title-local-key'
$env:NACOS_AUTH_IDENTITY_VALUE = 'invoice-title-local-value'
$env:NACOS_AUTH_TOKEN = 'SW52b2ljZVRpdGxlTG9jYWxUb2tlbkZvckRldmVsb3BtZW50T25seTIwMjY='
$nacosProperties = Join-Path $nacosHome 'conf\application.properties'
Set-NacosProperty $nacosProperties 'nacos.server.main.port' '28848'
Set-NacosProperty $nacosProperties 'nacos.console.port' '28081'
Set-NacosProperty $nacosProperties 'nacos.core.auth.server.identity.key' $env:NACOS_AUTH_IDENTITY_KEY
Set-NacosProperty $nacosProperties 'nacos.core.auth.server.identity.value' $env:NACOS_AUTH_IDENTITY_VALUE
Set-NacosProperty $nacosProperties 'nacos.core.auth.plugin.nacos.token.secret.key' $env:NACOS_AUTH_TOKEN
Start-Process -FilePath 'cmd.exe' -WindowStyle Hidden -WorkingDirectory (Join-Path $nacosHome 'bin') `
    -ArgumentList @('/c', 'startup.cmd -m standalone')
Wait-Port 28848 'Nacos'
$nacosPid = (Get-NetTCPConnection -State Listen -LocalPort 28848).OwningProcess
$nacosPid | Set-Content -LiteralPath (Join-Path $pidRoot 'nacos.pid') -Encoding ascii

$mysqlSummary = if ($UseLocalMySql) { 'external MySQL' } else { 'MySQL 23306' }
Write-Host "Infrastructure started: $mysqlSummary, Redis 26379, Nacos 28848." -ForegroundColor Green
