package com.codescene.jetbrains.platform.fileeditor.ace

import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.view.AceData
import com.codescene.jetbrains.platform.fileeditor.BaseCwfFileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class CwfAceFileEditor(project: Project, file: VirtualFile, data: CwfAceFileEditorProviderData) :
    BaseCwfFileEditor<AceData?>(
        project,
        file,
        View.ACE,
        data.aceData,
    )
