package com.codescene.jetbrains.flag

import com.codescene.jetbrains.util.Constants
import java.util.Properties

object RuntimeFlags {
    private val props: Properties by lazy {
        Properties().apply {
            val stream = this@RuntimeFlags.javaClass.classLoader.getResourceAsStream("feature-flags.properties")
            stream?.let { load(stream) }
        }
    }

    val isDevMode
        get() =
            System.getProperty(Constants.CWF_DEVMODE_FLAG)?.toBoolean()
                ?: props.getProperty("feature.cwf.devMode")?.toBoolean()
                ?: false

    val aceFeature
        get() =
            System.getProperty(Constants.ACE_FLAG)?.toBoolean()
                ?: props.getProperty("feature.ace")?.toBoolean()
                ?: false
}
