package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.GLOBAL_CONDITIONALS
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class GlobalConditionalsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = GLOBAL_CONDITIONALS

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}