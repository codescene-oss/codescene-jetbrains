package com.codescene.jetbrains.util

/**
 * Singleton object to store all telemetry events we log in our plugin.
 * When adding new event here, please update [telemetrySamples] in resources.
 * This is needed for user transparency, to see all the exact data being logged.
 */
object TelemetryEvents {
    const val ON_ACTIVATE_EXTENSION = "on_activate_extension"

    const val OPEN_CODE_HEALTH_DOCS = "open_code_health_docs"
    const val OPEN_DOCS_PANEL = "open_interactive_docs_panel"

    const val SETTINGS_VISIBILITY = "control_center/visibility"
    const val OPEN_SETTINGS = "control_center/open-settings"
    const val OPEN_LINK = "control_center/open-link"

    const val MONITOR_VISIBILITY = "code_health_monitor/visibility"
    const val MONITOR_FILE_ADDED = "code_health_monitor/file_added"
    const val MONITOR_FILE_UPDATED = "code_health_monitor/file_updated"
    const val MONITOR_FILE_REMOVED = "code_health_monitor/file_removed"

    const val DETAILS_VISIBILITY = "code_health_details/visibility"
    const val DETAILS_FUNCTION_SELECTED = "code_health_details/function_selected"
    const val DETAILS_FUNCTION_DESELECTED = "code_health_details/function_deselected"

    const val REVIEW_OR_DELTA_TIMEOUT = "review_or_delta_timeout"
    const val ANALYSIS_PERFORMANCE = "analysis/performance"

    const val ACE_INFO_PRESENTED = "ace_info/presented"
    const val ACE_INFO_ACKNOWLEDGED = "ace_info/acknowledged"
    const val ACE_REFACTOR_REQUESTED = "refactor/requested"
    const val ACE_REFACTOR_PRESENTED = "refactor/presented"
    const val ACE_REFACTOR_APPLIED = "refactor/applied"
    const val ACE_REFACTOR_REJECTED = "refactor/rejected"

    // not yet implemented
    const val STATS = "stats"
}