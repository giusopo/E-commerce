# Genera le password usate dai secrets di Docker Compose (docker-compose.yml).
# I file vengono creati in secrets/, cartella esclusa da Git, solo se non esistono:
# eseguire di nuovo lo script non cambia password gia' in uso dal volume del database.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$secretsDirectory = Join-Path $projectRoot 'secrets'

# Password casuale di 32 caratteri alfanumerici da un generatore crittografico.
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

New-Item -ItemType Directory -Path $secretsDirectory -Force | Out-Null
foreach ($name in 'db_root_password', 'db_password') {
    $path = Join-Path $secretsDirectory "$name.txt"
    if (Test-Path -LiteralPath $path) {
        Write-Host "secrets/$name.txt esiste gia': non modificato."
    } else {
        # Senza a capo finale: il contenuto del file e' esattamente la password.
        [System.IO.File]::WriteAllText($path, (New-Secret), (New-Object System.Text.UTF8Encoding $false))
        Write-Host "secrets/$name.txt creato."
    }
}
Write-Host 'Secrets pronti. Non condividere e non aggiungere a Git la cartella secrets/.'
