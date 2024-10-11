package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LinesOfCodeInSingleFileCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Lines of Code in a Single File"
    override val id = "codeVision.codescene.locSingleFile"
    override val name = "com.codescene.codeVision.locSingleFile"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}