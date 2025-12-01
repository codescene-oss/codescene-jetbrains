package com.codescene.jetbrains.util

import com.codescene.jetbrains.UiLabelsBundle
import com.intellij.ui.ColorUtil
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
    const val CODE_HEALTH_URL = "${DOCUMENTATION_URL}guides/technical/code-health.html"
    const val TERMS_AND_CONDITIONS_URL = "$CODESCENE_URL/policies"
    const val CONTACT_URL = "$CODESCENE_URL/company/contact-us"
    const val CONTACT_US_ABOUT_ACE_URL = "${CODESCENE_URL}/contact-us-about-codescene-ace"
    const val FREE_TRIAL_URL = "$CODESCENE_URL/product/free-trial?trial-experiment-variant=free_trial_landing_page_with_form"
    const val SUPPORT_URL = "https://supporthub.codescene.com/kb-tickets/new"
    const val AI_PRINCIPLES_URL = "$CODESCENE_URL/product/ace/principles"

    const val REPOSITORY_URL = "https://github.com/codescene-oss/codescene-jetbrains"
    const val TELEMETRY_SAMPLES_URL =
        "$REPOSITORY_URL/tree/master/src/main/resources/telemetrySamples"
    const val TELEMETRY_EVENTS_URL =
        "$REPOSITORY_URL/tree/master/src/main/kotlin/com/codescene/jetbrains/util/TelemetryEvents.kt"

    val RED = JBColor(Color(224, 82, 92), Color(224, 82, 92))
    val GREEN = JBColor(Color(79, 159, 120), Color(79, 159, 120))
    val ORANGE = JBColor(Color(250, 163, 125), Color(238, 147, 107))
    val BLUE = ColorUtil.fromHex("#3f6dc7")

    const val GENERAL_CODE_HEALTH = "General Code Health"
    const val CODE_HEALTH_MONITOR = "Code Health Monitor"
    const val TELEMETRY_EDITOR_TYPE = "jetbrains"

    // CodeScene actions
    const val REVIEW = "review"
    const val DELTA = "delta"
    const val ACE = "ace"

    const val INFO_NOTIFICATION_GROUP = "CodeScene Information"

    // ACE constants
    const val ACE_ACKNOWLEDGEMENT_FILE = "ace-info.md"
    const val ACE_STATUS = "CodeScene ACE Status"
    const val ACE_NOTIFICATION_GROUP = "CodeScene ACE"
    const val ACE_REFACTORING_SUGGESTION = "Refactoring suggestion"
    const val ACE_REFACTORING_RECOMMENDATION = "Refactoring recommendation"
    const val ACE_REFACTORING_RESULTS = "Refactoring results"
    const val ACE_ACKNOWLEDGEMENT = "CodeScene ACE Auto-Refactoring"

    // Code Smell constants
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

    // CWF
    const val IDE_TYPE = "JetBrains"
    val ALLOWED_DOMAINS = listOf(
        "https://refactoring.com",
        "https://en.wikipedia.org",
        "https://codescene.io",
        "https://codescene.com",
        "https://blog.ploeh.dk/2018/08/27/on-constructor-over-injection/",
        "https://supporthub.codescene.com"
    )
    const val DELTA_ANALYSIS_JOB = "deltaAnalysis"
    const val AUTO_REFACTOR_JOB = "autoRefactor"
    const val JOB_STATE_RUNNING = "running"
    const val JOB_STATE_QUEUED = "queued"

    val codeSceneWindowFileNames = listOf(
        UiLabelsBundle.message("ace"),
        UiLabelsBundle.message("codeSmellDocs"),
        UiLabelsBundle.message("aceAcknowledge"),
    )

    // ACE Status Bar Widget
    const val SIGNED_IN = "CodeScene ACE is active"
    const val SIGNED_OUT = "Configure ACE auth token in extension settings"
    const val DEACTIVATED = "Enable ACE in the extension settings"
    const val OUT_OF_CREDITS = "Out of ACE credits"
    const val RETRY = "Retry ACE activation"

    // Feature flags
    const val CWF_FLAG = "FEATURE_CWF"
    const val ACE_FLAG = "FEATURE_ACE"
}