$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$serverDirectory = Join-Path $projectRoot '.tools/mariadb-11.4.13-winx64'
# Credenziali amministratore generate da Start-Database.ps1, escluse da Git.
$rootOptionFile = Join-Path $projectRoot '.local-db/root.cnf'
$listener = Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $listener) {
    Write-Host 'Il database locale e gia arrestato.'
    exit 0
}
$serverProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
if ($serverProcess.ExecutablePath -ne (Join-Path $serverDirectory 'bin/mariadbd.exe')) {
    throw 'La porta 3307 appartiene a un altro programma: nessun processo e stato arrestato.'
}
if (-not (Test-Path -LiteralPath $rootOptionFile)) {
    throw "File $rootOptionFile assente: eseguire Start-Database.ps1 per rigenerare le credenziali."
}

& "$serverDirectory/bin/mariadb-admin.exe" "--defaults-file=$rootOptionFile" --protocol=tcp --host=127.0.0.1 --port=3307 shutdown
if ($LASTEXITCODE -ne 0) { throw 'Arresto del database locale fallito.' }
Write-Host 'Database locale arrestato. I dati sono conservati in .local-db.'
