package com.codescene.jetbrains.components.settings.tab

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*

@Suppress("DialogTitleCapitalization")
class SettingsTab : BoundConfigurable(UiLabelsBundle.message("settingsTitle")) {
    private val settings = CodeSceneGlobalSettingsStore.getInstance().state

    override fun createPanel(): DialogPanel = panel {
        row {
            checkBox(UiLabelsBundle.message("enableCodeLenses"))
                .bindSelected(settings::enableCodeLenses)
                .comment(UiLabelsBundle.message("enableCodeLensesComment"))
        }

        row {
            checkBox(UiLabelsBundle.message("enableAutoRefactor"))
                .bindSelected(settings::enableAutoRefactor)
                .comment(UiLabelsBundle.message("enableAutoRefactorComment"))
        }

        row {
            checkBox(UiLabelsBundle.message("previewCodeHealthGate"))
                .bindSelected(settings::previewCodeHealthGate)
                .comment(UiLabelsBundle.message("previewCodeHealthGateComment"))
        }

        row {
            checkBox(UiLabelsBundle.message("gitignore"))
                .bindSelected(settings::excludeGitignoreFiles)
                .comment(UiLabelsBundle.message("gitignoreComment"))
        }

        panel {
            groupRowsRange(UiLabelsBundle.message("server")) {
                row(UiLabelsBundle.message("serverUrl")) {
                    textField()
                        .align(Align.FILL)
                        .resizableColumn()
                        .comment(UiLabelsBundle.message("serverUrlComment"))
                        .bindText(settings::serverUrl)
                }
            }

            groupRowsRange(UiLabelsBundle.message("editor")) {
                row(UiLabelsBundle.message("foldingRangeProvider")) {
                    comboBox(listOf("All", "TODO"))
                        .align(Align.FILL)
                        .resizableColumn()
                        .comment(
                            UiLabelsBundle.message("foldingRangeProviderComment"),
                            maxLineLength = MAX_LINE_LENGTH_WORD_WRAP
                        )
                }

                row(UiLabelsBundle.message("defaultFormatter")) {
                    comboBox(listOf("None", "TODO"))
                        .align(Align.FILL)
                        .resizableColumn()
                        .comment(
                            UiLabelsBundle.message("defaultFormatterComment"),
                            maxLineLength = MAX_LINE_LENGTH_WORD_WRAP
                        )
                }
            }

            groupRowsRange(UiLabelsBundle.message("notebook")) {
                row(UiLabelsBundle.message("notebookFormatter")) {
                    comboBox(listOf("None", "TODO"))
                        .align(Align.FILL)
                        .resizableColumn()
                        .comment(
                            UiLabelsBundle.message("notebookFormatterComment"),
                            maxLineLength = MAX_LINE_LENGTH_WORD_WRAP
                        )
                }
            }
        }
    }
}
