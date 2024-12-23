package com.codescene.jetbrains.util

import com.codescene.jetbrains.util.Constants.GREEN
import com.codescene.jetbrains.util.Constants.ORANGE
import com.codescene.jetbrains.util.Constants.RED
import com.intellij.ui.JBColor

fun resolveHealthBadge(score: Double): Pair<String, JBColor> =
    if (score < 4.0) "Unhealthy" to RED
    else if (score >= 4.0 && score < 9.0) "Problematic" to ORANGE
    else "Healthy" to GREEN