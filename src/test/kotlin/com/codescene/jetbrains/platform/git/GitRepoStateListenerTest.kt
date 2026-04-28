package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.git.FileEvent
import com.codescene.jetbrains.core.git.FileEventType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.MessageBusConnection
import git4idea.repo.GitRepository
import git4idea.status.GitStagingAreaHolder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GitRepoStateListenerTest {
    private lateinit var project: Project
    private lateinit var observer: GitChangeObserverAdapter
    private lateinit var listener: GitRepoStateListener
    private lateinit var messageBus: MessageBus
    private lateinit var connection: MessageBusConnection
    private val testDispatcher = StandardTestDispatcher()

    private val workspacePath = "/test/workspace"
    private val gitRootPath = "/test/workspace"

    @Before
    fun setup() {
        project = mockk(relaxed = true)
        observer = mockk(relaxed = true)
        messageBus = mockk(relaxed = true)
        connection = mockk(relaxed = true)

        every { project.messageBus } returns messageBus
        every { messageBus.connect(any<GitRepoStateListener>()) } returns connection

        listener = GitRepoStateListener(project, observer, workspacePath, gitRootPath, testDispatcher)
    }

    @After
    fun teardown() {
        listener.dispose()
    }

    @Test
    fun `start subscribes to GitStagingAreaHolder TOPIC`() {
        listener.start()

        verify(exactly = 1) { messageBus.connect(listener) }
        verify(exactly = 1) { connection.subscribe(GitStagingAreaHolder.TOPIC, any()) }
    }

    @Test
    fun `stagingAreaChanged triggers reconciliation for matching git root`() =
        runTest(testDispatcher) {
            val listenerSlot = slot<GitStagingAreaHolder.StagingAreaListener>()
            every { connection.subscribe(GitStagingAreaHolder.TOPIC, capture(listenerSlot)) } returns Unit

            val repository = mockk<GitRepository>()
            val root = mockk<VirtualFile>()
            every { repository.root } returns root
            every { root.path } returns gitRootPath

            coEvery { observer.getChangedFilesVsBaseline() } returns emptySet()
            coEvery { observer.getTrackedFiles() } returns emptySet()

            listener.start()
            listenerSlot.captured.stagingAreaChanged(repository)

            advanceUntilIdle()

            verify(atLeast = 1) { observer.getTrackedFiles() }
        }

    @Test
    fun `stagingAreaChanged ignores repositories with different git root`() =
        runTest(testDispatcher) {
            val listenerSlot = slot<GitStagingAreaHolder.StagingAreaListener>()
            every { connection.subscribe(GitStagingAreaHolder.TOPIC, capture(listenerSlot)) } returns Unit

            val repository = mockk<GitRepository>()
            val root = mockk<VirtualFile>()
            every { repository.root } returns root
            every { root.path } returns "/different/git/root"

            listener.start()
            listenerSlot.captured.stagingAreaChanged(repository)

            advanceUntilIdle()

            verify(exactly = 0) { observer.getTrackedFiles() }
        }

    @Test
    fun `reconciliation queues DELETE for files no longer in changed list`() =
        runTest(testDispatcher) {
            val listenerSlot = slot<GitStagingAreaHolder.StagingAreaListener>()
            every { connection.subscribe(GitStagingAreaHolder.TOPIC, capture(listenerSlot)) } returns Unit

            val repository = mockk<GitRepository>()
            val root = mockk<VirtualFile>()
            every { repository.root } returns root
            every { root.path } returns gitRootPath

            val trackedFile = "$workspacePath/file.kt"
            coEvery { observer.getTrackedFiles() } returns setOf(trackedFile)
            coEvery { observer.getChangedFilesVsBaseline() } returns emptySet()

            val eventSlot = slot<FileEvent>()
            every { observer.queueEvent(capture(eventSlot)) } returns Unit

            listener.start()
            listenerSlot.captured.stagingAreaChanged(repository)

            advanceUntilIdle()

            assertEquals(FileEventType.DELETE, eventSlot.captured.type)
            assertEquals(trackedFile, eventSlot.captured.path)
        }

    @Test
    fun `reconciliation does not queue DELETE for files still in changed list`() =
        runTest(testDispatcher) {
            val listenerSlot = slot<GitStagingAreaHolder.StagingAreaListener>()
            every { connection.subscribe(GitStagingAreaHolder.TOPIC, capture(listenerSlot)) } returns Unit

            val repository = mockk<GitRepository>()
            val root = mockk<VirtualFile>()
            every { repository.root } returns root
            every { root.path } returns gitRootPath

            val trackedFile = "$workspacePath/file.kt"
            coEvery { observer.getTrackedFiles() } returns setOf(trackedFile)
            coEvery { observer.getChangedFilesVsBaseline() } returns setOf("file.kt")

            listener.start()
            listenerSlot.captured.stagingAreaChanged(repository)

            advanceUntilIdle()

            verify(exactly = 0) { observer.queueEvent(any()) }
        }

    @Test
    fun `dispose disconnects MessageBusConnection`() {
        listener.start()
        listener.dispose()

        verify(exactly = 1) { connection.disconnect() }
    }

    @Test
    fun `debouncing cancels previous reconciliation job`() =
        runTest(testDispatcher) {
            val listenerSlot = slot<GitStagingAreaHolder.StagingAreaListener>()
            every { connection.subscribe(GitStagingAreaHolder.TOPIC, capture(listenerSlot)) } returns Unit

            val repository = mockk<GitRepository>()
            val root = mockk<VirtualFile>()
            every { repository.root } returns root
            every { root.path } returns gitRootPath

            coEvery { observer.getTrackedFiles() } returns emptySet()
            coEvery { observer.getChangedFilesVsBaseline() } returns emptySet()

            listener.start()

            listenerSlot.captured.stagingAreaChanged(repository)
            listenerSlot.captured.stagingAreaChanged(repository)
            listenerSlot.captured.stagingAreaChanged(repository)

            advanceUntilIdle()

            verify(exactly = 1) { observer.getTrackedFiles() }
        }
}
