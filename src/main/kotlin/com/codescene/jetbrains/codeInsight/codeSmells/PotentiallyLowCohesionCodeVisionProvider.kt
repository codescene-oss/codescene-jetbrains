package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class PotentiallyLowCohesionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Potentially Low Cohesion"
    override val id = "codeVision.codescene.potentiallyLowCohesion"
    override val name = "com.codescene.codeVision.potentiallyLowCohesion"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}