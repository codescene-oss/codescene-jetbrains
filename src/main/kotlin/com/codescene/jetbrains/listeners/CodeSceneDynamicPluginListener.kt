package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Log
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor

class CodeSceneDynamicPluginListener : DynamicPluginListener {

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        super.beforePluginUnload(pluginDescriptor, isUpdate)

        Log.info("Unloaded ${pluginDescriptor.pluginId} ${pluginDescriptor.version} plugin", this::class::simpleName.toString())
        CodeSceneGlobalSettingsStore.getInstance().updateTermsAndConditionsAcceptance(false)
    }
}