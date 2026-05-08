$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root 'backend\songshanhu-blog-backend'
$frontend = Join-Path $root 'frontend'

$backendArgs = @(
  '-DskipTests',
  'spring-boot:run',
  '-Dspring-boot.run.arguments=--server.port=8082'
)

$frontendArgs = @(
  'run',
  'dev',
  '--',
  '--host',
  '--port',
  '3000'
)

$backendProc = Start-Process -FilePath 'mvn' -ArgumentList $backendArgs -WorkingDirectory $backend -PassThru
Start-Sleep -Seconds 2
$frontendProc = Start-Process -FilePath 'npm' -ArgumentList $frontendArgs -WorkingDirectory $frontend -PassThru

Write-Host ("Backend PID={0}  Frontend PID={1}" -f $backendProc.Id, $frontendProc.Id)
Write-Host "Frontend: http://localhost:3000/"
Write-Host "Backend:   http://localhost:8082/api"

Wait-Process -Id @($backendProc.Id, $frontendProc.Id)

