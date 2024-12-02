package com.codescene.jetbrains.util

import com.codescene.jetbrains.components.tree.HealthDetails
import kotlin.math.abs

fun round(score: Double): Double = kotlin.math.floor(score * 100.0) / 100.0

fun getChangePercentage(healthDetails: HealthDetails): Double {
    if (healthDetails.oldScore == 0.0) {
        return if (healthDetails.newScore == 0.0) 0.0 else 100.0
    }

    val percentageChange = ((healthDetails.newScore - healthDetails.oldScore) / healthDetails.oldScore) * 100

    return round(abs(percentageChange))
}

private fun codeImproved(healthDetails: HealthDetails) =
    if (healthDetails.newScore > healthDetails.oldScore) "+"
    else "-"

data class HealthInformation(val change: String, val percentage: String = "")

fun getCodeHealth(healthDetails: HealthDetails): HealthInformation {
    val newScore = round(healthDetails.newScore)
    val oldScore = round(healthDetails.oldScore)

    val changePercentage = getChangePercentage(healthDetails)
    val sign = codeImproved(healthDetails)

    return if (newScore != oldScore) HealthInformation(
        "$oldScore → $newScore",
        "($sign${changePercentage}%)"
    ) else HealthInformation(newScore.toString())
}