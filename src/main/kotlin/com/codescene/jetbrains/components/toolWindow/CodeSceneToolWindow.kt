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
import javax.swing.tree.TreeNode

enum class NodeType {
    ROOT,
    CODE_HEALTH,
    FILE_FINDING,
    FUNCTION_FINDING
}

//TODO: remove
val parsedTreeComponents = CodeDelta(
    fileLevelFindings = listOf(
        ChangeDetails(
            category = "Lines of Code in a Single File",
            description = "This module has 2942 lines of code, improve code health by reducing it to 1000",
            changeType = ChangeType.INTRODUCED,
            position = Position(line = 1, column = 1)
        ),
        ChangeDetails(
            category = "Overall Code Complexity",
            description = "This module has a mean cyclomatic complexity of 8.56 across 64 functions. The mean complexity threshold is 4",
            changeType = ChangeType.INTRODUCED,
            position = Position(line = 1, column = 1)
        )
    ),
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
    val focusLine: Int? = 1,
    val displayName: String,
    val nodeType: NodeType,
    val additionalText: String = ""
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
        val (change, percentage) = getCodeHealth(healthDetails)

        val health = CodeHealthFinding(
            tooltip = "The Code health for this file is declining. Explore the functions below for more details.", //TODO: localize
            filePath,
            displayName = "Code Health: $change",
            additionalText = percentage,
            nodeType = NodeType.CODE_HEALTH
        )

        add(DefaultMutableTreeNode(health))
    }

    private fun DefaultMutableTreeNode.addFileLeaf(filePath: String, delta: CodeDelta) =
        delta.fileLevelFindings.forEach {
            val finding = CodeHealthFinding(
                tooltip = it.description,
                filePath,
                displayName = it.category,
                nodeType = NodeType.FILE_FINDING
            )

            add(DefaultMutableTreeNode(finding))
        }

    private fun DefaultMutableTreeNode.addFunctionLeaf(filePath: String, delta: CodeDelta) =
        delta.functionLevelFindings.forEach { (function, details) ->
            val finding = CodeHealthFinding(
                tooltip = getFunctionDeltaTooltip(function, details),
                filePath,
                focusLine = details[0].position.line,
                displayName = function.name,
                nodeType = NodeType.FUNCTION_FINDING
            )

            add(DefaultMutableTreeNode(finding))
        }

    private fun buildTree(filePath: String, delta: CodeDelta): TreeNode {
        val root = CodeHealthFinding(tooltip = filePath, filePath, displayName = filePath, nodeType = NodeType.ROOT) //TODO: change tooltip logic

        return DefaultMutableTreeNode(root).apply {
            addCodeHealthLeaf(filePath, delta)
            addFileLeaf(filePath, delta)
            addFunctionLeaf(filePath, delta)
        }
    }

    private fun handleTreeSelectionEvent(event: TreeSelectionEvent) {
        val navigationService = CodeNavigationService.getInstance(project)

        (event.source as? JTree)?.clearSelection()

        val selectedNode = event.path.lastPathComponent as? DefaultMutableTreeNode

        (selectedNode?.takeIf { it.isLeaf }?.userObject as? CodeHealthFinding)?.also { finding ->
            scope.launch {
                navigationService.focusOnLine(finding.filePath, finding.focusLine!!)
            }
        }
    }
}