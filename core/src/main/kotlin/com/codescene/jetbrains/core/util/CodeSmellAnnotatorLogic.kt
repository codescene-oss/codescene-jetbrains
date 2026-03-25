package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.review.Review

fun shouldAnnotateCodeSmells(
    review: Review?,
    aceCache: List<FnToRefactor>,
): Boolean = review != null || aceCache.isNotEmpty()
