package com.codescene.jetbrains.core.mapper

import com.codescene.data.delta.ChangeDetail as DeltaChangeDetail
import com.codescene.data.delta.Delta
import com.codescene.data.delta.FunctionFinding as DeltaFunctionFinding
import com.codescene.jetbrains.core.delta.DeltaCacheItem
import com.codescene.jetbrains.core.models.CwfMessage
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig
import com.codescene.jetbrains.core.models.view.FunctionToRefactor
import com.codescene.jetbrains.core.util.Constants.DELTA_ANALYSIS_JOB
import com.codescene.jetbrains.core.util.Constants.JOB_STATE_RUNNING
import io.mockk.every
import io.mockk.mockk
import java.util.Optional
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeHealthMonitorMapperTest {
    private val mapper = CodeHealthMonitorMapper()
    private val autoRefactorConfig = AutoRefactorConfig()
    private val noRefactorResolver: (String, String, DeltaFunctionFinding) -> FunctionToRefactor? = { _, _, _ -> null }
    private val emptyDeltaResults = emptyList<Pair<String, DeltaCacheItem>>()

    private fun createDelta(
        scoreChange: Double? = 0.0,
        newScore: Double? = null,
        oldScore: Double? = null,
        fileLevelFindings: List<DeltaChangeDetail>? = null,
        functionLevelFindings: List<DeltaFunctionFinding>? = null,
    ): Delta {
        val delta = mockk<Delta>(relaxed = true)
        every { delta.scoreChange } returns scoreChange
        every { delta.newScore } returns Optional.ofNullable(newScore)
        every { delta.oldScore } returns Optional.ofNullable(oldScore)
        every { delta.fileLevelFindings } returns fileLevelFindings
        every { delta.functionLevelFindings } returns functionLevelFindings
        return delta
    }

    private fun createCacheItem(delta: Delta) =
        DeltaCacheItem(headHash = "head", currentHash = "current", deltaApiResponse = delta)

    private fun toCwfData(
        deltaResults: List<Pair<String, DeltaCacheItem>> = emptyDeltaResults,
        activeJobs: List<String> = emptyList(),
        pro: Boolean = true,
        devmode: Boolean = false,
    ) = mapper.toCwfData(
        deltaResults,
        activeJobs,
        noRefactorResolver,
        autoRefactorConfig,
        pro,
        devmode,
    )

    private fun singleFileDelta(delta: Delta) =
        toCwfData(deltaResults = listOf("file.kt" to createCacheItem(delta))).data!!.fileDeltaData[0]

    @Test
    fun `toCwfData returns HOME view`() {
        val result = toCwfData()
        assertEquals(View.HOME.value, result.view)
    }

    @Test
    fun `toCwfData sets pro flag`() {
        val result = toCwfData(pro = true)
        assertTrue(result.pro)
    }

    @Test
    fun `toCwfData sets devmode flag`() {
        val result = toCwfData(devmode = true)
        assertTrue(result.devmode)
    }

    @Test
    fun `toCwfData sets signedIn to true`() {
        val result = toCwfData()
        assertTrue(result.data!!.signedIn)
    }

    @Test
    fun `toCwfData maps active jobs`() {
        val result = toCwfData(activeJobs = listOf("a.kt", "b.kt"))
        val jobs = result.data!!.jobs
        assertEquals(2, jobs.size)
        assertEquals(DELTA_ANALYSIS_JOB, jobs[0].type)
        assertEquals(JOB_STATE_RUNNING, jobs[0].state)
        assertEquals("a.kt", jobs[0].file.fileName)
        assertEquals("b.kt", jobs[1].file.fileName)
    }

    @Test
    fun `toCwfData maps empty delta results`() {
        val result = toCwfData()
        assertTrue(result.data!!.fileDeltaData.isEmpty())
    }

    @Test
    fun `toCwfData maps delta results with scores`() {
        val delta = createDelta(scoreChange = 1.5, newScore = 8.0, oldScore = 6.5)
        val fileDelta = singleFileDelta(delta)
        assertEquals("file.kt", fileDelta.file.fileName)
        assertEquals(1.5, fileDelta.delta.scoreChange, 0.001)
        assertEquals(8.0, fileDelta.delta.newScore!!, 0.001)
        assertEquals(6.5, fileDelta.delta.oldScore!!, 0.001)
    }

    @Test
    fun `toCwfData maps null scores`() {
        val delta = createDelta(scoreChange = null, newScore = null, oldScore = null)
        val fileDelta = singleFileDelta(delta)
        assertEquals(0.0, fileDelta.delta.scoreChange, 0.001)
        assertEquals(null, fileDelta.delta.newScore)
        assertEquals(null, fileDelta.delta.oldScore)
    }

    @Test
    fun `toCwfData maps file level findings`() {
        val changeDetail = mockk<DeltaChangeDetail>(relaxed = true)
        every { changeDetail.category } returns "Complex Method"
        every { changeDetail.description } returns "desc"
        every { changeDetail.changeType } returns mockk { every { value() } returns "introduced" }
        every { changeDetail.line } returns Optional.of(10)

        val delta = createDelta(fileLevelFindings = listOf(changeDetail))
        val findings = singleFileDelta(delta).delta.fileLevelFindings
        assertEquals(1, findings.size)
        assertEquals("Complex Method", findings[0].category)
        assertEquals("desc", findings[0].description)
        assertEquals("introduced", findings[0].changeType)
        assertEquals(10, findings[0].line)
    }

    @Test
    fun `toCwfData maps file level finding with absent line to 0`() {
        val changeDetail = mockk<DeltaChangeDetail>(relaxed = true)
        every { changeDetail.category } returns "Cat"
        every { changeDetail.description } returns "desc"
        every { changeDetail.changeType } returns mockk { every { value() } returns "fixed" }
        every { changeDetail.line } returns Optional.empty()

        val delta = createDelta(fileLevelFindings = listOf(changeDetail))
        assertEquals(0, singleFileDelta(delta).delta.fileLevelFindings[0].line)
    }

    @Test
    fun `toCwfData maps null file level findings to empty list`() {
        val delta = createDelta(fileLevelFindings = null)
        assertTrue(singleFileDelta(delta).delta.fileLevelFindings.isEmpty())
    }

    @Test
    fun `toCwfData maps function level findings with range`() {
        val range = mockk<com.codescene.data.delta.Range>(relaxed = true)
        every { range.startLine } returns 1
        every { range.startColumn } returns 2
        every { range.endLine } returns 10
        every { range.endColumn } returns 20

        val function = mockk<com.codescene.data.delta.Function>(relaxed = true)
        every { function.name } returns "myFn"
        every { function.range } returns Optional.of(range)

        val fnFinding = mockk<DeltaFunctionFinding>(relaxed = true)
        every { fnFinding.function } returns function
        every { fnFinding.changeDetails } returns emptyList()

        val delta = createDelta(functionLevelFindings = listOf(fnFinding))
        val fnFindings = singleFileDelta(delta).delta.functionLevelFindings
        assertEquals(1, fnFindings.size)
        assertEquals("myFn", fnFindings[0].function.name)
        assertEquals(1, fnFindings[0].function.range!!.startLine)
        assertEquals(10, fnFindings[0].function.range!!.endLine)
    }

    @Test
    fun `toCwfData maps null function level findings to empty list`() {
        val delta = createDelta(functionLevelFindings = null)
        assertTrue(singleFileDelta(delta).delta.functionLevelFindings.isEmpty())
    }

    @Test
    fun `toCwfData passes autoRefactorConfig through`() {
        val config = AutoRefactorConfig(activated = true, visible = true, disabled = false)
        val result = mapper.toCwfData(emptyDeltaResults, emptyList(), noRefactorResolver, config, devmode = false)
        assertEquals(config, result.data!!.autoRefactor)
    }

    @Test
    fun `toMessage encodes home payload as CWF message`() {
        val result =
            mapper.toMessage(
                deltaResults = emptyList(),
                activeJobs = listOf("src/Main.kt"),
                functionToRefactorResolver = noRefactorResolver,
                autoRefactorConfig = autoRefactorConfig,
                devmode = true,
            )

        val parsed = Json.decodeFromString(CwfMessage.serializer(), result)
        assertEquals("update-renderer", parsed.messageType)
        assertTrue(result.contains("\"view\": \"home\""))
        assertTrue(result.contains("\"src/Main.kt\""))
    }

    @Test
    fun `buildUpdate returns message and notification state`() {
        val delta = createDelta(scoreChange = 1.0)
        val result =
            mapper.buildUpdate(
                deltaResults = listOf("file.kt" to createCacheItem(delta)),
                activeJobs = listOf("src/Main.kt"),
                functionToRefactorResolver = noRefactorResolver,
                autoRefactorConfig = autoRefactorConfig,
                devmode = true,
            )

        assertTrue(result.message.contains("\"view\": \"home\""))
        assertTrue(result.message.contains("\"src/Main.kt\""))
        assertEquals(true, result.hasNotification)
    }

    @Test
    fun `hasNotification returns true when delta results exist`() {
        val delta = createDelta(scoreChange = 1.0)
        val deltaResults = listOf("file.kt" to createCacheItem(delta))

        assertEquals(true, mapper.hasNotification(deltaResults))
    }

    @Test
    fun `hasNotification returns false when delta results are empty`() {
        assertEquals(false, mapper.hasNotification(emptyList()))
    }
}
