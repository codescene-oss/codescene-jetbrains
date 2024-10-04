package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LowCohesionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Low Cohesion"
    override val id = "codeVision.codescene.lowCohesion"
    override val name = "com.codescene.codeVision.lowCohesion"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}