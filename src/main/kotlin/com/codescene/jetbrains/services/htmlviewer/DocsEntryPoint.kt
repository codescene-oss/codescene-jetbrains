package com.codescene.jetbrains.services.htmlviewer

enum class DocsEntryPoint(
    val value: String,
) {
    ACTION("action"),
    CODE_VISION("codelens (review)"),
    INTENTION_ACTION("diagnostic-item"),
    CODE_HEALTH_DETAILS("code-health-details"),
}
