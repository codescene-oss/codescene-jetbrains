package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.CONSTRUCTOR_OVER_INJECTION
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class ConstructorOverInjectionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = CONSTRUCTOR_OVER_INJECTION

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}