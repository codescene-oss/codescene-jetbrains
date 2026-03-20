package com.codescene.jetbrains.core.util

fun <T> withPluginClassLoader(
    classLoader: ClassLoader,
    action: () -> T,
): T {
    val original = Thread.currentThread().contextClassLoader
    Thread.currentThread().contextClassLoader = classLoader
    try {
        return action()
    } finally {
        Thread.currentThread().contextClassLoader = original
    }
}
