package com.codescene.jetbrains.components.settings.tab

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.api.AceService
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DialogTitleCapitalization")
class SettingsTab : BoundConfigurable(UiLabelsBundle.message("settingsTitle")) {
    private val settings = CodeSceneGlobalSettingsStore.getInstance().state
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun createPanel(): DialogPanel {
        return panel {
            row {
                checkBox(UiLabelsBundle.message("enableCodeLenses"))
                    .bindSelected(settings::enableCodeLenses)
                    .comment(UiLabelsBundle.message("enableCodeLensesComment"))
            }

//            row {
//                checkBox(UiLabelsBundle.message("enableAutoRefactor"))
//                    .bindSelected(settings::enableAutoRefactor)
//                    .comment(UiLabelsBundle.message("enableAutoRefactorComment"))
//            }

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

    override fun apply() {
        super.apply()
        scope.launch {
            AceService.getInstance().runPreflight(true)
        }
    }
}
