$processes = Get-CimInstance Win32_Process | Where-Object {
    $_.CommandLine -and (
        ($_.CommandLine -like '*idea-sandbox*codescene-jetbrains*') -or
        ($_.CommandLine -like '*GradleWrapperMain*runIde*') -or
        ($_.CommandLine -like '*make*run-ide*')
    )
}

$processes | ForEach-Object {
    Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
}
