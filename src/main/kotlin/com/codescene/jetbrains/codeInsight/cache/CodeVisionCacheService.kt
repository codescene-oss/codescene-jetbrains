package com.codescene.jetbrains.codeInsight.cache

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolderEx
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
internal class CodeVisionCacheService {
    companion object {
        private val key = Key<ConcurrentHashMap<String, CodeVisionWithStamp>>("code.vision.cache")
    }

    private fun getOrCreateCache(editor: Editor) = (editor as UserDataHolderEx).putUserDataIfAbsent(key, ConcurrentHashMap())

    fun getVisionDataForEditor(editor: Editor, providerId: String): CodeVisionWithStamp? {
        val cache = getOrCreateCache(editor)

        return cache[providerId]
    }

    fun storeVisionDataForEditor(editor: Editor, providerId: String, data: CodeVisionWithStamp) {
        val fileCache = getOrCreateCache(editor)

        fileCache[providerId] = data
    }

    data class CodeVisionWithStamp(val codeVisionEntries: List<Pair<TextRange, CodeVisionEntry>>, val modificationStamp: Long)
}