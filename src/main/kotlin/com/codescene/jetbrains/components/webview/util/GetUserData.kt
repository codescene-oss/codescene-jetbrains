package com.codescene.jetbrains.components.webview.util

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.fileeditor.ace.CWF_ACE_DATA_KEY
import com.codescene.jetbrains.fileeditor.ace.CwfAceFileEditorProviderData
import com.codescene.jetbrains.fileeditor.ace.acknowledge.CWF_ACE_ACKNOWLEDGE_DATA_KEY
import com.codescene.jetbrains.fileeditor.ace.acknowledge.CwfAceAcknowledgeEditorProviderData
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

/**
 * Retrieves user data stored in a custom editor tab of a given type.
 *
 * @param T Type of the user data.
 * @param project The IntelliJ project.
 * @param tabTitle The display name of the tab to look for.
 * @param key The Key associated with the user data.
 * @return The user data if a matching open file is found, null otherwise.
 */
private fun <T> getUserDataForTab(
    project: Project,
    tabTitle: String,
    key: Key<T>
): T? = FileEditorManager.getInstance(project)
    .openFiles
    .find { it.name == tabTitle }
    ?.getUserData(key)

/**
 * Retrieves the ACE user data stored in the custom ACE editor tab.
 *
 * Looks for an open file whose name matches the ACE tab title and
 * returns the associated [CWF_ACE_DATA_KEY] user data if present.
 */
fun getAceUserData(project: Project): CwfAceFileEditorProviderData? =
    getUserDataForTab(project, UiLabelsBundle.message("ace"), CWF_ACE_DATA_KEY)

/**
 * Retrieves the ACE user data stored in the custom ACE acknowledge editor tab.
 *
 * Looks for an open file whose name matches the ACE acknowledge tab title and
 * returns the associated [CWF_ACE_ACKNOWLEDGE_DATA_KEY] user data if present.
 */
fun getAceAcknowledgeUserData(project: Project): CwfAceAcknowledgeEditorProviderData? =
    getUserDataForTab(project, UiLabelsBundle.message("aceAcknowledge"), CWF_ACE_ACKNOWLEDGE_DATA_KEY)