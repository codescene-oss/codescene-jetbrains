package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.LINES_OF_CODE_IN_A_SINGLE_FILE
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LinesOfCodeInSingleFileCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = LINES_OF_CODE_IN_A_SINGLE_FILE

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}