package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.PRIMITIVE_OBSESSION
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class PrimitiveObsessionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = PRIMITIVE_OBSESSION

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}