package com.codescene.jetbrains.core.contracts

interface IOpenFilesObserver {
    fun getAllVisibleFileNames(): Set<String>
}
