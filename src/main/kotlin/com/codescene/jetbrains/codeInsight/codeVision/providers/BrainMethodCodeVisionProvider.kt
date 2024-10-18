package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class BrainMethodCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Brain Method"
    override val id = "codeVision.codescene.brainMethod"
    override val name = "com.codescene.codeVision.brainMethod"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}
