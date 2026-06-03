package com.codescene.jetbrains.core.git

data class MainLineBranchContext(
    val configuredBaseline: String? = null,
    val defaultBranchFromOriginHead: String? = null,
    val localBranchNames: Set<String> = emptySet(),
) {
    fun isMainLineBranch(branchName: String): Boolean {
        val normalized = branchName.trim()
        if (normalized.isEmpty()) return false

        configuredBaseline?.let { baseline ->
            return normalized.equals(baseline, ignoreCase = true)
        }

        defaultBranchFromOriginHead?.let { defaultBranch ->
            return normalized.equals(defaultBranch, ignoreCase = true)
        }

        return MAIN_LINE_BRANCH_NAMES.any { it.equals(normalized, ignoreCase = true) }
    }

    fun refsForMergeBaseProbe(): List<String> {
        val refs = linkedSetOf<String>()

        configuredBaseline?.let {
            appendBranchRefs(refs, it)
            return refs.toList()
        }

        defaultBranchFromOriginHead?.let {
            appendBranchRefs(refs, it)
            return refs.toList()
        }

        val fallbackBranches =
            MAIN_LINE_BRANCH_NAMES.filter { name ->
                localBranchNames.any { local -> local.equals(name, ignoreCase = true) }
            }
        for (branch in fallbackBranches) {
            appendBranchRefs(refs, branch)
        }

        return refs.toList()
    }

    private fun appendBranchRefs(
        refs: LinkedHashSet<String>,
        branchName: String,
    ) {
        refs.add(branchName)
        refs.add("origin/$branchName")
    }
}
