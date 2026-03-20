package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.platform.UiLabelsBundle
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import java.awt.Color

object PlatformConstants {
    const val CODESCENE_PLUGIN_ID = "com.codescene.vanilla"
    const val TELEMETRY_EDITOR_TYPE = "jetbrains"

    const val INFO_NOTIFICATION_GROUP = "CodeScene Information"
    const val ERROR_NOTIFICATION_GROUP = "CodeScene Error"
    const val ACE_NOTIFICATION_GROUP = "CodeScene ACE"
    const val ACE_STATUS = "CodeScene ACE Status"

    val RED = JBColor(Color(224, 82, 92), Color(224, 82, 92))
    val GREEN = JBColor(Color(79, 159, 120), Color(79, 159, 120))
    val ORANGE = JBColor(Color(250, 163, 125), Color(238, 147, 107))
    val BLUE = ColorUtil.fromHex("#3f6dc7")

    val codeSceneWindowFileNames =
        listOf(
            UiLabelsBundle.message("ace"),
            UiLabelsBundle.message("codeSmellDocs"),
            UiLabelsBundle.message("aceAcknowledge"),
        )
}
