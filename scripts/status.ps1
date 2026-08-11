$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$mysqlPort = if ($env:INVOICE_MYSQL_PORT) { [int]$env:INVOICE_MYSQL_PORT } else { 23306 }
$checks = @(
    @{ Name = 'Employee'; Port = 24173; Url = 'http://127.0.0.1:24173/' },
    @{ Name = 'Finance'; Port = 24175; Url = 'http://127.0.0.1:24175/' },
    @{ Name = 'Backend API'; Port = 28082; Url = 'http://127.0.0.1:28082/swagger-ui.html' },
    @{ Name = 'Nacos'; Port = 28848; Url = 'http://127.0.0.1:28848/nacos/' },
    @{ Name = 'MySQL'; Port = $mysqlPort; Url = '' },
    @{ Name = 'Redis'; Port = 26379; Url = '' }
)

foreach ($check in $checks) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $check.Port -ErrorAction SilentlyContinue
    $state = if ($listener) { 'RUNNING' } else { 'STOPPED' }
    "{0,-12} {1,-8} port {2} {3}" -f $check.Name, $state, $check.Port, $check.Url
}
