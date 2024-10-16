package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class DuplicatedAssertionBlocks : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Duplicated Assertion Blocks"
    override val id = "codeVision.codescene.duplicatedAssertionBlocks"
    override val name = "com.codescene.codeVision.duplicatedAssertionBlocks"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}