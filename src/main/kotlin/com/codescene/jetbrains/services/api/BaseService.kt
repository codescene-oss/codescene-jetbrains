package com.codescene.jetbrains.services.api

import com.codescene.jetbrains.util.Log

data class TimedResult<T>(val result: T, val elapsedMs: Long)

open class BaseService() {
    private val serviceImplementation = this::class.java.simpleName

    /**
     * Executes the given action using the plugin's ClassLoader to avoid class-loading issues.
     * This is necessary when calling CodeScene dependencies to resolve conflicts such as:
     * - ANTLR version mismatches causing ClassCastException (e.g., ANTLRInputStream vs CharStream).
     * - Clojure dependencies failing due to incompatible URLConnection handling
     *   (e.g., ZipResourceFile$MyURLConnection vs JarURLConnection).
     */
    protected fun <T> runWithClassLoaderChange(action: () -> T): TimedResult<T> {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        val classLoader = this@BaseService.javaClass.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        return try {
            Log.debug("Switching to plugin's ClassLoader: ${classLoader.javaClass.name}", serviceImplementation)

            val startTime = System.currentTimeMillis()

            val result = action()

            val elapsedTime = System.currentTimeMillis() - startTime

            Log.info("Received response from CodeScene API in ${elapsedTime}ms", serviceImplementation)
            TimedResult(result, elapsedTime)
        } catch (e: Exception) {
            Log.debug("Exception during CodeScene API operation. Error message: ${e.message}", serviceImplementation)
            throw (e)
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader

            Log.debug("Reverted to original ClassLoader: ${originalClassLoader.javaClass.name}", serviceImplementation)
        }
    }
}
