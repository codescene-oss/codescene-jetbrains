package com.codescene.jetbrains.actions

import com.codescene.jetbrains.CodeSceneIcons
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.config.global.MonitorTreeSortOptions
import com.codescene.jetbrains.flag.RuntimeFlags
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import javax.swing.Icon

abstract class SortByMonitorOption(
    label: String,
    description: String,
    icon: Icon,
    private val sortOption: MonitorTreeSortOptions
) : ToggleAction(label, description, icon) {
    override fun isSelected(event: AnActionEvent) =
        CodeSceneGlobalSettingsStore.getInstance().state.monitorTreeSortOption == sortOption

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        CodeSceneGlobalSettingsStore.getInstance().state.monitorTreeSortOption = sortOption
        event.project?.messageBus?.syncPublisher(ToolWindowRefreshNotifier.TOPIC)?.refresh(null)
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        val presentation = event.presentation
        presentation.isEnabledAndVisible = CodeSceneGlobalSettingsStore.getInstance().state.codeHealthMonitorEnabled && !RuntimeFlags.cwfFeature
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

class CodeHealthMonitorSortGroupActions {
    class SortByScoreChangeAscending : SortByMonitorOption(
        UiLabelsBundle.message("scoreChangeAscending"),
        UiLabelsBundle.message("scoreChangeAscendingInfo"),
        CodeSceneIcons.CODE_HEALTH_LOW,
        MonitorTreeSortOptions.SCORE_ASCENDING
    )

    class SortByScoreChangeDescending : SortByMonitorOption(
        UiLabelsBundle.message("scoreChangeDescending"),
        UiLabelsBundle.message("scoreChangeDescendingInfo"),
        CodeSceneIcons.CODE_HEALTH_HIGH,
        MonitorTreeSortOptions.SCORE_DESCENDING
    )

    class SortByFileName : SortByMonitorOption(
        UiLabelsBundle.message("fileName"),
        UiLabelsBundle.message("fileNameInfo"),
        AllIcons.ObjectBrowser.Sorted,
        MonitorTreeSortOptions.FILE_NAME
    )
}
