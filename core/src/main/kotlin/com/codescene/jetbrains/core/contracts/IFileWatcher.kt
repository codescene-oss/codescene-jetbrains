package com.codescene.jetbrains.core.contracts

typealias FileChangeCallback = (changedPaths: List<String>) -> Unit

interface IFileWatcher {
    fun addListener(callback: FileChangeCallback)

    fun removeListener(callback: FileChangeCallback)
}
