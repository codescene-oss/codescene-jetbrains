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
    private lateinit var notifier: CodeHealthDetailsRefreshNotifier

    private var suppressFocusOnLine: Boolean = false
    private var selectedNode: CodeHealthFinding? = null
    private val collapsedPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun createTree(
        results: ConcurrentHashMap<String, CodeDelta>,
        project: Project
    ): Tree {
        this.project = project
        this.notifier = project.messageBus.syncPublisher(CodeHealthDetailsRefreshNotifier.TOPIC)

        val root = DefaultMutableTreeNode()
        results.map { buildNode(it.key, it.value) }.forEach { root.add(it) }

        return getTree(root)
    }

    private fun getTree(root: DefaultMutableTreeNode): Tree {
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
        if (selectedNode != null) selectNode(tree)

        return tree
    }

    /**
     * Nodes are rendered expanded by default, so to preserve the collapsed state
     * between refreshes, we must manually collapse nodes based on the saved state.
     */
    private fun expandNodes(tree: JTree) =
        SwingUtilities.invokeLater {
            val rootNode = tree.model.root as DefaultMutableTreeNode
            val parentNodes = (0 until rootNode.childCount).map { rootNode.getChildAt(it) as DefaultMutableTreeNode }

            parentNodes.forEach {
                val userObject = it.userObject

                if (userObject is CodeHealthFinding) {
                    val filePath = userObject.filePath
                    val shouldBeExpanded = !collapsedPaths.contains(filePath) && !it.isLeaf
                    if (shouldBeExpanded) tree.expandPath(TreePath(it.path))
                }
            }
        }

    /**
     * The tree is rendered between each review. To preserve the selected node state
     * between refreshes, we must manually select the node based on the saved state.
     */
    private fun selectNode(tree: JTree) =
        SwingUtilities.invokeLater {
            val root = tree.model.root as DefaultMutableTreeNode
            val parents = (0 until root.childCount).map { root.getChildAt(it) as DefaultMutableTreeNode }
            val selectedNodeParent =
                parents.find { (it.userObject as CodeHealthFinding).filePath == selectedNode!!.filePath }

            if (selectedNodeParent != null) {
                val found = getSelectedNode(selectedNodeParent)

                if (found != null) {
                    suppressFocusOnLine = true

                    tree.selectionModel.selectionPath = TreePath(found.path)

                    suppressFocusOnLine = false
                } else {
                    selectedNode = null
                    notifier.refresh(null)
                }
            }
        }

    private fun getSelectedNode(node: DefaultMutableTreeNode): DefaultMutableTreeNode? {
        val type = selectedNode!!.nodeType
        val isHealthNode =
            type == NodeType.CODE_HEALTH_NEUTRAL || type == NodeType.CODE_HEALTH_DECREASE || type == NodeType.CODE_HEALTH_INCREASE

        if (isHealthNode) {
            return node.getChildAt(0) as DefaultMutableTreeNode
        }

        (1 until node.childCount).map {
            val child = node.getChildAt(it) as DefaultMutableTreeNode
            val finding = child.userObject as CodeHealthFinding

            if (selectedNode!!.displayName == finding.displayName) {
                return child
            }
        }

        return null
    }

    private fun handleTreeSelectionEvent(event: TreeSelectionEvent) {
        val navigationService = CodeNavigationService.getInstance(project)

        val targetNode = event.path.lastPathComponent as? DefaultMutableTreeNode
        val finding = targetNode?.userObject as? CodeHealthFinding ?: return

        if (targetNode.isLeaf) {
            if (!suppressFocusOnLine) navigationService.focusOnLine(finding.filePath, finding.focusLine!!)

            notifier.refresh(finding)
            selectedNode = targetNode.userObject as CodeHealthFinding
        } else {
            (event.source as? JTree)?.clearSelection()

            notifier.refresh(null)
            selectedNode = null
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