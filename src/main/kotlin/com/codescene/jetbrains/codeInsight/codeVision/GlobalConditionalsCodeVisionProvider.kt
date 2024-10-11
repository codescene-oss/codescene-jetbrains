package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class GlobalConditionalsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Global Conditionals"
    override val id = "codeVision.codescene.globalConditionals"
    override val name = "com.codescene.codeVision.globalConditionals"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}