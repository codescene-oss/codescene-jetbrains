package com.codescene.jetbrains.core.mapper

import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.view.DocsData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentationMapperTest {
    private val mapper = DocumentationMapper()

    @Test
    fun `toCwfData returns DOCS view`() {
        val docsData = DocsData(docType = "type", fileData = FileMetaType(fileName = "a.kt"))
        val result = mapper.toCwfData(docsData, devmode = false)
        assertEquals(View.DOCS.value, result.view)
    }

    @Test
    fun `toCwfData passes docsData through as data`() {
        val docsData = DocsData(docType = "docs_general_code_health", fileData = FileMetaType(fileName = "a.kt"))
        val result = mapper.toCwfData(docsData, devmode = false)
        assertEquals(docsData, result.data)
    }

    @Test
    fun `toCwfData sets pro and devmode flags`() {
        val docsData = DocsData(docType = "type", fileData = FileMetaType(fileName = ""))
        val result = mapper.toCwfData(docsData, pro = true, devmode = true)
        assertTrue(result.pro)
        assertTrue(result.devmode)
    }
}
