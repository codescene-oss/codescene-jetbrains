package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.FILE_SIZE_ISSUE
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class FileSizeIssueCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = FILE_SIZE_ISSUE

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}