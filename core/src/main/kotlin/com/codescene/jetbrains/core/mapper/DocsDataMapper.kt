package com.codescene.jetbrains.core.mapper

import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import com.codescene.jetbrains.core.models.view.DocsData

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
