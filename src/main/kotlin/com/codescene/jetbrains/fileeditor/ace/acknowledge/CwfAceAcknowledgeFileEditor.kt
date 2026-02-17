package com.codescene.jetbrains.fileeditor.ace.acknowledge

import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.view.AceAcknowledgeData
import com.codescene.jetbrains.fileeditor.BaseCwfFileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class CwfAceAcknowledgeFileEditor(
    project: Project,
    file: VirtualFile,
    data: CwfAceAcknowledgeEditorProviderData,
) :
    BaseCwfFileEditor<AceAcknowledgeData>(
            project,
            file,
            View.ACE_ACKNOWLEDGE,
            data.aceAcknowledgeData,
        )
