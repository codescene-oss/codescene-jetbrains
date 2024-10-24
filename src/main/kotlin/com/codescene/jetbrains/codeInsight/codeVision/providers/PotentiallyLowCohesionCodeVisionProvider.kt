package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.POTENTIALLY_LOW_COHESION
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class PotentiallyLowCohesionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = POTENTIALLY_LOW_COHESION

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}