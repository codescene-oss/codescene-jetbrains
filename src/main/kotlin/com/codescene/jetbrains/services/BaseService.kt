package com.codescene.jetbrains.services

import com.codescene.jetbrains.util.Log

open class BaseService() {

    private val serviceImplementation = this::class.java.simpleName

    /**
     * Executes the given action using the plugin's ClassLoader to avoid class-loading issues.
     * This is necessary when calling CodeScene dependencies to resolve conflicts such as:
     * - ANTLR version mismatches causing ClassCastException (e.g., ANTLRInputStream vs CharStream).
     * - Clojure dependencies failing due to incompatible URLConnection handling
     *   (e.g., ZipResourceFile$MyURLConnection vs JarURLConnection).
     */
    protected fun <T> runWithClassLoaderChange(action: () -> T): T {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        val classLoader = this@BaseService.javaClass.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        return try {
            Log.debug("[$serviceImplementation] Switching to plugin's ClassLoader: ${classLoader.javaClass.name}")

            val startTime = System.currentTimeMillis()

            val result = action()

            val elapsedTime = System.currentTimeMillis() - startTime
            Log.info("[$serviceImplementation] Received response from CodeScene API in ${elapsedTime}ms")

            result
        } catch (e: Exception) {
            Log.error("[$serviceImplementation] Exception during ClassLoader change operation: ${e.message}")

            throw (e)
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader

            Log.debug("[$serviceImplementation] Reverted to original ClassLoader: ${originalClassLoader.javaClass.name}")
        }
    }
}