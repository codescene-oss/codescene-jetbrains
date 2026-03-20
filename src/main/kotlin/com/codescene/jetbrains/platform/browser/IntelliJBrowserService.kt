package com.codescene.jetbrains.platform.browser

import com.codescene.jetbrains.core.contracts.IBrowserService
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.Service

@Service
class IntelliJBrowserService : IBrowserService {
    override fun openUrl(url: String) {
        BrowserUtil.browse(url)
    }
}
