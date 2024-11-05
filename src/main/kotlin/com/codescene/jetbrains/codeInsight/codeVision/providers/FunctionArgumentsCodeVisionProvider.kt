package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.EXCESS_NUMBER_OF_FUNCTION_ARGUMENTS
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class FunctionArgumentsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = EXCESS_NUMBER_OF_FUNCTION_ARGUMENTS

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}