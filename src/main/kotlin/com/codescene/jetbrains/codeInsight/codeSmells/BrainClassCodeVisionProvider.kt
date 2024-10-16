package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class BrainClassCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "BrainClass"
    override val id = "codeVision.codescene.brainClass"
    override val name = "com.codescene.codeVision.brainClass"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}