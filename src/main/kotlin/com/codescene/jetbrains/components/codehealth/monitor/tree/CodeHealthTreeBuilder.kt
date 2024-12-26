package com.codescene.jetbrains.components.codehealth.monitor.tree

import com.codescene.jetbrains.components.codehealth.monitor.tree.listeners.CustomTreeExpansionListener
import com.codescene.jetbrains.components.codehealth.monitor.tree.listeners.TreeMouseMotionAdapter
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.notifier.CodeHealthDetailsRefreshNotifier
import com.codescene.jetbrains.services.CodeNavigationService
import com.codescene.jetbrains.util.getFileFinding
import com.codescene.jetbrains.util.getFunctionFinding
import com.codescene.jetbrains.util.getHealthFinding
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.Tree
import java.awt.Component
import java.awt.Dimension
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath

enum class NodeType {
    ROOT,
    CODE_HEALTH_DECREASE,
    CODE_HEALTH_INCREASE,
    CODE_HEALTH_NEUTRAL,
    FILE_FINDING,
    FILE_FINDING_FIXED,
    FUNCTION_FINDING
}

data class CodeHealthFinding(
    val tooltip: String = "",
    val filePath: String,
    val focusLine: Int? = 1,
    val displayName: String,
    val nodeType: NodeType,
    val additionalText: String = ""
)

class CodeHealthTreeBuilder {
    private lateinit var project: Project
    private val collapsedPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun createTree(
        results: ConcurrentHashMap<String, CodeDelta>,
        project: Project
    ): Tree {
        this.project = project

        val root = DefaultMutableTreeNode()
        results.map { buildNode(it.key, it.value) }.forEach { root.add(it) }

        val tree = Tree(DefaultTreeModel(root)).apply {
            isRootVisible = false
            isFocusable = false
            alignmentX = Component.LEFT_ALIGNMENT
            cellRenderer = CustomTreeCellRenderer()
            minimumSize = Dimension(200, 80)

            addTreeSelectionListener(::handleTreeSelectionEvent)
            addMouseMotionListener(TreeMouseMotionAdapter(this))
            addTreeExpansionListener(CustomTreeExpansionListener(collapsedPaths))
        }

        expandNodes(tree)

        return tree
    }

    /**
     * Nodes are rendered expanded by default, so to preserve the collapsed state
     * between refreshes, we must manually collapse nodes based on the saved state.
     */
    private fun expandNodes(tree: JTree) =
        SwingUtilities.invokeLater {
            val rootNode = tree.model.root as DefaultMutableTreeNode
            val childNodes = (0 until rootNode.childCount).map { rootNode.getChildAt(it) as DefaultMutableTreeNode }

            childNodes.forEach { child ->
                val userObject = child.userObject

                if (userObject is CodeHealthFinding) {
                    val filePath = userObject.filePath
                    val shouldBeExpanded = !collapsedPaths.contains(filePath) && !child.isLeaf
                    if (shouldBeExpanded) tree.expandPath(TreePath(child.path))
                }
            }
        }

    private fun handleTreeSelectionEvent(event: TreeSelectionEvent) {
        val navigationService = CodeNavigationService.getInstance(project)

        val selectedNode = event.path.lastPathComponent as? DefaultMutableTreeNode
        val finding = selectedNode?.userObject as? CodeHealthFinding ?: return

        val notifier = project.messageBus.syncPublisher(CodeHealthDetailsRefreshNotifier.TOPIC)

        if (selectedNode.isLeaf) {
            navigationService.focusOnLine(finding.filePath, finding.focusLine!!)
            notifier.refresh(finding)
        } else {
            (event.source as? JTree)?.clearSelection()
            notifier.refresh(null)
        }
    }

    private fun buildNode(filePath: String, delta: CodeDelta): MutableTreeNode {
        val root = CodeHealthFinding(
            filePath = filePath,
            tooltip = filePath,
            displayName = filePath,
            nodeType = NodeType.ROOT
        )

        return DefaultMutableTreeNode(root).apply {
            addCodeHealthLeaf(filePath, delta)
            addFileLeaves(filePath, delta)
            addFunctionLeaves(filePath, delta)
        }
    }

    private fun DefaultMutableTreeNode.addCodeHealthLeaf(filePath: String, delta: CodeDelta) {
        val health = getHealthFinding(filePath, delta)

        add(DefaultMutableTreeNode(health))
    }

    private fun DefaultMutableTreeNode.addFileLeaves(filePath: String, delta: CodeDelta) =
        delta.fileLevelFindings.forEach {
            val finding = getFileFinding(filePath, it)

            add(DefaultMutableTreeNode(finding))
        }

    private fun DefaultMutableTreeNode.addFunctionLeaves(filePath: String, delta: CodeDelta) =
        delta.functionLevelFindings.forEach { (function, details) ->
            val finding = getFunctionFinding(filePath, function, details)

            add(DefaultMutableTreeNode(finding))
        }
}