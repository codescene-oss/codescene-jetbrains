package com.codescene.jetbrains.core

import com.codescene.jetbrains.core.contracts.ILogger

object TestLogger : ILogger {
    override fun info(
        message: String,
        service: String?,
    ) {}

    override fun warn(
        message: String,
        service: String?,
    ) {}

    override fun debug(
        message: String,
        service: String?,
    ) {}

    override fun error(
        message: String,
        service: String?,
    ) {}
}
