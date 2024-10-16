package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class LargeAssertionBlocksCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Large Assertion Blocks"
    override val id = "codeVision.codescene.largeAssertionBlocks"
    override val name = "com.codescene.codeVision.largeAssertionBlocks"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}