package com.codescene.jetbrains.notifier

import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.util.messages.Topic

const val CODE_HEALTH_DETAILS_NOTIFIER = "Refresh $CODESCENE Code Health Details"

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
interface CodeHealthDetailsRefreshNotifier {
    fun refresh(finding: CodeHealthFinding?)

    companion object {
        val TOPIC: Topic<CodeHealthDetailsRefreshNotifier> =
            Topic.create(CODE_HEALTH_DETAILS_NOTIFIER, CodeHealthDetailsRefreshNotifier::class.java)
    }
}
