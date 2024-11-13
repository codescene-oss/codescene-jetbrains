package com.codescene.jetbrains.components.toolWindow

import com.codescene.jetbrains.components.tree.CustomTreeCellRenderer
import com.codescene.jetbrains.data.*
import com.codescene.jetbrains.data.Function
import com.codescene.jetbrains.services.CodeNavigationService
import com.codescene.jetbrains.util.getCodeHealth
import com.codescene.jetbrains.util.getFunctionDeltaTooltip
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.treeStructure.Tree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
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
    val tooltip: String,
    val filePath: String,
    val focusLine: Int = 1,
    val displayName: String
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
        val tree = buildTree(filePath, delta)

        return Tree(DefaultTreeModel(tree)).apply {
            isFocusable = false
            cellRenderer = CustomTreeCellRenderer()
            minimumSize = Dimension(200, 80)

            addTreeSelectionListener(::handleTreeSelectionEvent)
        }
    }

    private fun DefaultMutableTreeNode.addCodeHealthLeaf(filePath: String, delta: CodeDelta) {
        val healthDetails = HealthDetails(delta.oldScore, delta.newScore)
        val healthInformation = getCodeHealth(healthDetails)

        val health = CodeHealthFinding(
            tooltip = "The Code health for this file is declining. Explore the functions below for more details.",
            filePath,
            displayName = healthInformation
        )

        add(DefaultMutableTreeNode(health))
    }

    private fun DefaultMutableTreeNode.addFunctionLeaf(filePath: String, delta: CodeDelta) =
        delta.functionLevelFindings.forEach { (function, details) ->
            val finding = CodeHealthFinding(
                tooltip = getFunctionDeltaTooltip(function, details),
                filePath,
                focusLine = details[0].position.line,
                displayName = function.name
            )

            add(DefaultMutableTreeNode(finding))
        }

    private fun buildTree(filePath: String, delta: CodeDelta) = DefaultMutableTreeNode(filePath).apply {
        addCodeHealthLeaf(filePath, delta)
        addFunctionLeaf(filePath, delta)
    }

    private fun handleTreeSelectionEvent(event: TreeSelectionEvent) {
        val navigationService = CodeNavigationService.getInstance(project)

        (event.source as? JTree)?.clearSelection()

        val selectedNode = event.path.lastPathComponent as? DefaultMutableTreeNode

        (selectedNode?.takeIf { it.isLeaf }?.userObject as? CodeHealthFinding)?.also { finding ->
            scope.launch {
                navigationService.focusOnLine(finding.filePath, finding.focusLine)
            }
        }
    }
}