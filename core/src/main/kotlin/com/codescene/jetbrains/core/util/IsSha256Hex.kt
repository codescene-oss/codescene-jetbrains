package com.codescene.jetbrains.core.util

fun isSha256Hex(candidate: String) = candidate.matches(Regex("^[a-fA-F0-9]{64}$"))
