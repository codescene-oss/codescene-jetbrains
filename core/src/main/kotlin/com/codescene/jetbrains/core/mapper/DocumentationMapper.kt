package com.codescene.jetbrains.core.mapper

import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.view.DocsData

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
}
