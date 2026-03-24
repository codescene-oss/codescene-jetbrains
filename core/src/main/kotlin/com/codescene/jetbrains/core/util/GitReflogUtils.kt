package com.codescene.jetbrains.core.util

fun parseBranchCreationCommitFromReflog(lines: List<String>): String? =
    lines
        .asReversed()
        .find { it.contains("created from", ignoreCase = true) }
        ?.split(" ")
        ?.getOrNull(0)
