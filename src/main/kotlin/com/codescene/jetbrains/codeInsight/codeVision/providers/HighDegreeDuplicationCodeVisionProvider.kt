package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.HIGH_DEGREE_OF_CODE_DUPLICATION

class HighDegreeDuplicationCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = HIGH_DEGREE_OF_CODE_DUPLICATION
}