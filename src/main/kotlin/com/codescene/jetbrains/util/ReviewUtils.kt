package com.codescene.jetbrains.util

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.services.UIRefreshService
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.ProjectManager
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Creates a delegate for the "Enable Review Code Lenses" setting.
 *
 * This observable property is initialized to `true`. Whenever its value changes,
 * it iterates through all open projects and all open editors in those projects,
 * and triggers a Code Vision UI refresh for each editor.
 */
fun enableCodeLensesDelegate(): ReadWriteProperty<Any?, Boolean> =
    Delegates.observable(true) { _, _, _ ->
        ProjectManager.getInstance().openProjects.forEach { project ->
            val editors = EditorFactory.getInstance().allEditors.filter { it.project == project }.toList()

            CoroutineScope(Dispatchers.Main).launch {
                editors.forEach {
                    UIRefreshService.getInstance(project)
                        .refreshCodeVision(it, CodeSceneCodeVisionProvider.getProviders())
                }
            }
        }
    }