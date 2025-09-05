param(
  [Parameter(Mandatory=$true)][string]$FunctionName
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$fnDir = Join-Path $root "functions/$FunctionName"
if (!(Test-Path $fnDir)) { throw "Function folder not found: $fnDir" }

$outDir = Join-Path $root "dist"
if (!(Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }

$zipPath = Join-Path $outDir ("$FunctionName.zip")
if (Test-Path $zipPath) { Remove-Item $zipPath }

Push-Location $fnDir
try {
  if (Test-Path package.json) {
    npm install --omit=dev | Out-Null
  }
  Compress-Archive -Path * -DestinationPath $zipPath -Force
} finally {
  Pop-Location
}

Write-Host "Created: $zipPath"


