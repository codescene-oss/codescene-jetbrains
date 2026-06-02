package com.codescene.jetbrains.platform.telemetry

import com.intellij.openapi.project.ProjectManager

internal fun openProjectPathPrefixes(): List<String> =
    ProjectManager.getInstance().openProjects.mapNotNull { it.basePath }.distinct()
