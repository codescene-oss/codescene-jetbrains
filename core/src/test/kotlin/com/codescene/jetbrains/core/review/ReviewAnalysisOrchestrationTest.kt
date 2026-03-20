package com.codescene.jetbrains.core.review

import com.codescene.data.review.Review
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.models.TelemetryInfo
import com.codescene.jetbrains.core.testdoubles.InMemoryReviewCacheService
import com.codescene.jetbrains.core.testdoubles.RecordingTelemetryService
import com.codescene.jetbrains.core.util.TelemetryEvents
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ReviewAnalysisOrchestrationTest {
    @Test
    fun `completeReviewAnalysis logs performance and stores review in cache`() {
        val telemetry = RecordingTelemetryService()
        val cache = InMemoryReviewCacheService()
        val review = mockk<Review>(relaxed = true)

        completeReviewAnalysis(
            path = "a.kt",
            fileName = "a.kt",
            code = "content",
            result = review,
            elapsedMs = 200,
            telemetryInfo = TelemetryInfo(loc = 15, language = "kt"),
            telemetryService = telemetry,
            reviewCacheService = cache,
            logger = TestLogger,
            serviceName = "svc",
        )

        val event = telemetry.events.single()
        assertEquals(TelemetryEvents.ANALYSIS_PERFORMANCE, event.name)
        assertEquals("review", event.data["type"])
        assertEquals(200L, event.data["elapsedMs"])
        assertEquals(15, event.data["loc"])
        assertEquals("kt", event.data["language"])
        assertSame(review, cache.get(ReviewCacheQuery(fileContents = "content", filePath = "a.kt")))
    }
}
