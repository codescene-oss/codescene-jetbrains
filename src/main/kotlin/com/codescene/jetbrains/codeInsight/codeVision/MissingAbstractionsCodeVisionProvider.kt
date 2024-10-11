package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class MissingAbstractionsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Missing Arguments Abstractions"
    override val id = "codeVision.codescene.missingArgumentsAbstractions"
    override val name = "com.codescene.codeVision.missingArgumentsAbstractions"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}