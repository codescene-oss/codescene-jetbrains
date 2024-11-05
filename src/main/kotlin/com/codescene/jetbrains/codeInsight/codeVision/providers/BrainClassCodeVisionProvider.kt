package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.BRAIN_CLASS
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class BrainClassCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = BRAIN_CLASS

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}