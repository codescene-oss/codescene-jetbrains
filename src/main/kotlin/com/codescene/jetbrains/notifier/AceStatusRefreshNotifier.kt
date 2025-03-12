package com.codescene.jetbrains.notifier

import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.util.messages.Topic

const val ACE_STATUS_NOTIFIER = "Refresh $CODESCENE ACE status"

interface AceStatusRefreshNotifier {
    fun refresh(invertLogic: Boolean)

    companion object {
        val TOPIC: Topic<AceStatusRefreshNotifier> =
            Topic.create(ACE_STATUS_NOTIFIER, AceStatusRefreshNotifier::class.java)
    }
}



