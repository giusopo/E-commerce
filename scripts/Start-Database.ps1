$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsDirectory = Join-Path $projectRoot '.tools'
$databaseDirectory = Join-Path $projectRoot '.local-db'
$dataDirectory = Join-Path $databaseDirectory 'data'
$version = '11.4.13'
$archiveName = "mariadb-$version-winx64.zip"
$archivePath = Join-Path $toolsDirectory $archiveName
$serverDirectory = Join-Path $toolsDirectory "mariadb-$version-winx64"
$expectedHash = 'd62986d433eeebfde218560b276103831604a61e929e87f1a17f5aebd80257e2'
$serverPort = 3307

New-Item -ItemType Directory -Path $toolsDirectory, $databaseDirectory -Force | Out-Null
if (-not (Test-Path -LiteralPath "$serverDirectory/bin/mariadbd.exe")) {
    if (-not (Test-Path -LiteralPath $archivePath)) {
        Write-Host "Scaricamento MariaDB $version per Windows (solo al primo avvio)..."
        Invoke-WebRequest -UseBasicParsing "https://archive.mariadb.org/mariadb-$version/winx64-packages/$archiveName" -OutFile $archivePath
    }
    if ((Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) {
        throw "Checksum MariaDB errato. Rimuovere solo $archivePath e riprovare."
    }
    Write-Host 'Estrazione MariaDB...'
    Expand-Archive -LiteralPath $archivePath -DestinationPath $toolsDirectory -Force
}

$serverExecutable = Join-Path $serverDirectory 'bin/mariadbd.exe'
$clientExecutable = Join-Path $serverDirectory 'bin/mariadb.exe'

function Invoke-LocalSql([string] $Sql, [string] $Database = '') {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $clientExecutable
    $startInfo.Arguments = "--no-defaults --protocol=tcp --host=127.0.0.1 --port=$serverPort --user=root --password=root --batch --binary-mode --skip-column-names --default-character-set=utf8mb4 $Database"
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process = [System.Diagnostics.Process]::Start($startInfo)
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    # Su Windows StandardInput usa la code page OEM: corromperebbe gli apostrofi
    # tipografici del dump. Inviare esplicitamente i byte UTF-8 al client.
    $sqlBytes = [System.Text.Encoding]::UTF8.GetBytes($Sql)
    $process.StandardInput.BaseStream.Write($sqlBytes, 0, $sqlBytes.Length)
    $process.StandardInput.Close()
    $process.WaitForExit()
    $output = $stdout.Result.Trim()
    $errorOutput = $stderr.Result.Trim()
    if ($process.ExitCode -ne 0) {
        throw "Errore MariaDB: $errorOutput"
    }
    return $output
}

$listener = Get-NetTCPConnection -LocalPort $serverPort -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    $serverProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
    if ($serverProcess.ExecutablePath -ne $serverExecutable) {
        throw "Porta $serverPort occupata da un altro programma. Il database locale non e' stato avviato."
    }
} else {
    if (-not (Test-Path -LiteralPath "$dataDirectory/mysql")) {
        Write-Host 'Inizializzazione database locale...'
        & "$serverDirectory/bin/mariadb-install-db.exe" "--datadir=$dataDirectory" '--password=root'
        if ($LASTEXITCODE -ne 0) { throw 'Inizializzazione MariaDB fallita.' }
    }
    $serverArguments = @(
        "--defaults-file=`"$dataDirectory/my.ini`"",
        "--datadir=`"$dataDirectory`"",
        "--port=$serverPort",
        '--bind-address=127.0.0.1',
        '--skip-name-resolve',
        "--pid-file=`"$databaseDirectory/mariadb.pid`"",
        "--log-error=`"$databaseDirectory/mariadb.log`""
    )
    $server = Start-Process -FilePath $serverExecutable -ArgumentList $serverArguments -WindowStyle Hidden -PassThru
    $ready = $false
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        if ($server.HasExited) { throw "MariaDB terminato. Consultare $databaseDirectory/mariadb.log" }
        try {
            $null = Invoke-LocalSql 'SELECT 1;'
            $ready = $true
            break
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
    if (-not $ready) { throw "Database non pronto. Consultare $databaseDirectory/mariadb.log" }
}

$null = Invoke-LocalSql 'CREATE DATABASE IF NOT EXISTS ecommerce CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;'
$seedMarker = Join-Path $databaseDirectory 'seed-imported'
if (-not (Test-Path -LiteralPath $seedMarker)) {
    $tableCount = Invoke-LocalSql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'ecommerce';"
    if ([int] $tableCount -gt 0) {
        throw 'Il database locale contiene gia tabelle senza il marcatore seed-imported. Verificare l''importazione prima di proseguire: nessun dato e'' stato sovrascritto.'
    }
    Write-Host 'Importazione catalogo, immagini e dati dal dump del progetto...'
    $dump = [System.IO.File]::ReadAllText((Join-Path $projectRoot 'src/main/db/ecommerce.sql'), [System.Text.Encoding]::UTF8)
    $null = Invoke-LocalSql $dump 'ecommerce'
    Set-Content -LiteralPath $seedMarker -Value $version -Encoding ASCII
}

$null = Invoke-LocalSql @'
CREATE USER IF NOT EXISTS 'beautyblind'@'127.0.0.1' IDENTIFIED BY 'beautyblind';
GRANT ALL PRIVILEGES ON ecommerce.* TO 'beautyblind'@'127.0.0.1';
'@
Write-Host "Database BeautyBlind pronto su 127.0.0.1:$serverPort. I dati sono conservati in .local-db."
