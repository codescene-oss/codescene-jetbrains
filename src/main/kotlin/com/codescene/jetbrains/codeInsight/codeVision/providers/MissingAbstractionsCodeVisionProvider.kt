package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.MISSING_ARGUMENTS_ABSTRACTIONS
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class MissingAbstractionsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = MISSING_ARGUMENTS_ABSTRACTIONS

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}