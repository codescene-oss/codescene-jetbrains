package com.codescene.jetbrains.components.codehealth.monitor.tree

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.components.codehealth.monitor.tree.listeners.CustomTreeExpansionListener
import com.codescene.jetbrains.components.codehealth.monitor.tree.listeners.TreeMouseMotionAdapter
import com.codescene.jetbrains.notifier.CodeHealthDetailsRefreshNotifier
import com.codescene.jetbrains.services.CodeNavigationService
import com.codescene.jetbrains.services.telemetry.TelemetryService
import com.codescene.jetbrains.util.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
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
    val additionalText: String = "",
    val functionFindingIssues: Int = 1,
    val numberOfImprovableFunctions: Int = 0
)

@Service(Service.Level.PROJECT)
class CodeHealthTreeBuilder(private val project: Project) {
    private var suppressFocusOnLine: Boolean = false
    private var selectedNode: CodeHealthFinding? = null
    private val collapsedPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val service = "Health Tree Builder - ${project.name}"
    private var codeHealthSelected = false

    companion object {
        fun getInstance(project: Project): CodeHealthTreeBuilder = project.service<CodeHealthTreeBuilder>()
    }

    fun createTree(
        results: List<Map.Entry<String, Delta>>,
        shouldCollapseTree: Boolean
    ): Tree {
        val root = DefaultMutableTreeNode()
        results.map { buildNode(it.key, it.value) }.forEach { root.add(it) }

        if (shouldCollapseTree) results.forEach { collapsedPaths.add(it.key) }

        return getTree(root, shouldCollapseTree)
    }

    private fun getTree(root: DefaultMutableTreeNode, shouldCollapseTree: Boolean): Tree {
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

        if (!shouldCollapseTree) expandNodes(tree) else {
            deselectNodeAndCodeHealthFinding()
            TreeUtil.collapseAll(tree, -1)
        }

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
            val parent = getParentNode(root, selectedNode!!.filePath)
            val selectedChild = getSelectedNode(parent, selectedNode!!)

            if (selectedChild != null) {
                suppressFocusOnLine = true
                tree.selectionModel.selectionPath = TreePath(selectedChild.path)
                suppressFocusOnLine = false
            } else
                deselectNodeAndCodeHealthFinding()
        }

    private fun deselectNodeAndCodeHealthFinding() {
        selectedNode = null
        project.messageBus.syncPublisher(CodeHealthDetailsRefreshNotifier.TOPIC).refresh(null)
    }

    private fun handleTreeSelectionEvent(event: TreeSelectionEvent) {
        val navigationService = CodeNavigationService.getInstance(project)
        val targetNode = event.path.lastPathComponent as? DefaultMutableTreeNode
        val finding = targetNode?.userObject as? CodeHealthFinding ?: return

        if (targetNode.isLeaf) {
            Log.debug("Selected node with finding $finding", service)
            handleSelectionTelemetry(finding)

            if (!suppressFocusOnLine && finding.focusLine != null)
                navigationService.focusOnLine(finding.filePath, finding.focusLine)

            project.messageBus.syncPublisher(CodeHealthDetailsRefreshNotifier.TOPIC).refresh(finding)
            selectedNode = targetNode.userObject as CodeHealthFinding
        } else {
            (event.source as? JTree)?.clearSelection()

            handleDeselectionTelemetry()
            deselectNodeAndCodeHealthFinding()
        }
    }

    private fun handleDeselectionTelemetry() {
        if (selectedNode != null && !codeHealthSelected) {
            TelemetryService.getInstance().logUsage(TelemetryEvents.DETAILS_FUNCTION_DESELECTED)
        }
        codeHealthSelected = false
    }

    private fun handleSelectionTelemetry(finding: CodeHealthFinding) {
        if (shouldSelectHealthNode(finding)) {
            TelemetryService.getInstance().logUsage(TelemetryEvents.OPEN_CODE_HEALTH_DOCS)
            codeHealthSelected = true
        } else if (selectedNode == null || shouldSelectFunctionOfFileNode(finding)) {
            // TODO: provide additional data isRefactoringSupported when refactoring logic available
            TelemetryService.getInstance().logUsage(
                TelemetryEvents.DETAILS_FUNCTION_SELECTED,
                mutableMapOf<String, Any>(Pair("nIssues", finding.functionFindingIssues))
            )
            codeHealthSelected = false
        }
    }

    private fun shouldSelectFunctionOfFileNode(finding: CodeHealthFinding): Boolean =
        !isHealthNode(finding.nodeType) && codeHealthSelected

    private fun shouldSelectHealthNode(finding: CodeHealthFinding): Boolean =
        isHealthNode(finding.nodeType) && !codeHealthSelected

    private fun buildNode(filePath: String, delta: Delta): MutableTreeNode {
        val root = getRootNode(filePath, delta)

        return DefaultMutableTreeNode(root).apply {
            addCodeHealthLeaf(filePath, delta)
            addFileLeaves(filePath, delta)
            addFunctionLeaves(filePath, delta)
        }
    }

    private fun DefaultMutableTreeNode.addCodeHealthLeaf(filePath: String, delta: Delta) {
        val health = getHealthFinding(filePath, delta)

        add(DefaultMutableTreeNode(health))
    }

    private fun DefaultMutableTreeNode.addFileLeaves(filePath: String, delta: Delta) =
        delta.fileLevelFindings.forEach {
            val finding = getFileFinding(filePath, it)

            add(DefaultMutableTreeNode(finding))
        }

    private fun DefaultMutableTreeNode.addFunctionLeaves(filePath: String, delta: Delta) =
        delta.functionLevelFindings.forEach {
            val finding = getFunctionFinding(filePath, it.function, it.changeDetails)

            add(DefaultMutableTreeNode(finding))
        }
}