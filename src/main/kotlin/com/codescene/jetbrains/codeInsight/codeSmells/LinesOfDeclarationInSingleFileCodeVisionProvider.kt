package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LinesOfDeclarationInSingleFileCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Lines of Declaration in a Single File"
    override val id = "codeVision.codescene.linesOfDeclarationSingleFile"
    override val name = "com.codescene.codeVision.linesOfDeclarationSingleFile"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}