package com.codescene.jetbrains.components.settings.tab

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.notifier.AceStatusRefreshNotifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

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
            val enableAceCheckBox = checkBox(UiLabelsBundle.message("enableAutoRefactor"))
                .bindSelected(settings::enableAutoRefactor)
                .comment(UiLabelsBundle.message("enableAutoRefactorComment"))

            enableAceCheckBox.component.addActionListener(object : ActionListener {
                override fun actionPerformed(e: ActionEvent?) {
                    ApplicationManager.getApplication().messageBus.syncPublisher(AceStatusRefreshNotifier.TOPIC)
                        .refresh(true)
                }
            })
        }

        /*
        TODO: Uncomment this code when Code Health gate is integrated in plugin:
            row {
                checkBox(UiLabelsBundle.message("previewCodeHealthGate"))
                    .bindSelected(settings::previewCodeHealthGate)
                    .comment(UiLabelsBundle.message("previewCodeHealthGateComment"))
            }
        */

        row {
            checkBox(UiLabelsBundle.message("gitignore"))
                .bindSelected(settings::excludeGitignoreFiles)
                .comment(UiLabelsBundle.message("gitignoreComment"))
        }

//        panel {
//            groupRowsRange(UiLabelsBundle.message("server")) {
//                row(UiLabelsBundle.message("serverUrl")) {
//                    textField()
//                        .align(Align.FILL)
//                        .enabled(false) //TODO: Enable when functionality is implemented
//                        .resizableColumn()
//                        .comment(EXAMPLE_SERVER_URL)
//                        .bindText(settings::serverUrl)
//                }
//            }
//        }
    }
}
