package com.codescene.jetbrains.components.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.event.MouseEvent
import javax.swing.JOptionPane

class CustomStatusBarWidget : StatusBarWidget.TextPresentation, StatusBarWidget {
    private var statusBar: StatusBar? = null

    override fun ID(): String = "CustomStatusBarWidget"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getAlignment(): Float = 0.5f

    override fun getText(): String = "Active"

    override fun getTooltipText(): String = "CodeScene ACE Status: ${getText()}"

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        statusBar = null
    }

    override fun getClickConsumer(): Consumer<MouseEvent>? = Consumer<MouseEvent> {
        JOptionPane.showMessageDialog(null, "You clicked the custom status bar widget!")
    }


}

class CustomStatusBarWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String = "CustomStatusBarWidgetFactory"

    override fun getDisplayName(): String = "CodeScene ACE Status"

    override fun isAvailable(project: Project): Boolean = true

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = CustomStatusBarWidget()

    override fun isEnabledByDefault(): Boolean = true
}

// Helper class to programmatically control widget visibility
//class WidgetManager(private val project: Project) {
//
//    // Get the StatusBarWidgetsManager service
//    private val widgetsManager = project.service<StatusBarWidgetsManager>()
//
//    // Enable the widget programmatically
//    fun enableWidget() {
//        widgetsManager.canBeEnabledOnStatusBar(CustomStatusBarWidgetFactory(), C)
//    }
//
//    // Disable the widget programmatically
//    fun disableWidget() {
//        widgetsManager.disableWidget(MyCustomStatusBarWidget.ID)
//    }
//
//    // Check if the widget is currently enabled
//    fun isWidgetEnabled(): Boolean {
//        return widgetsManager.isWidgetEnabled(MyCustomStatusBarWidget.ID)
//    }
//
//    // Toggle widget visibility
//    fun toggleWidget() {
//        if (isWidgetEnabled()) {
//            disableWidget()
//        } else {
//            enableWidget()
//        }
//    }
//}