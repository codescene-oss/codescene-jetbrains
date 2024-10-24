package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.LARGE_ASSERTION_BLOCKS
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LargeAssertionBlocksCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = LARGE_ASSERTION_BLOCKS

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}