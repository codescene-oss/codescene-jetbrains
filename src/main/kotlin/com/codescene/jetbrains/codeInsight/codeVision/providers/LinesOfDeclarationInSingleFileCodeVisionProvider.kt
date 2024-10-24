package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LinesOfDeclarationInSingleFileCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Lines of Declaration in a Single File"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}