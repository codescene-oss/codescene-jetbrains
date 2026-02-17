package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.LINES_OF_DECLARATION_IN_A_SINGLE_FILE

class LinesOfDeclarationInSingleFileCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = LINES_OF_DECLARATION_IN_A_SINGLE_FILE
}
