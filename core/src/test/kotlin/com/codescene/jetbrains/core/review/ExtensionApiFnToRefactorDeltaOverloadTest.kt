package com.codescene.jetbrains.core.review

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.CacheParams
import com.codescene.ExtensionAPI.CodeParams
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionApiFnToRefactorDeltaOverloadTest {
    @Test
    fun `fnToRefactor has overload taking Delta as third parameter`() {
        val hasExactDeltaOverload =
            ExtensionAPI::class.java.methods.any { method ->
                method.name == "fnToRefactor" &&
                    method.parameterTypes.size == 3 &&
                    method.parameterTypes[0] == CodeParams::class.java &&
                    method.parameterTypes[1] == CacheParams::class.java &&
                    method.parameterTypes[2] == com.codescene.data.delta.Delta::class.java
            }
        assertTrue(hasExactDeltaOverload)
    }
}
