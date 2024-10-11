package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class ModularityIssueCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Modularity Issue"
    override val id = "codeVision.codescene.modularityIssue"
    override val name = "com.codescene.codeVision.modularityIssue"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}