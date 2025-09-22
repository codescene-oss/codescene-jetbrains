package com.codescene.jetbrains.components.webview.util

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.fileeditor.CWF_ACE_DATA_KEY
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

/**
 * Retrieves the ACE user data stored in the custom ACE editor tab.
 *
 * Looks for an open file whose name matches the ACE tab title and
 * returns the associated [CWF_ACE_DATA_KEY] user data if present.
 */
fun getAceUserData(project: Project) = FileEditorManager.getInstance(project)
    .openFiles
    .find { it.name == UiLabelsBundle.message("ace") }
    ?.getUserData(CWF_ACE_DATA_KEY)