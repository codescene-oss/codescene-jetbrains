package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.PRIMITIVE_OBSESSION

class PrimitiveObsessionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = PRIMITIVE_OBSESSION
}
