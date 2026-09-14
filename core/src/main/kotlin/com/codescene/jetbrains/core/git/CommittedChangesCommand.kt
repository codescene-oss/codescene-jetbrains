package com.codescene.jetbrains.core.git

fun committedChangesLogArgs(baseCommit: String): List<String> =
    listOf(
        "--first-parent",
        "--diff-merges=off",
        "--name-only",
        "--pretty=format:",
        "--diff-filter=ACMR",
        "$baseCommit..HEAD",
    )
