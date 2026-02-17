package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.LARGE_METHOD

class LargeMethodCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = LARGE_METHOD
}
