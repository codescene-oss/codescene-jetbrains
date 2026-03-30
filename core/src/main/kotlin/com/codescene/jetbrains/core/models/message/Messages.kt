package com.codescene.jetbrains.core.models.message

enum class LifecycleMessages(val value: String) {
    INIT("init"),
    UPDATE_RENDERER("update-renderer"),
}

enum class LoginMessages(val value: String) {
    OPEN_LOGIN("open-login"),
    OPEN_HOME("open-home"),
    INIT_LOGIN("init-login"),
}

enum class PanelMessages(val value: String) {
    APPLY("apply"),
    CLOSE("close"),
    RETRY("retry"),
    REJECT("reject"),
    COPY_CODE("copyCode"),
    ACKNOWLEDGED("acknowledged"),
    OPEN_DOCS_FOR_FUNCTION("open-docs-for-function"),
    REQUEST_AND_PRESENT_REFACTORING("request-and-present-refactoring"),
    CODE_HEALTH_DETAILS_FUNCTION_SELECTED("code-health-details-function-selected"),
    CODE_HEALTH_DETAILS_FUNCTION_DESELECTED("code-health-details-function-deselected"),
}

enum class EditorMessages(val value: String) {
    SHOW_DIFF("showDiff"),
    OPEN_LINK("open-link"), // Custom message appended on script initialization,
    OPEN_SETTINGS("open-settings"),
    GOTO_FUNCTION_LOCATION("goto-function-location"),
}

enum class StateChangeMessages(val value: String) {
    COMMIT_BASELINE("commitBaseline"),
}
