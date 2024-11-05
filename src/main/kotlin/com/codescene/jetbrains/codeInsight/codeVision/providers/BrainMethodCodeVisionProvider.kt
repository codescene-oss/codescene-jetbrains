package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.BRAIN_METHOD
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class BrainMethodCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = BRAIN_METHOD

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}
