package com.codescene.jetbrains.util

import com.codescene.data.delta.ChangeDetail
import com.codescene.data.delta.Delta
import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Range
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_DECREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_INCREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_NEUTRAL
import com.codescene.jetbrains.CodeSceneIcons.CODE_SMELL_FOUND
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.components.codehealth.monitor.tree.NodeType
import com.codescene.jetbrains.services.CodeSceneDocumentationService
import com.codescene.jetbrains.services.DocsSourceType
import com.codescene.jetbrains.services.DocumentationParams
import com.codescene.jetbrains.util.Constants.GREEN
import com.codescene.jetbrains.util.Constants.ORANGE
import com.codescene.jetbrains.util.Constants.RED
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon

data class SubHeader(
    val fileIcon: Icon,
    val fileName: String,
    val status: String,
    val statusIcon: Icon
)

data class Paragraph(
    val body: String,
    val heading: String,
    val icon: Icon? = null,
    val codeSmell: CodeSmell? = null
)

enum class CodeHealthDetailsType {
    FILE,
    HEALTH,
    FUNCTION
}

data class HealthData(
    val header: String,
    val status: String,
    val score: Double,
)

data class CodeHealthDetails(
    val filePath: String,
    val header: String,
    val subHeader: SubHeader,
    val body: List<Paragraph>,
    val type: CodeHealthDetailsType,
    val healthData: HealthData? = null,
)

enum class HealthState(val label: String, val color: JBColor) {
    UNHEALTHY("Unhealthy", RED),
    PROBLEMATIC("Problematic", ORANGE),
    HEALTHY("Healthy", GREEN);

    companion object {
        fun fromScore(score: Double): HealthState =
            when {
                score < 4.0 -> UNHEALTHY
                score in 4.0..8.9 -> PROBLEMATIC
                else -> HEALTHY
            }
    }
}

data class CodeHealthHeader(
    val subText: String,
    val text: String,
    val icon: Icon
)

fun resolveHealthBadge(score: Double): Pair<String, JBColor> = HealthState.fromScore(score).let { it.label to it.color }

private fun createSubHeader(
    file: Pair<String, String>?,
    status: String,
    statusIcon: Icon,
    type: CodeHealthDetailsType
): SubHeader {
    val fileType = FileTypeManager.getInstance().getFileTypeByExtension(file?.second ?: "")

    return SubHeader(
        fileName = if (type == CodeHealthDetailsType.FILE) fileType.displayName else file!!.first,
        fileIcon = fileType.icon,
        status = status,
        statusIcon = statusIcon
    )
}

private fun <T> extractUsingRegex(input: String, regex: Regex, extractor: (MatchResult.Destructured) -> T?): T? {
    val matchResult = regex.find(input)

    return if (matchResult != null)
        extractor(matchResult.destructured)
    else null
}

private fun resolveStatus(delta: Delta, type: NodeType, percentage: String) =
    when (type) {
        NodeType.CODE_HEALTH_NEUTRAL -> ""
        NodeType.CODE_HEALTH_INCREASE ->
            "Increased to ${round(delta.newScore)} $percentage"

        NodeType.CODE_HEALTH_DECREASE ->
            "Declined from ${round(delta.oldScore)} $percentage"

        else -> throw IllegalArgumentException("Unexpected node type: $type")
    }

private fun resolveCodeHealthHeader(type: NodeType, newScore: Double, oldScore: Double): CodeHealthHeader? =
    when (type) {
        NodeType.CODE_HEALTH_NEUTRAL -> {
            CodeHealthHeader(
                "Code Health Unchanged", round(oldScore).toString(), CODE_HEALTH_NEUTRAL
            )
        }

        NodeType.CODE_HEALTH_INCREASE -> {
            CodeHealthHeader("Code Health Increasing", round(newScore).toString(), CODE_HEALTH_INCREASE)
        }

        NodeType.CODE_HEALTH_DECREASE -> {
            CodeHealthHeader(
                "Code Health Decreasing", round(newScore).toString(),
                CODE_HEALTH_DECREASE
            )
        }

        else -> null
    }

private fun getHealthFinding(
    file: Pair<String, String>?,
    finding: CodeHealthFinding,
    delta: Delta
): CodeHealthDetails {
    val healthHeader = resolveCodeHealthHeader(finding.nodeType, delta.newScore, delta.oldScore)

    return CodeHealthDetails(
        filePath = finding.filePath,
        header = UiLabelsBundle.message("healthScore"),
        subHeader = createSubHeader(file, healthHeader!!.subText, healthHeader.icon, CodeHealthDetailsType.HEALTH),
        healthData = HealthData(
            healthHeader.subText,
            resolveStatus(delta, finding.nodeType, percentage = finding.additionalText),
            round(delta.newScore)
        ),
        body = listOf(
            Paragraph(
                icon = null,
                body = UiLabelsBundle.message("healthScoreDetails"),
                heading = UiLabelsBundle.message("whyThisIsImportant")
            )
        ),
        type = CodeHealthDetailsType.HEALTH
    )
}

private fun getFunctionFindingBody(changeDetails: List<ChangeDetail>?, finding: CodeHealthFinding) =
    changeDetails?.map { it ->
        val changeType = it.changeType.replaceFirstChar { it.uppercaseChar() }
        val body = it.description.replace(finding.displayName, "<code>${finding.displayName}</code>")

        val codeSmell = CodeSmell().apply {
            category = it.category
            details = it.description
            highlightRange = Range().apply {
                startLine = it.position.line
                endLine = it.position.line
                startColumn = it.position.column
                endColumn = it.position.column
            }
        }

        Paragraph(
            body = body,
            heading = "$changeType: ${it.category}",
            icon = CODE_SMELL_FOUND,
            codeSmell = codeSmell
        )
    } ?: listOf()

fun isMatchingFinding(displayName: String, startLine: Int?, finding: CodeHealthFinding) =
    displayName == finding.displayName && startLine == finding.focusLine

private fun getFunctionFinding(
    file: Pair<String, String>?,
    finding: CodeHealthFinding,
    delta: Delta
): CodeHealthDetails {
    val changeDetails = delta.functionLevelFindings
        .find { isMatchingFinding(it.function.name, it.function.range.startLine, finding) }?.changeDetails
    val subHeaderLabel = if (changeDetails != null && changeDetails.size > 1)
        UiLabelsBundle.message("multipleCodeSmells")
    else
        UiLabelsBundle.message("functionSmell")

    return CodeHealthDetails(
        filePath = finding.filePath,
        header = finding.displayName,
        subHeader = createSubHeader(
            file,
            subHeaderLabel,
            AllIcons.Nodes.WarningIntroduction,
            CodeHealthDetailsType.FUNCTION
        ),
        body = getFunctionFindingBody(changeDetails, finding),
        type = CodeHealthDetailsType.FUNCTION
    )
}

private fun getFileFinding(
    file: Pair<String, String>?,
    finding: CodeHealthFinding
) = CodeHealthDetails(
    filePath = finding.filePath,
    header = finding.displayName,
    subHeader = createSubHeader(
        file,
        UiLabelsBundle.message("fileIssue"),
        AllIcons.Nodes.WarningIntroduction,
        CodeHealthDetailsType.FILE
    ),
    body = listOf(Paragraph(finding.tooltip, "Problem")),
    type = CodeHealthDetailsType.FILE
)

fun getHealthFinding(delta: Delta, finding: CodeHealthFinding): CodeHealthDetails? {
    val file = extractUsingRegex(finding.filePath, Regex(".*/([^/]+)\\.([^.]+)$")) { (fileName, extension) ->
        fileName to extension
    }

    return when (finding.nodeType) {
        NodeType.CODE_HEALTH_INCREASE, NodeType.CODE_HEALTH_DECREASE, NodeType.CODE_HEALTH_NEUTRAL ->
            getHealthFinding(file, finding, delta)

        NodeType.FILE_FINDING, NodeType.FILE_FINDING_FIXED -> getFileFinding(file, finding)
        NodeType.FUNCTION_FINDING -> getFunctionFinding(file, finding, delta)
        else -> null
    }
}

private fun handleMouseClick(project: Project, codeSmell: CodeSmell, filePath: String) {
    val editorManager = FileEditorManager.getInstance(project)
    val file = LocalFileSystem.getInstance().findFileByPath(filePath)
    val documentationService = CodeSceneDocumentationService.getInstance(project)

    file?.let {
        if (!editorManager.isFileOpen(file)) editorManager.openFile(file, true)

        val fileDescriptor = OpenFileDescriptor(
            project,
            file,
            codeSmell.highlightRange.startLine - 1,
            codeSmell.highlightRange.startColumn - 1
        )

        editorManager.openTextEditor(fileDescriptor, true)
        editorManager.selectedTextEditor?.let {
            documentationService.openDocumentationPanel(
                DocumentationParams(
                    it,
                    codeSmell,
                    DocsSourceType.CODE_HEALTH_DETAILS
                )
            )
        }
    }
}

fun getMouseAdapter(
    project: Project, codeSmell: CodeSmell, filePath: String
) = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
        handleMouseClick(project, codeSmell, filePath)
    }

    override fun mouseEntered(e: MouseEvent) {
        e.component.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }

    override fun mouseExited(e: MouseEvent) {
        e.component.cursor = Cursor.getDefaultCursor()
    }
}