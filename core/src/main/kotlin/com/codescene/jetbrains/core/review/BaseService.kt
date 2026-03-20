package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.util.withPluginClassLoader

data class TimedResult<T>(val result: T, val elapsedMs: Long)

open class BaseService(
    protected val log: ILogger,
) {
    private val serviceImplementation = this::class.java.simpleName

    /**
     * Executes the given action using the plugin's ClassLoader to avoid class-loading issues.
     * This is necessary when calling CodeScene dependencies to resolve conflicts such as:
     * - ANTLR version mismatches causing ClassCastException (e.g., ANTLRInputStream vs CharStream).
     * - Clojure dependencies failing due to incompatible URLConnection handling
     *   (e.g., ZipResourceFile$MyURLConnection vs JarURLConnection).
     */
    protected fun <T> runWithClassLoaderChange(action: () -> T): TimedResult<T> {
        val classLoader = this@BaseService.javaClass.classLoader

        return try {
            log.debug("Switching to plugin's ClassLoader: ${classLoader.javaClass.name}", serviceImplementation)

            val startTime = System.currentTimeMillis()

            val result = withPluginClassLoader(classLoader, action)

            val elapsedTime = System.currentTimeMillis() - startTime

            log.info("Received response from CodeScene API in ${elapsedTime}ms", serviceImplementation)
            TimedResult(result, elapsedTime)
        } catch (e: Exception) {
            log.debug("Exception during CodeScene API operation. Error message: ${e.message}", serviceImplementation)
            throw (e)
        } finally {
            log.debug("Reverted to original ClassLoader", serviceImplementation)
        }
    }
}
