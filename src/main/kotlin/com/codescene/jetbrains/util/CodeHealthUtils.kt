package com.codescene.jetbrains.util

import com.codescene.jetbrains.components.toolWindow.HealthDetails
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

fun round(score: Double, decimals: Int = 2) = BigDecimal(score)
    .setScale(decimals, RoundingMode.HALF_EVEN)
    .toDouble()

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