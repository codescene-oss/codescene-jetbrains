package com.codescene.jetbrains.components.webview.mapper

import com.codescene.jetbrains.components.webview.data.view.AceData
import com.codescene.jetbrains.components.webview.data.CwfData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

@Service
class AceMapper {
    companion object {
        fun getInstance(): AceMapper = ApplicationManager.getApplication().getService(AceMapper::class.java)
    }

    fun toCwfData(
        ace: Any,
        pro: Boolean = true,
        devmode: Boolean = true
    ): CwfData<AceData>? {

        return null;
    }
}