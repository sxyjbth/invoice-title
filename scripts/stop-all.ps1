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

Write-Host 'Application processes started by this project were stopped. Existing MySQL was not changed.' -ForegroundColor Green
