package com.codescene.jetbrains.flag

import com.codescene.jetbrains.util.Constants

object RuntimeFlags {
    val isDevMode = System.getProperty("cwfIsDevMode")?.toBoolean() ?: false
    val cwfFeature = System.getProperty(Constants.CWF_FLAG)?.toBoolean() ?: false
}