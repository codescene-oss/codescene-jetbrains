package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LowCohesionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Low Cohesion"
    override val id = "codeVision.codescene.lowCohesion"
    override val name = "com.codescene.codeVision.lowCohesion"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}