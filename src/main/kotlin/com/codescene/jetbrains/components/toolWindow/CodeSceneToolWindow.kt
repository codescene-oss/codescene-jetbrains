package com.codescene.jetbrains.components.toolWindow

import com.codescene.jetbrains.components.tree.CustomTreeCellRenderer
import com.codescene.jetbrains.data.*
import com.codescene.jetbrains.data.Function
import com.codescene.jetbrains.util.getCodeHealth
import com.codescene.jetbrains.util.getFunctionDeltaTooltip
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.JBPanel
import com.intellij.ui.treeStructure.Tree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.BoxLayout
import javax.swing.JTree
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

//TODO: remove
val parsedTreeComponents = CodeDelta(
    fileLevelFindings = emptyList(),
    functionLevelFindings = listOf(
        FunctionFinding(
            function = Function(
                name = "foo",
                range = HighlightRange(startLine = 1, startColumn = 1, endLine = 3, endColumn = 2)
            ),
            changeDetails = listOf(
                ChangeDetails(
                    category = "Excess Number of Function Arguments",
                    description = "foo has 6 arguments, threshold = 4",
                    changeType = ChangeType.INTRODUCED,
                    position = Position(line = 1, column = 1)
                )
            )
        ),
        FunctionFinding(
            function = Function(
                name = "bar",
                range = HighlightRange(startLine = 5, startColumn = 1, endLine = 7, endColumn = 2)
            ),
            changeDetails = listOf(
                ChangeDetails(
                    category = "Excess Number of Function Arguments",
                    description = "bar has 6 arguments, threshold = 4",
                    changeType = ChangeType.INTRODUCED,
                    position = Position(line = 5, column = 1)
                )
            )
        )
    ),
    oldScore = 10.0,
    newScore = 9.6882083290695
)

data class HealthDetails(
    val oldScore: Double,
    val newScore: Double,
)

val deltaAnalyses: List<Pair<String, CodeDelta>> = listOf(("src/main/kotlin/Test.js" to parsedTreeComponents))

data class CodeHealthFinding(
    val displayName: String,
    val filePath: String,
    val tooltip: String
)

//TODO: Refactor, make disposable?
class CodeSceneToolWindow {
    private lateinit var project: Project
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getContent(project: Project): JBPanel<JBPanel<*>> {
        this.project = project

        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            deltaAnalyses.forEach { (name, delta) ->
                val fileTreePanel = createFileTree(name, delta)

                fileTreePanel.alignmentX = Component.LEFT_ALIGNMENT

                add(fileTreePanel)
            }
        }
    }

    private fun createFileTree(
        filePath: String,
        delta: CodeDelta
    ): Tree {
        val healthDetails = HealthDetails(delta.oldScore, delta.newScore)
        val healthInformation = getCodeHealth(healthDetails)
        val health = CodeHealthFinding(
            displayName = healthInformation,
            filePath,
            tooltip = "The Code health for this file is declining. Explore the functions below for more details."
        )

        val root = DefaultMutableTreeNode(filePath).apply {
            add(DefaultMutableTreeNode(health))

            delta.functionLevelFindings.forEach { (function, details) ->
                val finding = CodeHealthFinding(
                    displayName = function.name,
                    filePath,
                    tooltip = getFunctionDeltaTooltip(function, details)
                )

                add(DefaultMutableTreeNode(finding))
            }
        }

        val tree = Tree(DefaultTreeModel(root)).apply {
            cellRenderer = CustomTreeCellRenderer()
            isFocusable = false
            minimumSize = Dimension(200, 80)

            addTreeSelectionListener(::handleTreeSelectionEvent)
        }

        return tree
    }

    private fun handleTreeSelectionEvent(event: TreeSelectionEvent) {
        (event.source as? JTree)?.clearSelection()

        val selectedNode = event.path.lastPathComponent as? DefaultMutableTreeNode

        (selectedNode?.takeIf { it.isLeaf }?.userObject as? CodeHealthFinding)?.also { finding ->
            scope.launch {
                focusOnLine(finding.filePath, 5)
            }
        }
    }

    private suspend fun openEditorAndMoveCaret(file: VirtualFile, line: Int) = withContext(Dispatchers.Main) {
        val openFileDescriptor = OpenFileDescriptor(project, file, line - 1, 0)
        val editor = FileEditorManager.getInstance(project)
            .openTextEditor(openFileDescriptor, true)

        if (editor != null) {
            moveCaret(editor, line)
        }
    }

    private fun moveCaret(editor: Editor, line: Int) {
        val caretModel = editor.caretModel

        WriteCommandAction.runWriteCommandAction(project) {
            val position = LogicalPosition(line - 1, 0)

            caretModel.moveToLogicalPosition(position)
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        }
    }

    private fun getFileByName(filePath: String): VirtualFile {
        val fileName = File(filePath).name

        val file = ReadAction.compute<VirtualFile?, Throwable> {
            FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.projectScope(project))
                .firstOrNull { it.path.endsWith(filePath) } //TODO: check if I need this
        }

        return file
    }

    private suspend fun focusOnLine(filePath: String, line: Int) {
        val file = getFileByName(filePath)

        openEditorAndMoveCaret(file, line)
    }
}