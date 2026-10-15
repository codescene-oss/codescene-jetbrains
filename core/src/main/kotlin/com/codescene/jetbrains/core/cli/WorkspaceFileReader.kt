package com.codescene.jetbrains.core.cli

data class CurrentFile(
    val gitBlobSha: String,
    val text: String?,
)

data class LoadedText(
    val text: String,
    val unsaved: Boolean,
)

class WorkspaceFileReader(
    private val openText: (String) -> LoadedText?,
    private val readBytes: (String) -> ByteArray?,
) {
    fun current(absolutePath: String): CurrentFile? {
        val loaded = openText(absolutePath)
        if (loaded != null && loaded.unsaved) return CurrentFile(GitBlobSha.ofString(loaded.text), loaded.text)
        val bytes = readBytes(absolutePath) ?: return null
        return CurrentFile(GitBlobSha.ofBytes(bytes), null)
    }
}

data class ObservedFile(
    val absolutePath: String,
    val repoRoot: String,
    val current: CurrentFile,
)
