package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.COMPLEX_CONDITIONAL
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class ComplexConditionalCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = COMPLEX_CONDITIONAL

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}