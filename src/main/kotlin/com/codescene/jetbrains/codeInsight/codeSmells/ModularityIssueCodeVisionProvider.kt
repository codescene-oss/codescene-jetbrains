package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class ModularityIssueCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Modularity Issue"
    override val id = "codeVision.codescene.modularityIssue"
    override val name = "com.codescene.codeVision.modularityIssue"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}