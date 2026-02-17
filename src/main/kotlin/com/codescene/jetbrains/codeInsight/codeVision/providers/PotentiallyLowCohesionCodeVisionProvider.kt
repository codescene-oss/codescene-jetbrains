package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.POTENTIALLY_LOW_COHESION

class PotentiallyLowCohesionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = POTENTIALLY_LOW_COHESION
}
