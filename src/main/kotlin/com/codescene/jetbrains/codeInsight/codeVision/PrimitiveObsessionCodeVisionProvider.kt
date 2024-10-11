package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class PrimitiveObsessionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Primitive Obsession"
    override val id = "codeVision.codescene.primitiveObsession"
    override val name = "com.codescene.codeVision.primitiveObsession"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}