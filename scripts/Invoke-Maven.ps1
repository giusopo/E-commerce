param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArguments
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

# Alcuni PC hanno JAVA_HOME obsoleto anche quando java e javac funzionano nel PATH.
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath "$env:JAVA_HOME/bin/javac.exe")) {
    $javaInfo = New-Object System.Diagnostics.ProcessStartInfo
    $javaInfo.FileName = 'java'
    $javaInfo.Arguments = '-XshowSettings:properties -version'
    $javaInfo.UseShellExecute = $false
    $javaInfo.CreateNoWindow = $true
    $javaInfo.RedirectStandardError = $true
    $javaProcess = [System.Diagnostics.Process]::Start($javaInfo)
    $javaSettings = $javaProcess.StandardError.ReadToEnd()
    $javaProcess.WaitForExit()
    if ($javaSettings -match 'java\.home\s*=\s*([^\r\n]+)') {
        $detectedJavaHome = $Matches[1].Trim()
        if (Test-Path -LiteralPath "$detectedJavaHome/bin/javac.exe") {
            $env:JAVA_HOME = $detectedJavaHome
        }
    }
    if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath "$env:JAVA_HOME/bin/javac.exe")) {
        throw 'Installare un JDK e configurare JAVA_HOME con la sua cartella.'
    }
}

Push-Location $projectRoot
try {
    & "$projectRoot/mvnw.cmd" @MavenArguments
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
