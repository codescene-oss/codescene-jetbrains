package com.codescene.jetbrains.util

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

fun getCodeHealth(healthDetails: HealthDetails): HealthInformation {
    val newScore = healthDetails.newScore ?: "N/A"
    val oldScore = healthDetails.oldScore ?: "N/A"

    val changePercentage = getChangePercentage(healthDetails)
    val sign = codeImproved(healthDetails)

    val percentage = if (changePercentage == null || sign.isEmpty()) "" else "$sign$changePercentage%"

    return if (newScore != oldScore) {
        HealthInformation("$oldScore → $newScore", percentage)
    } else {
        HealthInformation(newScore.toString())
    }
}
