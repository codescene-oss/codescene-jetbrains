$p = "\\?\$((Get-Location).Path)\NUL"
if (Test-Path -LiteralPath $p) {
    Remove-Item -LiteralPath $p -Force -ErrorAction SilentlyContinue
}
