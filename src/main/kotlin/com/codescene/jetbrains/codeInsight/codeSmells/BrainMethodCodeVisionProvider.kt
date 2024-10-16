package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class BrainMethodCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Brain Method"
    override val id = "codeVision.codescene.brainMethod"
    override val name = "com.codescene.codeVision.brainMethod"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}
