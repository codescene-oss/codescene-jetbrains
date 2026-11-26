package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.flag.AceAvailability

fun shouldAnnotateCodeSmells(
    review: Review?,
    aceCache: List<FnToRefactor>,
): Boolean = review != null || aceCache.isNotEmpty()

fun isAceRefactorQuickFixAvailable(
    aceTokenConfigured: Boolean,
    aceEnabled: Boolean = AceAvailability.ENABLED,
): Boolean = aceEnabled && aceTokenConfigured
