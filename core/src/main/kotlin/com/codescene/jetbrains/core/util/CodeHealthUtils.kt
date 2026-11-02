package com.codescene.jetbrains.core.util

import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import kotlin.math.abs

data class HealthDetails(
    val oldScore: Double?,
    val newScore: Double?,
)

fun getChangePercentage(healthDetails: HealthDetails): Double? {
    if (healthDetails.newScore == null || healthDetails.oldScore == null) return null

    if (healthDetails.oldScore == 0.0) {
        return if (healthDetails.newScore == 0.0) 0.0 else 100.0
    }

    val percentageChange = ((healthDetails.newScore - healthDetails.oldScore) / healthDetails.oldScore) * 100

    return kotlin.math.floor(abs(percentageChange) * 100.0) / 100.0
}

private fun codeImproved(healthDetails: HealthDetails) =
    if (healthDetails.newScore == null || healthDetails.oldScore == null) {
        ""
    } else if (healthDetails.newScore > healthDetails.oldScore) {
        "+"
    } else {
        "-"
    }

data class HealthInformation(
    val change: String,
    val percentage: String = "",
)

private const val NO_SCORE = "N/A"

private fun scoreText(score: Double?) = score?.toString() ?: NO_SCORE

fun resolveCodeHealthDescription(
    deltaResult: Delta?,
    review: Review?,
): String? {
    val oldReviewScore = deltaResult?.oldScore?.orElse(null)
    val newReviewScore = deltaResult?.newScore?.orElse(null)
    // Compared as nullable values: an absent score is never mapped to a default before comparing.
    val hasChanged = oldReviewScore != newReviewScore

    return when {
        deltaResult != null && hasChanged -> getCodeHealth(HealthDetails(oldReviewScore, newReviewScore)).change
        review?.score?.isPresent == true -> review.score.get().toString()
        else -> NO_SCORE.takeIf { review != null }
    }
}

fun getCodeHealth(healthDetails: HealthDetails): HealthInformation {
    val changePercentage = getChangePercentage(healthDetails)
    val sign = codeImproved(healthDetails)

    val percentage = if (changePercentage == null || sign.isEmpty()) "" else "$sign$changePercentage%"

    return if (healthDetails.newScore != healthDetails.oldScore) {
        HealthInformation("${scoreText(healthDetails.oldScore)} -> ${scoreText(healthDetails.newScore)}", percentage)
    } else {
        HealthInformation(scoreText(healthDetails.newScore))
    }
}
