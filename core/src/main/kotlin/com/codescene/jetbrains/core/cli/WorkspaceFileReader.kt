package com.codescene.jetbrains.core.cli

data class CurrentFile(
    val gitBlobSha: String,
    val text: String?,
)

class WorkspaceFileReader(
    private val openText: (String) -> String?,
    private val readBytes: (String) -> ByteArray?,
) {
    fun current(absolutePath: String): CurrentFile? {
        val text = openText(absolutePath)
        if (text != null) return CurrentFile(GitBlobSha.ofString(text), text)
        val bytes = readBytes(absolutePath) ?: return null
        return CurrentFile(GitBlobSha.ofBytes(bytes), null)
    }
}

data class ObservedFile(
    val absolutePath: String,
    val repoRoot: String,
    val current: CurrentFile,
)
