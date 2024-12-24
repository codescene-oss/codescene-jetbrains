package com.codescene.jetbrains.util

import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_DECREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_INCREASE
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH_NEUTRAL
import com.codescene.jetbrains.CodeSceneIcons.CODE_SMELL_FOUND
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.components.codehealth.monitor.tree.NodeType
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.util.Constants.GREEN
import com.codescene.jetbrains.util.Constants.ORANGE
import com.codescene.jetbrains.util.Constants.RED
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.JBColor
import java.util.*
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
    val icon: Icon? = null
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
    val header: String,
    val subHeader: SubHeader,
    val body: List<Paragraph>,
    val type: CodeHealthDetailsType,
    val healthData: HealthData? = null
)

fun resolveHealthBadge(score: Double): Pair<String, JBColor> =
    if (score < 4.0) "Unhealthy" to RED
    else if (score >= 4.0 && score < 9.0) "Problematic" to ORANGE
    else "Healthy" to GREEN

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

//TODO: check neutral status
private fun resolveStatus(delta: CodeDelta, type: NodeType, percentage: String) =
    if (type == NodeType.CODE_HEALTH_NEUTRAL) {
        ""
    } else if (type == NodeType.CODE_HEALTH_INCREASE) {
        "Increased to ${round(delta.newScore)} $percentage"
    } else {
        "Declined from ${round(delta.oldScore)} $percentage"
    }

data class CodeHealthHeader(
    val subText: String,
    val text: String,
    val icon: Icon
)

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
    delta: CodeDelta
): CodeHealthDetails {
    val healthHeader = resolveCodeHealthHeader(finding.nodeType, delta.newScore, delta.oldScore)

    return CodeHealthDetails(
        header = "Code Health Score",
        subHeader = createSubHeader(file, healthHeader!!.subText, healthHeader.icon, CodeHealthDetailsType.HEALTH),
        healthData = HealthData(
            healthHeader.subText, resolveStatus(delta, finding.nodeType, percentage = finding.additionalText), round(delta.newScore)
        ),
        body = listOf(
            Paragraph(
                body = "This score indicates that your development speed could be about 40% slower and you might experience 25% more defects compared to a perfect Code Health score of 10. Improving this score can lead to faster progress and fewer bugs.",
                heading = "Why this is important",
                icon = null
            )
        ),
        type = CodeHealthDetailsType.HEALTH
    )
}

private fun getFunctionFindingBody(delta: CodeDelta, finding: CodeHealthFinding) =
    delta.functionLevelFindings.firstOrNull { it.function.name == finding.displayName }?.changeDetails?.map { it ->
        val changeType =
            it.changeType.name.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercaseChar() }
        val body =
            "${it.description.replace(finding.displayName, "<code>${finding.displayName}</code>")}"

        Paragraph(
            body = body,
            heading = "$changeType: ${it.category}",
            icon = CODE_SMELL_FOUND
        )
    } ?: listOf()

private fun getFunctionFinding(
    file: Pair<String, String>?,
    finding: CodeHealthFinding,
    delta: CodeDelta
): CodeHealthDetails = CodeHealthDetails(
    header = finding.displayName,
    subHeader = createSubHeader(
        file,
        "Multiple Code Smells",
        AllIcons.General.Warning,
        CodeHealthDetailsType.FUNCTION
    ),
    body = getFunctionFindingBody(delta, finding),
    type = CodeHealthDetailsType.FUNCTION
)

private fun getFileFinding(
    file: Pair<String, String>?,
    finding: CodeHealthFinding
): CodeHealthDetails {
    val fileType = FileTypeManager.getInstance().getFileTypeByExtension(file!!.second)

    return CodeHealthDetails(
        header = finding.displayName,
        subHeader = createSubHeader(file, "Multiple Code Smells", fileType.icon, CodeHealthDetailsType.FILE),
        body = listOf(Paragraph(finding.tooltip, "Problem")),
        type = CodeHealthDetailsType.FILE
    )
}

fun getHealthFinding(delta: CodeDelta, finding: CodeHealthFinding): CodeHealthDetails? {
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