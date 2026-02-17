package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.LOW_COHESION

class LowCohesionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = LOW_COHESION
}
