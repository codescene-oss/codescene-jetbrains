package com.codescene.jetbrains.notifier

import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.openapi.editor.Editor
import com.intellij.util.messages.Topic

const val DISPLAY_NAME = "Refresh $CODESCENE Tool Window"

interface ToolWindowRefreshNotifier {
    fun refresh(editor: Editor)

    companion object {
        val TOPIC: Topic<ToolWindowRefreshNotifier> =
            Topic.create(DISPLAY_NAME, ToolWindowRefreshNotifier::class.java)
    }
}