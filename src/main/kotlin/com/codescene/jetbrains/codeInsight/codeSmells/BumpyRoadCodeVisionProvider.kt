package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class BumpyRoadCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Bumpy Road Ahead"
    override val id = "codeVision.codescene.bumpyRoad"
    override val name = "com.codescene.codeVision.bumpyRoad"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}