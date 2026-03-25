package com.codescene.jetbrains.core.mapper

import com.codescene.jetbrains.core.models.CodeVisionCodeSmell
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import com.codescene.jetbrains.core.models.view.DocsData
import com.codescene.jetbrains.core.util.nameDocMap

data class DocsCodeSmellInput(
    val category: String,
    val functionName: String?,
    val startLine: Int,
    val endLine: Int,
    val startColumn: Int,
    val endColumn: Int,
)

fun toCodeSmellDocsData(
    filePath: String,
    docType: String,
    codeSmell: DocsCodeSmellInput,
): DocsData =
    DocsData(
        docType = docType,
        fileData =
            FileMetaType(
                fileName = filePath,
                fn =
                    Fn(
                        name = codeSmell.functionName ?: "",
                        range =
                            RangeCamelCase(
                                endLine = codeSmell.endLine,
                                startLine = codeSmell.startLine,
                                endColumn = codeSmell.endColumn,
                                startColumn = codeSmell.startColumn,
                            ),
                    ),
            ),
    )

fun toGeneralDocsData(docType: String): DocsData =
    DocsData(
        docType = docType,
        fileData = FileMetaType(fileName = ""),
    )

fun resolveGeneralDocsData(source: String): DocsData? {
    val docType = nameDocMap[source] ?: return null
    return toGeneralDocsData(docType)
}

fun resolveCodeSmellDocsData(
    filePath: String,
    codeSmell: CodeVisionCodeSmell,
): DocsData? {
    val docType = nameDocMap[codeSmell.category] ?: return null
    return toCodeSmellDocsData(
        filePath = filePath,
        docType = docType,
        codeSmell =
            DocsCodeSmellInput(
                category = codeSmell.category,
                functionName = codeSmell.functionName,
                startLine = codeSmell.highlightRange.startLine,
                endLine = codeSmell.highlightRange.endLine,
                startColumn = codeSmell.highlightRange.startColumn,
                endColumn = codeSmell.highlightRange.endColumn,
            ),
    )
}
