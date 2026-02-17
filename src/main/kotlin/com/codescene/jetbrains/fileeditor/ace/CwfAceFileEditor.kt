package com.codescene.jetbrains.fileeditor.ace

import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.view.AceData
import com.codescene.jetbrains.fileeditor.BaseCwfFileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class CwfAceFileEditor(project: Project, file: VirtualFile, data: CwfAceFileEditorProviderData) :
    BaseCwfFileEditor<AceData?>(
        project,
        file,
        View.ACE,
        data.aceData,
    )
