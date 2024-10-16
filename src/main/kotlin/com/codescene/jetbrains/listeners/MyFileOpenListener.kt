package com.codescene.jetbrains.listeners

import clojure.java.api.Clojure
import clojure.lang.IFn
import codescene.devtools.ide.DevToolsAPI
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

class MyFileOpenListener : FileEditorManagerListener {
    private val project: Project? = ProjectManager.getInstance().openProjects.firstOrNull()

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        printClojureVersion()
        printClassLoaders()
        readFileContents(file)
    }

    fun printClojureVersion() {
        val clojureVersion: IFn = Clojure.`var`("clojure.core", "clojure-version")
        val version = clojureVersion.invoke() as String
        println("Clojure version: $version")
    }

    fun printClassLoaders() {
        val classLoader = this::class.java.classLoader
        println("Current class classloader: $classLoader")
        val antlrClassLoader = org.antlr.v4.runtime.ANTLRInputStream::class.java.classLoader
        println("Antlr classloader: $antlrClassLoader")
        val threadClassLoader = Thread.currentThread().contextClassLoader
        println("Current thread classloader: $threadClassLoader")
    }

    private fun readFileContents(file: VirtualFile) {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        val classLoader = this.javaClass.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        try {
            if (file.isValid && project != null) {
                val bytes = file.contentsToByteArray()
                val code = bytes.toString(Charsets.UTF_8)
                val relativePath = VfsUtil.getRelativePath(file, project.baseDir)
                println("Callind CodeScene API from classloader: $classLoader")
                try {
                    val data = DevToolsAPI.review(relativePath, code)
                    println("Got response from CodeScene API: $data")
                } catch (e: Exception) {
                    println(e.message)
                }
            }
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }
}