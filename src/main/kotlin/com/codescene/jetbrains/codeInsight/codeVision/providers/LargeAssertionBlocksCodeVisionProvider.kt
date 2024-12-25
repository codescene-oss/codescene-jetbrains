package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.LARGE_ASSERTION_BLOCKS

class LargeAssertionBlocksCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = LARGE_ASSERTION_BLOCKS
}