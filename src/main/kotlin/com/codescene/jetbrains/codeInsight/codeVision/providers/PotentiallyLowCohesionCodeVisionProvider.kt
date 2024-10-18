package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class PotentiallyLowCohesionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Potentially Low Cohesion"
    override val id = "codeVision.codescene.potentiallyLowCohesion"
    override val name = "com.codescene.codeVision.potentiallyLowCohesion"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}