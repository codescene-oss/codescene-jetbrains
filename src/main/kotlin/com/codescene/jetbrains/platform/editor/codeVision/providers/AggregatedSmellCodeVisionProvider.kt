package com.codescene.jetbrains.platform.editor.codeVision.providers

import com.codescene.jetbrains.core.util.CodeVisionSmellCategories
import com.codescene.jetbrains.platform.editor.codeVision.CodeSceneCodeVisionProvider

class AggregatedSmellCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override fun categoriesForLenses(): List<String> = CodeVisionSmellCategories.orderedForDisplay
}
