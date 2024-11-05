package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.DUPLICATED_ASSERTION_BLOCKS
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class DuplicatedAssertionBlocksCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = DUPLICATED_ASSERTION_BLOCKS

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}