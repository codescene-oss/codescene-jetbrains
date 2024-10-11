package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LargeMethodCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Large Method"
    override val id = "codeVision.codescene.largeMethod"
    override val name = "com.codescene.codeVision.largeMethod"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}