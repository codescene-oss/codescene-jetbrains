package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GitBranchChangeListener(
    private val project: Project,
    private val periodicChangeLister: PeriodicChangeListerService,
    private val observer: GitChangeObserverAdapter?,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Disposable {
    private var connection: MessageBusConnection? = null
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    fun start() {
        Log.info("Starting branch change listener", "GitBranchChangeListener")
        connection?.disconnect()
        connection = project.messageBus.connect(this)
        connection?.subscribe(
            GitRepository.GIT_REPO_CHANGE,
            GitRepositoryChangeListener { repository ->
                Log.info("Repository changed branch=${repository.currentBranchName}", "GitBranchChangeListener")
                periodicChangeLister.markDirty()
                periodicChangeLister.markWorkspaceFileActivity()
                scope.launch {
                    observer?.repopulateFromRepoState()
                }
            },
        )
    }

    override fun dispose() {
        connection?.disconnect()
        connection = null
        scope.cancel()
    }
}
