package com.codescene.jetbrains.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.delta.ChangeDetail
import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Range
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_DECREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_INCREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_NEUTRAL
import com.codescene.jetbrains.CodeSceneIcons.CODE_SMELL_FIXED
import com.codescene.jetbrains.CodeSceneIcons.CODE_SMELL_FOUND
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.components.codehealth.monitor.tree.NodeType
import com.codescene.jetbrains.services.api.deltamodels.DeltaChangeDetail
import com.codescene.jetbrains.services.api.deltamodels.NativeDelta
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
import com.codescene.jetbrains.services.htmlviewer.CodeSceneDocumentationViewer
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.services.htmlviewer.DocumentationParams
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
    val score: String,
)

data class CodeHealthDetails(
    val filePath: String,
    val header: String,
    val subHeader: SubHeader,
    val body: List<Paragraph>,
    val type: CodeHealthDetailsType,
    val healthData: HealthData? = null,
    val refactorableFunction: FnToRefactor? = null,
)

enum class HealthState(val label: String, val color: JBColor) {
    UNHEALTHY("Unhealthy", RED),
    PROBLEMATIC("Problematic", ORANGE),
    HEALTHY("Healthy", GREEN),
    UNSCORABLE("N/A", JBColor.GRAY);

    companion object {
        fun fromScore(score: String): HealthState =
            when {
                score == UNSCORABLE.label -> UNSCORABLE
                score.toDouble() < 4.0 -> UNHEALTHY
                score.toDouble() in 4.0..8.9 -> PROBLEMATIC
                else -> HEALTHY
            }
    }
}

data class CodeHealthHeader(
    val subText: String,
    val text: String,
    val icon: Icon
)

fun resolveHealthBadge(score: String): Pair<String, JBColor> = HealthState.fromScore(score).let { it.label to it.color }

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

private fun resolveStatus(delta: NativeDelta, type: NodeType, percentage: String) =
    when (type) {
        NodeType.CODE_HEALTH_NEUTRAL -> ""
        NodeType.CODE_HEALTH_INCREASE -> "Increased to ${round(delta.newScore)} $percentage"
        NodeType.CODE_HEALTH_DECREASE -> "Declined from ${round(delta.oldScore)} $percentage"

        else -> throw IllegalArgumentException("Unexpected node type: $type")
    }

private fun resolveCodeHealthHeader(type: NodeType, newScore: Double?, oldScore: Double?): CodeHealthHeader? =
    when (type) {
        NodeType.CODE_HEALTH_NEUTRAL -> {
            CodeHealthHeader(
                "Code Health Unchanged",
                if (oldScore == null) "N/A" else round(oldScore).toString(),
                CODE_HEALTH_NEUTRAL
            )
        }

        NodeType.CODE_HEALTH_INCREASE -> {
            CodeHealthHeader(
                "Code Health Increasing",
                if (newScore == null) "N/A" else round(newScore).toString(),
                CODE_HEALTH_INCREASE
            )
        }

        NodeType.CODE_HEALTH_DECREASE -> {
            CodeHealthHeader(
                "Code Health Decreasing", if (newScore == null) "N/A" else round(newScore).toString(),
                CODE_HEALTH_DECREASE
            )
        }

        else -> null
    }

private fun getHealthFinding(
    file: Pair<String, String>?,
    finding: CodeHealthFinding,
    delta: NativeDelta
): CodeHealthDetails {
    val oldScore = delta.oldScore
    val newScore = delta.newScore

    val healthHeader = resolveCodeHealthHeader(finding.nodeType, newScore, oldScore)

    return CodeHealthDetails(
        filePath = finding.filePath,
        header = UiLabelsBundle.message("healthScore"),
        subHeader = createSubHeader(file, healthHeader!!.subText, healthHeader.icon, CodeHealthDetailsType.HEALTH),
        healthData = HealthData(
            healthHeader.subText,
            resolveStatus(delta, finding.nodeType, percentage = finding.additionalText),
            round(delta.newScore).toString()
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

fun isPositiveChange(changeType: ChangeDetail.ChangeType) =
    changeType == ChangeDetail.ChangeType.FIXED || changeType == ChangeDetail.ChangeType.IMPROVED

fun canBeImproved(changeType: ChangeDetail.ChangeType) =
    changeType == ChangeDetail.ChangeType.DEGRADED || changeType == ChangeDetail.ChangeType.INTRODUCED || changeType == ChangeDetail.ChangeType.IMPROVED

private fun getFunctionFindingBody(changeDetails: List<DeltaChangeDetail>?, finding: CodeHealthFinding) =
    changeDetails?.map { it ->
        val change = it.changeType.value().replaceFirstChar { it.uppercaseChar() }
        val body = it.description.replace(finding.displayName, "<code>${finding.displayName}</code>")

        val range = if (it.line != null) Range(
            it.line,
            0,
            it.line,
            0
        ) else null
        val codeSmell = CodeSmell(it.category, range, it.description)

        Paragraph(
            body = body,
            heading = "$change: ${it.category}",
            icon = if (!isPositiveChange(it.changeType)) CODE_SMELL_FOUND else CODE_SMELL_FIXED,
            codeSmell = codeSmell
        )
    } ?: listOf()

fun isMatchingFinding(displayName: String, focusLine: Int?, finding: CodeHealthFinding): Boolean {
    var matches = displayName == finding.displayName

    if (focusLine != null && finding.focusLine != null) matches = matches && focusLine == finding.focusLine

    return matches
}

private fun getFunctionFinding(
    file: Pair<String, String>?,
    finding: CodeHealthFinding,
    delta: NativeDelta,
    project: Project
): CodeHealthDetails {
    val changeDetails = delta.functionLevelFindings
        .find {
            isMatchingFinding(
                it.function?.name ?: "",
                it.function?.range?.startLine,
                finding
            )
        }?.changeDetails
    val refactorableFunctions = AceRefactorableFunctionsCacheService.getInstance(project)
        .getAll()
        .find { it.first == finding.filePath }
        ?.second?.result ?: emptyList()
    val labelAndIcon = if ((changeDetails?.count { !isPositiveChange(it.changeType) } ?: 0) >= 1)
        "Improvement opportunity" to AllIcons.Nodes.WarningIntroduction
    else
        "Issue(s) fixed" to CODE_SMELL_FIXED

    return CodeHealthDetails(
        filePath = finding.filePath,
        header = finding.displayName,
        subHeader = createSubHeader(
            file,
            labelAndIcon.first,
            labelAndIcon.second,
            CodeHealthDetailsType.FUNCTION
        ),
        body = getFunctionFindingBody(changeDetails, finding),
        type = CodeHealthDetailsType.FUNCTION,
        refactorableFunction = getRefactorableFunction(
            finding.displayName, finding.focusLine ?: 0, refactorableFunctions
        )
    )
}

private fun getFileFinding(
    file: Pair<String, String>?,
    finding: CodeHealthFinding
): CodeHealthDetails {
    val hasBeenFixed = finding.nodeType == NodeType.FILE_FINDING_FIXED
    val heading = if (hasBeenFixed) "Details" else "Problem"
    val subHeaderText = if (hasBeenFixed)
        UiLabelsBundle.message("issueFixed")
    else
        UiLabelsBundle.message("fileIssue")
    val icon = if (hasBeenFixed) CODE_SMELL_FIXED else AllIcons.Nodes.WarningIntroduction

    return CodeHealthDetails(
        filePath = finding.filePath,
        header = finding.displayName,
        subHeader = createSubHeader(
            file,
            subHeaderText,
            icon,
            CodeHealthDetailsType.FILE
        ),
        body = listOf(Paragraph(finding.tooltip, heading)),
        type = CodeHealthDetailsType.FILE
    )
}

fun getHealthFinding(delta: NativeDelta, finding: CodeHealthFinding, project: Project): CodeHealthDetails? {
    val file = extractUsingRegex(finding.filePath, Regex(".*/([^/]+)\\.([^.]+)$")) { (fileName, extension) ->
        fileName to extension
    }

    return when (finding.nodeType) {
        NodeType.CODE_HEALTH_INCREASE, NodeType.CODE_HEALTH_DECREASE, NodeType.CODE_HEALTH_NEUTRAL ->
            getHealthFinding(file, finding, delta)

        NodeType.FILE_FINDING, NodeType.FILE_FINDING_FIXED -> getFileFinding(file, finding)
        NodeType.FUNCTION_FINDING -> getFunctionFinding(file, finding, delta, project)
        else -> null
    }
}

private fun handleMouseClick(project: Project, codeSmell: CodeSmell, filePath: String) {
    val editorManager = FileEditorManager.getInstance(project)
    val file = LocalFileSystem.getInstance().findFileByPath(filePath)
    val docViewer = CodeSceneDocumentationViewer.getInstance(project)

    file?.let {
        if (!editorManager.isFileOpen(file)) editorManager.openFile(file, true)

        val (line, column) = codeSmell.highlightRange
            ?.let { it.startLine - 1 to it.startColumn - 1 }
            ?: (1 to 1)
        val fileDescriptor = OpenFileDescriptor(project, file, line, column)

        editorManager.openTextEditor(fileDescriptor, true)
        editorManager.selectedTextEditor?.let {
            docViewer.open(
                it,
                DocumentationParams(
                    codeSmell.category,
                    it.virtualFile.name,
                    it.virtualFile.path,
                    codeSmell.highlightRange.startLine,
                    DocsEntryPoint.CODE_HEALTH_DETAILS
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