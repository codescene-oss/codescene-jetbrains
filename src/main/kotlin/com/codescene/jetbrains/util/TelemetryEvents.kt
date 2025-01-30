package com.codescene.jetbrains.util

object TelemetryEvents {
    const val TELEMETRY_ON_ACTIVATE_EXTENSION = "on_activate_extension"
    const val TELEMETRY_OPEN_CODE_HEALTH_DOCS = "open_code_health_docs"
    const val TELEMETRY_OPEN_DOCS_PANEL = "open_interactive_docs_panel"
    const val TELEMETRY_SETTINGS_VISIBILITY = "control_center/visibility"
    const val TELEMETRY_OPEN_SETTINGS = "control_center/open-settings"
    const val TELEMETRY_OPEN_LINK = "control_center/open-link"
    const val TELEMETRY_MONITOR_VISIBILITY = "code_health_monitor/visibility"
    const val TELEMETRY_MONITOR_FILE_ADDED = "code_health_monitor/file_added"
    const val TELEMETRY_MONITOR_FILE_UPDATED = "code_health_monitor/file_updated"
    const val TELEMETRY_MONITOR_FILE_REMOVED = "code_health_monitor/file_removed"
    const val TELEMETRY_DETAILS_VISIBILITY = "code_health_details/visibility"
    const val TELEMETRY_DETAILS_FUNCTION_SELECTED = "code_health_details/function_selected"
    const val TELEMETRY_DETAILS_FUNCTION_DESELECTED = "code_health_details/function_deselected"

    // not yet implemented
    const val TELEMETRY_STATS = "stats"
}