package com.codescene.jetbrains.core.cli

fun toPosixRelPath(path: String): String = path.replace('\\', '/')
