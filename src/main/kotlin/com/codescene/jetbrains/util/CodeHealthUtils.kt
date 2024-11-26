package com.codescene.jetbrains.util

import com.codescene.jetbrains.components.tree.HealthDetails
import kotlin.math.abs

fun round(score: Double): Double = kotlin.math.floor(score * 100.0) / 100.0

fun getChangePercentage(healthDetails: HealthDetails): Double {
    val percentage = (healthDetails.newScore / healthDetails.oldScore) * 100
    val newScore = round(percentage)

    return round(abs(100 - newScore))
}

private fun codeImproved(healthDetails: HealthDetails) =
    if (healthDetails.newScore > healthDetails.oldScore) "+"
    else "-"

data class HealthInformation(val change: String, val percentage: String)

fun getCodeHealth(healthDetails: HealthDetails): HealthInformation {
    val newScore = round(healthDetails.newScore)
    val oldScore = round(healthDetails.oldScore)

    val changePercentage = getChangePercentage(healthDetails)
    val sign = codeImproved(healthDetails)

    return if (newScore != oldScore) HealthInformation(
        "$oldScore → $newScore",
        "($sign${changePercentage}%)"
    ) else HealthInformation(newScore.toString(), "")
}