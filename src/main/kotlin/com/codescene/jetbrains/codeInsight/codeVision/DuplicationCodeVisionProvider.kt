package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class DuplicationCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Code Duplication"
    override val id = "codeVision.codescene.codeDuplication"
    override val name = "com.codescene.codeVision.codeDuplication"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}