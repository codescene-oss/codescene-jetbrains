package com.codescene.jetbrains.components.webview.data.message

enum class LifecycleMessages(val value: String) {
    INIT("init"),
    UPDATE_RENDERER("update-renderer")
}

enum class LoginMessages(val value: String) {
    OPEN_LOGIN("open-login"),
    OPEN_HOME("open-home"),
    INIT_LOGIN("init-login")
}

enum class PanelMessages(val value: String) {
    APPLY("apply"),
    CLOSE("close"),
    RETRY("retry"),
    REJECT("reject"),
    COPY_CODE("copyCode"),
    SHOW_DIFF("showDiff"),
    ACKNOWLEDGED("acknowledged"),
    OPEN_DOCS_FOR_FUNCTION("open-docs-for-function"),
    REQUEST_AND_PRESENT_REFACTORING("request-and-present-refactoring"),
}

enum class EditorMessages(val value: String) {
    GOTO_FUNCTION_LOCATION("goto-function-location"),
    OPEN_SETTINGS("open-settings"),
    OPEN_LINK("open-link") // Custom message appended on script initialization
}

enum class StateChangeMessages(val value: String) {
    COMMIT_BASELINE("commitBaseline")
}