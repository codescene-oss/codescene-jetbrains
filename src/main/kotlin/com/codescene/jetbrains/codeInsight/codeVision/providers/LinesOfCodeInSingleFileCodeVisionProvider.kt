package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.LINES_OF_CODE_IN_A_SINGLE_FILE

class LinesOfCodeInSingleFileCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = LINES_OF_CODE_IN_A_SINGLE_FILE
}