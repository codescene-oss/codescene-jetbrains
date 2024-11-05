package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.LARGE_EMBEDDED_CODE_BLOCK
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LargeEmbeddedCodeBlockCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = LARGE_EMBEDDED_CODE_BLOCK

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}