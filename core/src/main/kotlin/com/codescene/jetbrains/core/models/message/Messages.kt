package com.codescene.jetbrains.core.models.message

enum class LifecycleMessages(val value: String) {
    INIT("init"),
    UPDATE_RENDERER("update-renderer"),
}

enum class PanelMessages(val value: String) {
    APPLY("apply"),
    CANCEL("cancel"),
    CLOSE("close"),
    RETRY("retry"),
    REJECT("reject"),
    COPY_CODE("copyCode"),
    ACKNOWLEDGED("acknowledged"),
    OPEN_DOCS_FOR_FUNCTION("open-docs-for-function"),
    REQUEST_AND_PRESENT_REFACTORING("request-and-present-refactoring"),
}

enum class EditorMessages(val value: String) {
    SHOW_DIFF("showDiff"),
    OPEN_LINK("open-link"), // Custom message appended on script initialization,
    OPEN_SETTINGS("open-settings"),
    GOTO_FUNCTION_LOCATION("goto-function-location"),
}

enum class UnsupportedMessages(val value: String) {
    SHOW_LOGOUTPUT("showLogoutput"),
    COMMIT_BASELINE("commitBaseline"),
    OPEN_CODE_HEALTH_VIEW("open-code-health-view"),
    CLOSE_ONBOARDING("close-onboarding"),
    SHOW_ONBOARDING("show-onboarding"),
    OPEN_HOME("open-home"),
    STOP_ANALYSIS("stop-analysis"),
    START_ANALYSIS("start-analysis"),
    OPEN_AUTH("open-auth"),
    AUTH_SIGN_IN("auth-sign-in"),
    AUTH_OPEN_BROWSER_AGAIN("auth-open-browser-again"),
    AUTH_CANCEL_SIGN_IN("auth-cancel-sign-in"),
    AUTH_SIGN_OUT("auth-sign-out"),
    AUTH_RETRY("auth-retry"),
    AUTH_SELECT_ACCOUNT("auth-select-account"),
    AUTH_LINK_PROJECT("auth-link-project"),
    AUTH_SET_ACCESS_TOKEN("auth-set-access-token"),
    AUTH_REMOVE_ACCESS_TOKEN("auth-remove-access-token"),
    AUTH_OPEN_SERVER_SETTING("auth-open-server-setting"),
    AUTH_OPEN_PAT_PAGE("auth-open-pat-page"),
}
