package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.LARGE_METHOD
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LargeMethodCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = LARGE_METHOD

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}