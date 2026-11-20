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

# Credenziali generate al primo avvio e conservate solo in .local-db/ (escluso da Git).
# root.cnf: file di opzioni MariaDB dell'amministratore, letto dal client con --defaults-file.
# credentials.properties: DB_URL, DB_USER e DB_PASSWORD dell'applicazione, letti da dev.LocalServer.
$rootOptionFile = Join-Path $databaseDirectory 'root.cnf'
$appCredentialsFile = Join-Path $databaseDirectory 'credentials.properties'
$appUser = 'beautyblind_app'
# Profilo applicativo delle versioni precedenti, con password pubblicata nel repository.
$legacyAppUser = 'beautyblind'
# Password root delle versioni precedenti, pubblicata nel repository: usata solo per sostituirla.
$legacyRootPassword = 'root'

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

# Password casuale di 32 caratteri alfanumerici da un generatore crittografico.
# Solo lettere e cifre: nessun carattere da proteggere in SQL, .cnf o .properties.
function New-Secret([int] $Length = 32) {
    $alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    $generator = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
    $buffer = New-Object byte[] 1
    $secret = New-Object System.Text.StringBuilder
    try {
        while ($secret.Length -lt $Length) {
            $generator.GetBytes($buffer)
            # 248 = 4 * 62: scarta i byte superiori per non favorire alcun carattere.
            if ($buffer[0] -lt 248) { $null = $secret.Append($alphabet[$buffer[0] % 62]) }
        }
    } finally {
        $generator.Dispose()
    }
    return $secret.ToString()
}

function Write-RootOptionFile([string] $Path, [string] $Password) {
    Set-Content -LiteralPath $Path -Encoding ASCII -Value @(
        '# Generato da scripts/Start-Database.ps1. Non condividere e non aggiungere a Git.',
        '[client]',
        'user=root',
        "password=$Password"
    )
}

function Read-RootPassword([string] $Path) {
    $line = Get-Content -LiteralPath $Path | Where-Object { $_ -match '^password=' } | Select-Object -First 1
    return $line.Substring('password='.Length)
}

function Test-RootLogin {
    try {
        $null = Invoke-LocalSql 'SELECT 1;'
        return $true
    } catch {
        return $false
    }
}

function Read-Properties([string] $Path) {
    $values = @{}
    if (Test-Path -LiteralPath $Path) {
        foreach ($line in Get-Content -LiteralPath $Path) {
            if ($line -match '^\s*([A-Za-z_]+)\s*=\s*(.*?)\s*$') { $values[$Matches[1]] = $Matches[2] }
        }
    }
    return $values
}

function Invoke-LocalSql([string] $Sql, [string] $Database = '') {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $clientExecutable
    # --defaults-file deve essere la prima opzione: il client legge solo quel file e la password
    # non compare tra gli argomenti del processo. --skip-print-query-on-error evita che un errore
    # ristampi l'istruzione SQL, che potrebbe contenere una password.
    $startInfo.Arguments = "--defaults-file=`"$script:rootOptionFile`" --protocol=tcp --host=127.0.0.1 --port=$serverPort --batch --binary-mode --skip-column-names --skip-print-query-on-error --default-character-set=utf8mb4 $Database"
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
}

if (-not (Test-Path -LiteralPath "$dataDirectory/mysql")) {
    if ($listener) { throw "Database in esecuzione senza dati in $dataDirectory. Arrestarlo e riprovare." }
    Write-Host 'Inizializzazione database locale con una password amministratore generata...'
    $rootPassword = New-Secret
    Write-RootOptionFile $rootOptionFile $rootPassword
    # mariadb-install-db per Windows accetta la password solo da riga di comando,
    # per la durata dell'inizializzazione; le connessioni successive usano root.cnf.
    & "$serverDirectory/bin/mariadb-install-db.exe" "--datadir=$dataDirectory" "--password=$rootPassword"
    if ($LASTEXITCODE -ne 0) { throw 'Inizializzazione MariaDB fallita.' }
} elseif (-not (Test-Path -LiteralPath $rootOptionFile)) {
    # Dati creati da una versione precedente dello script: root ha ancora la password pubblicata.
    Write-Host 'Database creato con le vecchie credenziali pubbliche: verranno sostituite.'
    Write-RootOptionFile $rootOptionFile $legacyRootPassword
}

if (-not $listener) {
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
        $ready = Test-RootLogin
        if ($ready) { break }
        Start-Sleep -Milliseconds 500
    }
}

# Una sostituzione della password interrotta lascia la nuova password in root.cnf.new:
# se root.cnf non e' piu' valido, si prova quella e, se funziona, diventa definitiva.
$pendingRootOptionFile = "$rootOptionFile.new"
if (-not (Test-RootLogin) -and (Test-Path -LiteralPath $pendingRootOptionFile)) {
    $currentRootOptionFile = $rootOptionFile
    $script:rootOptionFile = $pendingRootOptionFile
    if (Test-RootLogin) {
        Move-Item -LiteralPath $pendingRootOptionFile -Destination $currentRootOptionFile -Force
    }
    $script:rootOptionFile = $currentRootOptionFile
}
if (-not (Test-RootLogin)) {
    throw "Database non pronto o password amministratore non valida. Consultare $databaseDirectory/mariadb.log"
}

# Allinea tutti gli account root di loopback alla password di root.cnf (o a una nuova password
# se erano ancora quelle pubblicate) ed elimina gli account root legati al nome del computer:
# con --skip-name-resolve non possono autenticarsi e conserverebbero la vecchia password.
$rootPassword = Read-RootPassword $rootOptionFile
# Si sostituisce ogni volta che root.cnf contiene ancora la password pubblicata,
# anche se una sostituzione precedente si era interrotta prima di modificare MariaDB.
$rotateLegacyRoot = $rootPassword -ceq $legacyRootPassword
if ($rotateLegacyRoot) {
    $rootPassword = New-Secret
    # Scritta prima di modificare MariaDB: se lo script si interrompe, il prossimo avvio la ritrova.
    Write-RootOptionFile $pendingRootOptionFile $rootPassword
}
$rootAccounts = Invoke-LocalSql "SELECT CONCAT(QUOTE(User), '@', QUOTE(Host), ' ', Host IN ('127.0.0.1', '::1', 'localhost')) FROM mysql.user WHERE User = 'root';"
$rootStatements = foreach ($row in ($rootAccounts -split "`r?`n" | Where-Object { $_ })) {
    $account, $isLoopback = $row -split ' '
    if ($isLoopback -eq '1') { "ALTER USER $account IDENTIFIED BY '$rootPassword';" } else { "DROP USER $account;" }
}
$null = Invoke-LocalSql ($rootStatements -join "`n")
if ($rotateLegacyRoot) {
    Move-Item -LiteralPath $pendingRootOptionFile -Destination $rootOptionFile -Force
    Write-Host 'Password amministratore sostituita.'
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

# Profilo applicativo: password generata, privilegi limitati ai dati di ecommerce
# (l'applicazione esegue solo SELECT, INSERT, UPDATE e DELETE).
$appCredentials = Read-Properties $appCredentialsFile
$appPassword = $appCredentials['DB_PASSWORD']
if (-not $appPassword -or $appPassword -notmatch '^[A-Za-z0-9]{32}$') { $appPassword = New-Secret }
$null = Invoke-LocalSql @"
DROP USER IF EXISTS '$legacyAppUser'@'127.0.0.1';
CREATE USER IF NOT EXISTS '$appUser'@'127.0.0.1' IDENTIFIED BY '$appPassword';
ALTER USER '$appUser'@'127.0.0.1' IDENTIFIED BY '$appPassword';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM '$appUser'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ecommerce.* TO '$appUser'@'127.0.0.1';
"@
Set-Content -LiteralPath $appCredentialsFile -Encoding ASCII -Value @(
    '# Generato da scripts/Start-Database.ps1. Non condividere e non aggiungere a Git.',
    "DB_URL=jdbc:mysql://127.0.0.1:$serverPort/ecommerce?serverTimezone=UTC&connectTimeout=5000",
    "DB_USER=$appUser",
    "DB_PASSWORD=$appPassword"
)
Write-Host "Database BeautyBlind pronto su 127.0.0.1:$serverPort. I dati sono conservati in .local-db."
Write-Host "Credenziali locali generate in .local-db (root.cnf, credentials.properties), escluse da Git."
