package com.codescene.jetbrains.listeners

import codescene.devtools.ide.api
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.wm.IdeFrame
import java.io.InputStream
import java.nio.charset.StandardCharsets

class MyApplicationActivationListener : ApplicationActivationListener {

    override fun applicationActivated(ideFrame: IdeFrame) {
        thisLogger().warn("RUNNING THE API")

        println("Class path ... ${System.getProperty("java.class.path")}")

        try {
            val path = "src/main/resources/example2.js"
            val inputStream: InputStream? = MyApplicationActivationListener::class.java.classLoader.getResourceAsStream("example2.js")

            thisLogger().warn("inputStream")

            inputStream?.let {
                val content = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                thisLogger().warn("content")

                val data = api.review(path, content)

                println("Data: $data")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
