package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.STRING_HEAVY_FUNCTION_ARGUMENTS
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class StringHeavyArgumentsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = STRING_HEAVY_FUNCTION_ARGUMENTS

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}