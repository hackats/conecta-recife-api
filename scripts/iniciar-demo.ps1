param([int]$Porta = 8080)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $raiz 'target/conecta-recife-api-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    throw 'Gere o JAR primeiro com mvn package.'
}
foreach ($nome in @('cidadaos.csv', 'servicos.csv', 'historico.csv')) {
    if (-not (Test-Path -LiteralPath (Join-Path $raiz "data/$nome"))) {
        throw "CSV local ausente: $nome"
    }
}
Push-Location $raiz
try {
    & java -jar $jar "--server.port=$Porta"
    if ($LASTEXITCODE -ne 0) { throw "A API encerrou com código $LASTEXITCODE" }
} finally {
    Pop-Location
}
