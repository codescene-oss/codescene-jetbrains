package com.codescene.jetbrains.core.mapper

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.shared.Fn
import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import com.codescene.jetbrains.core.models.view.AceAcknowledgeData

class AceAcknowledgementMapper {
    fun toCwfData(
        filePath: String,
        fnToRefactor: FnToRefactor,
        autoRefactorConfig: AutoRefactorConfig,
        pro: Boolean = false,
        devmode: Boolean,
    ): CwfData<AceAcknowledgeData> =
        CwfData(
            pro = pro,
            devmode = devmode,
            view = View.ACE_ACKNOWLEDGE.value,
            data =
                AceAcknowledgeData(
                    fileData =
                        FileMetaType(
                            fn =
                                Fn(
                                    name = fnToRefactor.name,
                                    range =
                                        RangeCamelCase(
                                            endLine = fnToRefactor.range.endLine,
                                            endColumn = fnToRefactor.range.endColumn,
                                            startLine = fnToRefactor.range.startLine,
                                            startColumn = fnToRefactor.range.startColumn,
                                        ),
                                ),
                            fileName = filePath,
                        ),
                    autoRefactor = autoRefactorConfig,
                ),
        )
}
