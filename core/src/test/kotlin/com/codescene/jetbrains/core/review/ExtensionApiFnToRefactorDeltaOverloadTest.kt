package com.codescene.jetbrains.core.review

import com.codescene.ExtensionAPI
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionApiFnToRefactorDeltaOverloadTest {
    @Test
    fun `fnToRefactor has overload taking Delta as third parameter`() {
        val hasDeltaOverload =
            ExtensionAPI::class.java.methods.any { method ->
                method.name == "fnToRefactor" &&
                    method.parameterTypes.size == 3 &&
                    method.parameterTypes[2] == com.codescene.data.delta.Delta::class.java
            }
        assertTrue(hasDeltaOverload)
    }
}
