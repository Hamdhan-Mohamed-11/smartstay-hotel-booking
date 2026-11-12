$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force out | Out-Null
$sources = @((Get-ChildItem -Path "src/smartstay/*.java","test/smartstay/*.java") | ForEach-Object { $_.FullName })
& javac -Xlint:all -d out $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp out smartstay.EUK00361749_Tests
exit $LASTEXITCODE
