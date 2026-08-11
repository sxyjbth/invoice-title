[CmdletBinding()]
param(
    [switch]$UseLocalMySql
)

$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'start-infrastructure.ps1') -UseLocalMySql:$UseLocalMySql
& (Join-Path $PSScriptRoot 'start-applications.ps1')
& (Join-Path $PSScriptRoot 'status.ps1')
