package com.codescene.jetbrains.util

import com.intellij.ui.JBColor
import java.awt.Color

object Constants {
    const val CODESCENE = "CodeScene"
    const val CODESCENE_PLUGIN_ID = "com.codescene.vanilla"
    const val CODESCENE_SERVER_URL = "https://codescene.io"
    const val DOCUMENTATION_URL = "$CODESCENE_SERVER_URL/docs/"
    const val EXAMPLE_SERVER_URL = "https://domain.com"
    const val DOCUMENTATION_BASE_PATH = "docs/"
    const val ISSUES_PATH = "${DOCUMENTATION_BASE_PATH}issues/"
    const val STYLE_BASE_PATH = "styles/"
    const val IMAGES_BASE_PATH = "images/"
    const val LOGO_PATH = "${IMAGES_BASE_PATH}logo.svg"
    const val CODESCENE_URL = "https://codescene.com"
    const val CODE_HEALTH_URL = "$CODESCENE_URL/product/code-health#:~:text=Code%20Health%20is%20an%20aggregated,negative%20outcomes%20for%20your%20project"
    const val TERMS_AND_CONDITIONS_URL = "$CODESCENE_URL/policies"
    const val AI_PRINCIPLES_URL = "$CODESCENE_URL/ace/principles"
    const val CONTACT_URL = "$CODESCENE_URL/company/contact-us"
    const val SUPPORT_URL = "https://supporthub.codescene.com/kb-tickets/new"
    val RED = JBColor(Color(224, 82, 92), Color(224, 82, 92))
    val GREEN = JBColor(Color(79, 159, 120), Color(79, 159, 120))
    val ORANGE = JBColor(Color(250, 163, 125), Color(238, 147, 107))

    const val GENERAL_CODE_HEALTH = "General Code Health"
    const val CODE_HEALTH_MONITOR = "Code Health Monitor"

    const val BRAIN_CLASS = "Brain Class"
    const val BRAIN_METHOD = "Brain Method"
    const val BUMPY_ROAD_AHEAD = "Bumpy Road Ahead"
    const val COMPLEX_CONDITIONAL = "Complex Conditional"
    const val COMPLEX_METHOD = "Complex Method"
    const val CONSTRUCTOR_OVER_INJECTION = "Constructor Over Injection"
    const val DUPLICATED_ASSERTION_BLOCKS = "Duplicated Assertion Blocks"
    const val CODE_DUPLICATION = "Code Duplication"
    const val FILE_SIZE_ISSUE = "File Size Issue"
    const val EXCESS_NUMBER_OF_FUNCTION_ARGUMENTS = "Excess Number of Function Arguments"
    const val NUMBER_OF_FUNCTIONS_IN_A_SINGLE_MODULE = "Number of Functions in a Single Module"
    const val GLOBAL_CONDITIONALS = "Global Conditionals"
    const val DEEP_GLOBAL_NESTED_COMPLEXITY = "Deep, Global Nested Complexity"
    const val HIGH_DEGREE_OF_CODE_DUPLICATION = "High Degree of Code Duplication"
    const val LARGE_ASSERTION_BLOCKS = "Large Assertion Blocks"
    const val LARGE_EMBEDDED_CODE_BLOCK = "Large Embedded Code Block"
    const val LARGE_METHOD = "Large Method"
    const val LINES_OF_CODE_IN_A_SINGLE_FILE = "Lines of Code in a Single File"
    const val LINES_OF_DECLARATION_IN_A_SINGLE_FILE = "Lines of Declaration in a Single File"
    const val LOW_COHESION = "Low Cohesion"
    const val MISSING_ARGUMENTS_ABSTRACTIONS = "Missing Arguments Abstractions"
    const val MODULARITY_ISSUE = "Modularity Issue"
    const val DEEP_NESTED_COMPLEXITY = "Deep, Nested Complexity"
    const val OVERALL_CODE_COMPLEXITY = "Overall Code Complexity"
    const val POTENTIALLY_LOW_COHESION = "Potentially Low Cohesion"
    const val PRIMITIVE_OBSESSION = "Primitive Obsession"
    const val STRING_HEAVY_FUNCTION_ARGUMENTS = "String Heavy Function Arguments"

    // telemetry constants
    const val TELEMETRY_EDITOR_TYPE = "intellij"
    const val TELEMETRY_ON_ACTIVATE_EXTENSION = "on_activate_extension"
    const val TELEMETRY_OPEN_CODE_HEALTH_DOCS = "open_code_health_docs"
    const val TELEMETRY_OPEN_DOCS_PANEL = "open_interactive_docs_panel"
    const val TELEMETRY_OPEN_SETTINGS = "control_center/open-settings"
    const val TELEMETRY_OPEN_LINK = "control_center/open-link"
    const val TELEMETRY_MONITOR_FILE_ADDED = "code_health_monitor/file_added"
    const val TELEMETRY_MONITOR_FILE_UPDATED = "code_health_monitor/file_updated"
    const val TELEMETRY_MONITOR_FILE_REMOVED = "code_health_monitor/file_removed"
    const val TELEMETRY_DETAILS_FUNCTION_SELECTED = "code_health_details/function_selected"
    const val TELEMETRY_DETAILS_FUNCTION_DESELECTED = "code_health_details/function_deselected"

    // visibility events ?
    const val TELEMETRY_SETTINGS_VISIBILITY = "control_center/visibility"
    const val TELEMETRY_MONITOR_VISIBILITY = "code_health_monitor/visibility"
    const val TELEMETRY_DETAILS_VISIBILITY = "code_health_details/visibility"

    // not yet implemented
    const val TELEMETRY_STATS = "stats"
}