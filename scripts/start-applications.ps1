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
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtimeRoot = Join-Path $projectRoot '.runtime'
$logRoot = Join-Path $runtimeRoot 'logs'
$pidRoot = Join-Path $runtimeRoot 'pids'
New-Item -ItemType Directory -Force -Path $logRoot, $pidRoot | Out-Null

function Assert-PortFree([int]$Port, [string]$Name) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    if ($listener) { throw "$Name requires port $Port, but it is already in use. No foreign process was stopped." }
}

Assert-PortFree 24173 'Employee app'
Assert-PortFree 24175 'Finance admin'
Assert-PortFree 28082 'Backend API'

$nodeExe = Join-Path $runtimeRoot 'node\node-v24.18.1-win-x64\node.exe'
$javaHome = Join-Path $runtimeRoot 'jdk\jdk-21.0.11+10'
$mavenHome = Join-Path $runtimeRoot 'maven\apache-maven-3.9.16'
$mavenRepo = Join-Path $runtimeRoot 'maven-repository'
$settings = Join-Path $projectRoot 'scripts\maven-settings.xml'
$employeeViteEntry = Join-Path $projectRoot 'frontend\employee-h5\node_modules\vite\bin\vite.js'
$adminViteEntry = Join-Path $projectRoot 'frontend\finance-admin\node_modules\vite\bin\vite.js'

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$mavenHome\bin;$env:Path"
& (Join-Path $mavenHome 'bin\mvn.cmd') -o "-Dmaven.repo.local=$mavenRepo" -s $settings `
    -f (Join-Path $projectRoot 'backend\pom.xml') -DskipTests package

$employee = Start-Process -FilePath $nodeExe -WindowStyle Hidden -PassThru `
    -WorkingDirectory (Join-Path $projectRoot 'frontend\employee-h5') `
    -ArgumentList @($employeeViteEntry, '--host', '127.0.0.1', '--port', '24173', '--strictPort') `
    -RedirectStandardOutput (Join-Path $logRoot 'employee.out.log') -RedirectStandardError (Join-Path $logRoot 'employee.error.log')
$employee.Id | Set-Content -LiteralPath (Join-Path $pidRoot 'employee.pid') -Encoding ascii

$admin = Start-Process -FilePath $nodeExe -WindowStyle Hidden -PassThru `
    -WorkingDirectory (Join-Path $projectRoot 'frontend\finance-admin') `
    -ArgumentList @($adminViteEntry, '--host', '127.0.0.1', '--port', '24175', '--strictPort') `
    -RedirectStandardOutput (Join-Path $logRoot 'admin.out.log') -RedirectStandardError (Join-Path $logRoot 'admin.error.log')
$admin.Id | Set-Content -LiteralPath (Join-Path $pidRoot 'admin.pid') -Encoding ascii

$backendJar = Join-Path $projectRoot 'backend\target\invoice-title-service-0.1.0-SNAPSHOT.jar'
$backend = Start-Process -FilePath (Join-Path $javaHome 'bin\java.exe') -WindowStyle Hidden -PassThru `
    -WorkingDirectory (Join-Path $projectRoot 'backend') -ArgumentList @('-jar', $backendJar, '--spring.profiles.active=local') `
    -RedirectStandardOutput (Join-Path $logRoot 'backend.out.log') -RedirectStandardError (Join-Path $logRoot 'backend.error.log')
$backend.Id | Set-Content -LiteralPath (Join-Path $pidRoot 'backend.pid') -Encoding ascii

Write-Host 'Applications started: employee 24173, finance admin 24175, backend API 28082.' -ForegroundColor Green
