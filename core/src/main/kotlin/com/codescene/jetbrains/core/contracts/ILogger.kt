package com.codescene.jetbrains.core.contracts

interface ILogger {
    fun info(
        message: String,
        service: String? = "",
    )

    fun warn(
        message: String,
        service: String? = "",
    )

    fun debug(
        message: String,
        service: String? = "",
    )

    fun error(
        message: String,
        service: String? = "",
    )
}
