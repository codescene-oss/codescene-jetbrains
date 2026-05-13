package com.codescene.jetbrains.core.git

val MAIN_LINE_BRANCH_NAMES =
    listOf("main", "master", "develop", "trunk", "dev", "development")

fun mainLineRefsInProbeOrder(): List<String> = MAIN_LINE_BRANCH_NAMES.flatMap { listOf(it, "origin/$it") }

fun collectOrderedUniqueMergeBases(mergeBaseForRef: (ref: String) -> String?): List<String> {
    val ordered = mutableListOf<String>()
    val seen = mutableSetOf<String>()
    for (ref in mainLineRefsInProbeOrder()) {
        val sha = mergeBaseForRef(ref)?.trim()?.takeIf { it.isNotEmpty() } ?: continue
        if (seen.add(sha)) {
            ordered.add(sha)
        }
    }
    return ordered
}

fun selectClosestMergeBase(
    orderedUniqueMergeBases: List<String>,
    isAncestor: (ancestor: String, descendant: String) -> Boolean,
): String? {
    if (orderedUniqueMergeBases.isEmpty()) return null
    if (orderedUniqueMergeBases.size == 1) return orderedUniqueMergeBases.first()
    val winners =
        orderedUniqueMergeBases.filter { candidate ->
            orderedUniqueMergeBases.all { other ->
                other == candidate || isAncestor(other, candidate)
            }
        }
    return when (winners.size) {
        1 -> winners.first()
        else -> orderedUniqueMergeBases.first()
    }
}

fun resolveClosestMainLineMergeBase(
    isAncestor: (ancestor: String, descendant: String) -> Boolean,
    mergeBaseForRef: (ref: String) -> String?,
): String? {
    val ordered = collectOrderedUniqueMergeBases(mergeBaseForRef)
    return selectClosestMergeBase(ordered, isAncestor)
}
