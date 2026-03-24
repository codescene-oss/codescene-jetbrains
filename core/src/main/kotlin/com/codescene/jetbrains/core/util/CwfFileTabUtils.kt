package com.codescene.jetbrains.core.util

fun shouldCloseCwfDocTab(
    isDistinctOpenFile: Boolean,
    existingNameWithoutExtension: String,
    cwfDocBaseNames: Set<String>,
): Boolean = isDistinctOpenFile && cwfDocBaseNames.contains(existingNameWithoutExtension)
