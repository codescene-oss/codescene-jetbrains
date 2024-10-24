package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class ModularityIssueCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Modularity Issue"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}