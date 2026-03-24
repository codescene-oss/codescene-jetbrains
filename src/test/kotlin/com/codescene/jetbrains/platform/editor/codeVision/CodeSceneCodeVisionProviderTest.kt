package com.codescene.jetbrains.platform.editor.codeVision

import com.codescene.jetbrains.platform.editor.codeVision.providers.AceCodeVisionProvider
import com.codescene.jetbrains.platform.editor.codeVision.providers.AggregatedSmellCodeVisionProvider
import com.codescene.jetbrains.platform.editor.codeVision.providers.CodeHealthCodeVisionProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeSceneCodeVisionProviderTest {
    @Test
    fun `getProviders returns expected provider names`() {
        val providers = CodeSceneCodeVisionProvider.getProviders()

        assertEquals(
            listOf(
                AggregatedSmellCodeVisionProvider::class.simpleName!!,
                CodeHealthCodeVisionProvider::class.simpleName!!,
                AceCodeVisionProvider::class.simpleName!!,
            ),
            providers,
        )
    }
}
