package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.BRAIN_METHOD

class BrainMethodCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = BRAIN_METHOD
}
