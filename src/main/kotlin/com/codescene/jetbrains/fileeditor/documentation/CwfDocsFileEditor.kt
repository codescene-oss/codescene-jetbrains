package com.codescene.jetbrains.fileeditor.documentation

import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.fileeditor.BaseCwfFileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal class CwfDocsFileEditor(
    project: Project,
    file: VirtualFile,
    data: DocsData
) : BaseCwfFileEditor<DocsData>(
    project,
    file,
    View.DOCS,
    data
)