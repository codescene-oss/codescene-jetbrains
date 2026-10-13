package com.codescene.jetbrains.core.delta

fun presentedDeltaContents(
    scoredText: String?,
    oldGitBlobSha: String?,
    newGitBlobSha: String?,
): Pair<String, String> {
    val currentContent = scoredText ?: "blob:${newGitBlobSha.orEmpty()}"
    val headContent =
        if (oldGitBlobSha == newGitBlobSha) {
            currentContent
        } else {
            "baseline:${oldGitBlobSha.orEmpty()}"
        }
    return headContent to currentContent
}
