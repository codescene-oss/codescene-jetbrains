package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class StringHeavyArgumentsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "String-Heavy Function Arguments"
    override val id = "codeVision.codescene.stringHeavyArguments"
    override val name = "com.codescene.codeVision.stringHeavyArguments"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}