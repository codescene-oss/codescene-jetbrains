package com.codescene.jetbrains.platform.fileeditor.ace.acknowledge

import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.view.AceAcknowledgeData
import com.codescene.jetbrains.platform.fileeditor.BaseCwfFileEditor
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
