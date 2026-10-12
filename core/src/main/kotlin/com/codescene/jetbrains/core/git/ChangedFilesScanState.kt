package com.codescene.jetbrains.core.git

class ChangedFilesScanState {
    var lastChangedFileSetKey: String? = null
    var forceNextScan: Boolean = false

    fun markDirty() {
        forceNextScan = true
    }

    fun shouldSkipIdleScan(hadWorkspaceActivity: Boolean): Boolean =
        !forceNextScan && !hadWorkspaceActivity && lastChangedFileSetKey != null

    fun isUnchangedFileSet(changedFileSetKey: String): Boolean =
        !forceNextScan && changedFileSetKey == lastChangedFileSetKey

    fun recordScan(changedFileSetKey: String) {
        forceNextScan = false
        lastChangedFileSetKey = changedFileSetKey
    }
}

fun serializeChangedFileSet(filePaths: Set<String>): String = filePaths.sorted().joinToString("\u0000")

fun sortFilesByPriority(
    filePaths: Set<String>,
    visibleFiles: Set<String>,
): List<String> {
    val visibleKeys = visibleFiles.map { pathComparisonKey(it) }.toSet()
    val visible = mutableListOf<String>()
    val hidden = mutableListOf<String>()
    for (filePath in filePaths) {
        if (pathComparisonKey(filePath) in visibleKeys) {
            visible.add(filePath)
        } else {
            hidden.add(filePath)
        }
    }
    visible.sort()
    hidden.sort()
    return visible + hidden
}
