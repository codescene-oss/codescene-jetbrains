package com.codescene.jetbrains.platform.clipboard

import com.codescene.jetbrains.core.contracts.IClipboardService
import com.intellij.openapi.components.Service
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

@Service
class IntelliJClipboardService : IClipboardService {
    override fun copyToClipboard(text: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }
}
