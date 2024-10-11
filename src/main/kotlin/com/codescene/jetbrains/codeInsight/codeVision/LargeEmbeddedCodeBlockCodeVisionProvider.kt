package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LargeEmbeddedCodeBlockCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Large Embedded Code Block"
    override val id = "codeVision.codescene.largeEmbeddedCodeBlock"
    override val name = "com.codescene.codeVision.largeEmbeddedCodeBlock"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}