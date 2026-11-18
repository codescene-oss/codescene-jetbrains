package com.codescene.jetbrains.core.util

import com.codescene.data.delta.Delta
import com.codescene.data.review.Review

private const val NO_APPLICATION_CODE = "No application code detected for scoring"
private const val NO_SCORE = "n/a"

private fun scoreNumber(score: Double): String = if (score % 1.0 == 0.0) score.toLong().toString() else score.toString()

private fun isScored(score: Double?): Boolean = score != null && score != 0.0

private fun formatScore(score: Double?): String =
    if (isScored(score)) "${scoreNumber(score!!)}/10" else NO_APPLICATION_CODE

private fun scoreOrNoScore(score: Double?): String = if (isScored(score)) scoreNumber(score!!) else NO_SCORE

private fun deltaDescription(delta: Delta): String {
    val oldScore = delta.oldScore?.orElse(null)
    val newScore = delta.newScore?.orElse(null)
    // Compared as nullable values: an absent score is never mapped to a default before comparing.
    return when {
        oldScore == null && newScore != null -> formatScore(newScore)
        oldScore == newScore -> formatScore(oldScore)
        else -> "${scoreOrNoScore(oldScore)} → ${scoreOrNoScore(newScore)}"
    }
}

fun resolveCodeHealthDescription(
    deltaResult: Delta?,
    review: Review?,
): String? =
    when {
        deltaResult != null -> deltaDescription(deltaResult)
        review != null -> formatScore(review.score?.orElse(null))
        else -> null
    }
