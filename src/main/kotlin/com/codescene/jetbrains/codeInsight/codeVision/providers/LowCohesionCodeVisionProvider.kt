package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LowCohesionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Low Cohesion"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}