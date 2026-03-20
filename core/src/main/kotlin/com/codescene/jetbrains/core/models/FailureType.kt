package com.codescene.jetbrains.core.models

enum class FailureType(
    val value: String,
) {
    CANCELLED("Cancelled"),
    FAILED("Failed"),
    TIMED_OUT("Timed out"),
}
