package com.codescene.jetbrains.components.controlCenter.panel

import com.codescene.jetbrains.services.ExtensionSettings
import com.intellij.ui.dsl.builder.*

class SettingsComponent {
    val URL_PATTERN =
        "^https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$"

    var settingsState = ExtensionSettings.State()

    private fun urlValidation(url: String?) =
        url?.matches(Regex(URL_PATTERN)) == true

    val mainPanel = panel {
        group("CodeScene Settings") {
            row {
                checkBox("Enable Code Lenses").bindSelected(settingsState::enableCodeLenses)
                    .comment("Enable CodeScene Code Lenses")
            }

            row {
                checkBox("Enable Auto Refactor")
                    .bindSelected(settingsState::enableAutoRefactor)
                    .comment("Enable CodeScene ACE. This is currently only available for customers part of the ACE beta program.")
            }

            row {
                checkBox("Preview Code Health Gate")
                    .bindSelected(settingsState::previewCodeHealthGate)
                    .comment("Preview the experimental Code Health Gate (beta)")
            }

            row {
                checkBox("Gitignore")
                    .bindSelected(settingsState::excludeGitignoreFiles)
                    .comment("Exclude files in .gitignore from analysis")
            }
        }

        panel {
            groupRowsRange("Server") {
                row("Server URL") {
                    textField()
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .comment("https://domain.com")
                        .bindText(settingsState::cloudUrl)
                        .cellValidation {
                            addInputRule("Must be a valid URL") {
                                urlValidation(it.text)
                            }
                        }

                }
            }

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