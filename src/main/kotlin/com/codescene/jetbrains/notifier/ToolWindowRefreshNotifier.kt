package com.codescene.jetbrains.notifier

import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic

const val DISPLAY_NAME = "Refresh $CODESCENE Tool Window"

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
interface ToolWindowRefreshNotifier {
    fun refresh(
        file: VirtualFile?,
        shouldCollapseTree: Boolean = false,
    )

    fun invalidateAndRefresh(
        fileToInvalidate: String,
        file: VirtualFile? = null,
    )

    companion object {
        val TOPIC: Topic<ToolWindowRefreshNotifier> =
            Topic.create(DISPLAY_NAME, ToolWindowRefreshNotifier::class.java)
    }
}
