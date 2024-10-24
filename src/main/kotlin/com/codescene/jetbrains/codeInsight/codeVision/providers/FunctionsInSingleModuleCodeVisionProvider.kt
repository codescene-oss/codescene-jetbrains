package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class FunctionsInSingleModuleCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Number of Functions in a Single Module"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}