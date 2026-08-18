[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

function Normalize-ProcessPathEnvironment {
    $processPath = [Environment]::GetEnvironmentVariable('Path', 'Process')
    if (-not $processPath) { $processPath = [Environment]::GetEnvironmentVariable('PATH', 'Process') }
    [Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
    [Environment]::SetEnvironmentVariable('Path', $processPath, 'Process')
}

Normalize-ProcessPathEnvironment
$mysqlHost = if ($env:INVOICE_MYSQL_HOST) { $env:INVOICE_MYSQL_HOST } else { '127.0.0.1' }
$mysqlPort = if ($env:INVOICE_MYSQL_PORT) { [int]$env:INVOICE_MYSQL_PORT } else { 3306 }
$client = [System.Net.Sockets.TcpClient]::new()

try {
    $connected = $client.ConnectAsync($mysqlHost, $mysqlPort).Wait(5000)
    if (-not $connected -or -not $client.Connected) {
        throw "Connection timed out."
    }
} catch {
    throw "[external] existing MySQL is unavailable at ${mysqlHost}:$mysqlPort. Start the local MySQL service before this project. $($_.Exception.Message)"
} finally {
    $client.Dispose()
}

Write-Host "[external] existing MySQL is available at ${mysqlHost}:$mysqlPort. No MySQL process or configuration was changed." -ForegroundColor Green
