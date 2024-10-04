package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LargeMethodCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Large Method"
    override val id = "codeVision.codescene.largeMethod"
    override val name = "com.codescene.codeVision.largeMethod"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}