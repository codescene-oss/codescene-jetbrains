package com.codescene.jetbrains.core.mapper

import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.view.DocsData
import com.codescene.jetbrains.core.util.parseMessage

class DocumentationMapper {
    fun toCwfData(
        docsData: DocsData,
        pro: Boolean = true,
        devmode: Boolean,
    ): CwfData<DocsData> =
        CwfData(
            pro = pro,
            devmode = devmode,
            view = View.DOCS.value,
            data = docsData,
        )

    fun toMessage(
        docsData: DocsData,
        pro: Boolean = true,
        devmode: Boolean,
    ): String =
        parseMessage(
            mapper = {
                toCwfData(
                    docsData = docsData,
                    pro = pro,
                    devmode = devmode,
                )
            },
            serializer = CwfData.serializer(DocsData.serializer()),
        )
}
