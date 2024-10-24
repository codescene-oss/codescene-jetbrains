package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.BUMPY_ROAD_AHEAD
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class BumpyRoadCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = BUMPY_ROAD_AHEAD

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}