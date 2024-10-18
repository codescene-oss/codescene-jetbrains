package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class BrainClassCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Brain Class"
    override val id = "codeVision.codescene.brainClass"
    override val name = "com.codescene.codeVision.brainClass"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}