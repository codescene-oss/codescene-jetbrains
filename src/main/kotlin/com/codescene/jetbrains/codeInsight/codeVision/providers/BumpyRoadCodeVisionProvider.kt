package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.BUMPY_ROAD_AHEAD

class BumpyRoadCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = BUMPY_ROAD_AHEAD
}
