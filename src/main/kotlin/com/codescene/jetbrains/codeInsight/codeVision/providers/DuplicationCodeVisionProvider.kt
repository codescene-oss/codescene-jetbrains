package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.CODE_DUPLICATION

class DuplicationCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = CODE_DUPLICATION

}