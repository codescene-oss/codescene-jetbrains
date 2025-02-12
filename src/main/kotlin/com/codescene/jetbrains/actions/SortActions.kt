package com.codescene.jetbrains.actions

import com.codescene.jetbrains.CodeSceneIcons
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.config.global.MonitorTreeSortOptions
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction

class SortByScoreChangeAscending : ToggleAction(
    "Score Change, Ascending", "Largest Code Health decline first",
    CodeSceneIcons.CODE_HEALTH_LOW
) {
    override fun isSelected(p0: AnActionEvent) =
        CodeSceneGlobalSettingsStore.getInstance().state.monitorTreeSortOption == MonitorTreeSortOptions.SCORE_ASCENDING

    override fun setSelected(p0: AnActionEvent, p1: Boolean) {
        CodeSceneGlobalSettingsStore.getInstance().state.monitorTreeSortOption = MonitorTreeSortOptions.SCORE_ASCENDING
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

class SortByScoreChangeDescending : ToggleAction(
    "Score Change, Descending",
    "Largest Code Health increase first",
    CodeSceneIcons.CODE_HEALTH_HIGH
) {
    override fun isSelected(p0: AnActionEvent) =
        CodeSceneGlobalSettingsStore.getInstance().state.monitorTreeSortOption == MonitorTreeSortOptions.SCORE_DESCENDING

    override fun setSelected(p0: AnActionEvent, p1: Boolean) {
        CodeSceneGlobalSettingsStore.getInstance().state.monitorTreeSortOption = MonitorTreeSortOptions.SCORE_DESCENDING
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

class SortByFileName : ToggleAction(
    "File Name", "Using absolute path",
    AllIcons.ObjectBrowser.Sorted
) {
    override fun isSelected(p0: AnActionEvent) =
        CodeSceneGlobalSettingsStore.getInstance().state.monitorTreeSortOption == MonitorTreeSortOptions.FILE_NAME

    override fun setSelected(p0: AnActionEvent, p1: Boolean) {
        CodeSceneGlobalSettingsStore.getInstance().state.monitorTreeSortOption = MonitorTreeSortOptions.FILE_NAME
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}