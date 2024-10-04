package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class FileSizeIssueCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "File Size Issue"
    override val id = "codeVision.codescene.fileSizeIssue"
    override val name = "com.codescene.codeVision.fileSizeIssue"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}