package com.codescene.jetbrains.components.webview.util

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.fileeditor.ace.CWF_ACE_DATA_KEY
import com.codescene.jetbrains.fileeditor.ace.CwfAceFileEditorProviderData
import com.codescene.jetbrains.fileeditor.ace.acknowledge.CWF_ACE_ACKNOWLEDGE_DATA_KEY
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

/**
 * Retrieves the ACE user data stored in the custom ACE editor tab.
 *
 * Looks for an open file whose name matches the ACE tab title and
 * returns the associated [CWF_ACE_DATA_KEY] user data if present.
 */
fun getAceUserData(project: Project): CwfAceFileEditorProviderData? = FileEditorManager.getInstance(project)
    .openFiles
    .find { it.name == UiLabelsBundle.message("ace") }
    ?.getUserData(CWF_ACE_DATA_KEY)

/**
 * Retrieves the ACE user data stored in the custom ACE acknowledge editor tab.
 *
 * Looks for an open file whose name matches the ACE acknowledge tab title and
 * returns the associated [CWF_ACE_ACKNOWLEDGE_DATA_KEY] user data if present.
 */
fun getAceAcknowledgeUserData(project: Project) = FileEditorManager.getInstance(project)
    .openFiles
    .find { it.name == UiLabelsBundle.message("aceAcknowledge") }
    ?.getUserData(CWF_ACE_ACKNOWLEDGE_DATA_KEY)