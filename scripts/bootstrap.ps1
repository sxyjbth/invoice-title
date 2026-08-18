[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtimeRoot = Join-Path $projectRoot '.runtime'
$downloadRoot = Join-Path $runtimeRoot 'downloads'

New-Item -ItemType Directory -Force -Path $runtimeRoot, $downloadRoot | Out-Null

function Install-Archive {
    param(
        [Parameter(Mandatory)] [string]$Name,
        [Parameter(Mandatory)] [string]$Url,
        [Parameter(Mandatory)] [string]$ArchiveName,
        [Parameter(Mandatory)] [string]$Destination,
        [Parameter(Mandatory)] [string]$Marker
    )

    if (Test-Path -LiteralPath $Marker) {
        Write-Host "[exists] $Name"
        return
    }

    $archivePath = Join-Path $downloadRoot $ArchiveName
    if (-not (Test-Path -LiteralPath $archivePath)) {
        Write-Host "[download] $Name"
        Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $archivePath
    }
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    Write-Host "[extract] $Name"
    Expand-Archive -LiteralPath $archivePath -DestinationPath $Destination -Force
}

Install-Archive -Name 'Node.js 24.18.1' `
    -Url 'https://nodejs.org/dist/v24.18.1/node-v24.18.1-win-x64.zip' `
    -ArchiveName 'node-v24.18.1-win-x64.zip' `
    -Destination (Join-Path $runtimeRoot 'node') `
    -Marker (Join-Path $runtimeRoot 'node\node-v24.18.1-win-x64\node.exe')

Install-Archive -Name 'Eclipse Temurin JDK 21.0.11+10' `
    -Url 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.11%2B10/OpenJDK21U-jdk_x64_windows_hotspot_21.0.11_10.zip' `
    -ArchiveName 'OpenJDK21U-jdk_x64_windows_hotspot_21.0.11_10.zip' `
    -Destination (Join-Path $runtimeRoot 'jdk') `
    -Marker (Join-Path $runtimeRoot 'jdk\jdk-21.0.11+10\bin\java.exe')

Install-Archive -Name 'Apache Maven 3.9.16' `
    -Url 'https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip' `
    -ArchiveName 'apache-maven-3.9.16-bin.zip' `
    -Destination (Join-Path $runtimeRoot 'maven') `
    -Marker (Join-Path $runtimeRoot 'maven\apache-maven-3.9.16\bin\mvn.cmd')

$nodeHome = Join-Path $runtimeRoot 'node\node-v24.18.1-win-x64'
$pnpmRoot = Join-Path $runtimeRoot 'pnpm'
$pnpmCmd = Join-Path $pnpmRoot 'node_modules\.bin\pnpm.cmd'
$env:Path = "$nodeHome;$env:Path"

if (-not (Test-Path -LiteralPath $pnpmCmd)) {
    Write-Host '[install] pnpm 10.17.1 into project .runtime'
    & (Join-Path $nodeHome 'npm.cmd') install --prefix $pnpmRoot pnpm@10.17.1 --no-package-lock --registry https://registry.npmjs.org
}

Write-Host '[install] frontend dependencies into project directories'
& $pnpmCmd install --dir $projectRoot --registry https://registry.npmjs.org --store-dir (Join-Path $runtimeRoot 'pnpm-store')

$env:JAVA_HOME = Join-Path $runtimeRoot 'jdk\jdk-21.0.11+10'
$mavenHome = Join-Path $runtimeRoot 'maven\apache-maven-3.9.16'
$env:Path = "$env:JAVA_HOME\bin;$mavenHome\bin;$env:Path"
Write-Host '[prepare] backend dependencies in project Maven repository'
& (Join-Path $mavenHome 'bin\mvn.cmd') `
    "-Dmaven.repo.local=$(Join-Path $runtimeRoot 'maven-repository')" `
    -s (Join-Path $projectRoot 'scripts\maven-settings.xml') `
    -f (Join-Path $projectRoot 'backend\pom.xml') `
    dependency:go-offline

Write-Host ''
Write-Host 'Project-local runtime is ready. No global PATH, registry, or Windows service was changed.' -ForegroundColor Green
