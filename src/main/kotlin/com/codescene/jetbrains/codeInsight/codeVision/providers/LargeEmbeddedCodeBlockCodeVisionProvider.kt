package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.LARGE_EMBEDDED_CODE_BLOCK

class LargeEmbeddedCodeBlockCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = LARGE_EMBEDDED_CODE_BLOCK
}