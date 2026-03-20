package com.codescene.jetbrains.platform.fileeditor.documentation

import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.view.DocsData
import com.codescene.jetbrains.platform.fileeditor.BaseCwfFileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal class CwfDocsFileEditor(
    project: Project,
    file: VirtualFile,
    data: DocsData,
) : BaseCwfFileEditor<DocsData>(
        project,
        file,
        View.DOCS,
        data,
    )
