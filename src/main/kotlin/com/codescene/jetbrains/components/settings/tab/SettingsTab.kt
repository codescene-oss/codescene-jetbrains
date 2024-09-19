package com.codescene.jetbrains.components.settings.tab

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*

@Suppress("DialogTitleCapitalization")
class SettingsTab : BoundConfigurable("Settings") {
    private val settings = CodeSceneGlobalSettingsStore.getInstance().state

    override fun createPanel(): DialogPanel {
        return panel {
            row {
                checkBox("Enable Code Lenses").bindSelected(settings::enableCodeLenses)
                    .comment("Enable CodeScene Code Lenses")
            }

            row {
                checkBox("Enable Auto Refactor")
                    .bindSelected(settings::enableAutoRefactor)
                    .comment("Enable CodeScene ACE. This is currently only available for customers part of the ACE beta program.")
            }

            row {
                checkBox("Preview Code Health Gate")
                    .bindSelected(settings::previewCodeHealthGate)
                    .comment("Preview the experimental Code Health Gate (beta)")
            }

            row {
                checkBox("Gitignore")
                    .bindSelected(settings::excludeGitignoreFiles)
                    .comment("Exclude files in .gitignore from analysis")
            }

            panel {
                groupRowsRange("Server") {
                    row("Server URL") {
                        textField()
                            .align(AlignX.FILL)
                            .resizableColumn()
                            .comment("https://domain.com")
                            .bindText(settings::serverUrl)
                    }
                }

                //TODO: Make the combo boxes selectable when value type is determined
                groupRowsRange("Editor") {
                    row("Folding Range Provider") {
                        comboBox(listOf("All", "TODO"))
                            .align(AlignX.FILL)
                            .resizableColumn()
                            .comment(
                                "Defines a default folding range provider which takes precedence over all other folding range providers. Must be the identifier of an extension contributing a folding range provider.",
                                maxLineLength = MAX_LINE_LENGTH_WORD_WRAP
                            )
                    }

                    row("Default Formatter") {
                        comboBox(listOf("None", "TODO"))
                            .align(AlignX.FILL)
                            .resizableColumn()
                            .comment(
                                "Defines a default formatter which takes precedence over all other formatter settings. Must be the identifier of an extension contributing a formatter.",
                                maxLineLength = MAX_LINE_LENGTH_WORD_WRAP
                            )
                    }
                }

                groupRowsRange("Notebook") {
                    row("Default Formatter") {
                        comboBox(listOf("None", "TODO"))
                            .align(AlignX.FILL)
                            .resizableColumn()
                            .comment(
                                "Defines a default notebook formatter which takes precedence over all other formatter settings. Must be the identifier of an extension contributing a formatter.",
                                maxLineLength = MAX_LINE_LENGTH_WORD_WRAP
                            )
                    }
                }
            }
        }
    }
}