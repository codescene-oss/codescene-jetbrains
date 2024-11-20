package com.codescene.jetbrains.util

import com.codescene.jetbrains.components.toolWindow.HealthDetails
import kotlin.math.abs

fun round(score: Double): Double = kotlin.math.floor(score * 100.0) / 100.0

fun getPercentageChange(healthDetails: HealthDetails) =
    round((healthDetails.newScore / healthDetails.oldScore) * 100)

fun getChangePercentage(healthDetails: HealthDetails): Double {
    val change = 100 - getPercentageChange(healthDetails)

    return abs((round(change)))
}

fun codeImproved(healthDetails: HealthDetails) =
    if (healthDetails.newScore > healthDetails.oldScore) "+"
    else "-"

data class HealthInformation(val change: String, val percentage: String)

fun getCodeHealth(healthDetails: HealthDetails): HealthInformation {
    val newScore = round(healthDetails.newScore)
    val oldScore = round(healthDetails.oldScore)

    val changePercentage = getChangePercentage(healthDetails)
    val sign = codeImproved(healthDetails)

    return HealthInformation("$oldScore → $newScore", "($sign${changePercentage}%)")
}