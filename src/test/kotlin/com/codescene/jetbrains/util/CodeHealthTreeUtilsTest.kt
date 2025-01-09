package com.codescene.jetbrains.util

import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.components.codehealth.monitor.tree.NodeType
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class CodeHealthTreeUtilsTest {
    private lateinit var tree: JTree

    private val root = DefaultMutableTreeNode(
        CodeHealthFinding(
            tooltip = "Root Node",
            filePath = "root",
            focusLine = 0,
            displayName = "Root",
            nodeType = NodeType.ROOT
        )
    )

    private val node1 = DefaultMutableTreeNode(
        CodeHealthFinding(
            tooltip = "Tooltip for File1",
            filePath = "src/main/File1.kt",
            focusLine = 1,
            displayName = "File1",
            nodeType = NodeType.ROOT
        )
    )

    private val child11 = DefaultMutableTreeNode(
        CodeHealthFinding(
            tooltip = "Code Health is decreasing",
            filePath = "src/main/File1.kt",
            focusLine = 1,
            displayName = "Code Health decrease",
            nodeType = NodeType.CODE_HEALTH_DECREASE,
        )
    )

    private val child12 = DefaultMutableTreeNode(
        CodeHealthFinding(
            tooltip = "Complexity",
            filePath = "src/main/File1.kt",
            focusLine = 1,
            displayName = "Complex Code",
            nodeType = NodeType.FILE_FINDING,
        )
    )

    private val node2 = DefaultMutableTreeNode(
        CodeHealthFinding(
            tooltip = "Tooltip for File2",
            filePath = "src/main/File2.kt",
            focusLine = 1,
            displayName = "File2",
            nodeType = NodeType.ROOT
        )
    )

    private val child21 = DefaultMutableTreeNode(
        CodeHealthFinding(
            tooltip = "Code Health is increasing.",
            filePath = "src/main/File2.kt",
            focusLine = 1,
            displayName = "Code Health Increase",
            nodeType = NodeType.CODE_HEALTH_INCREASE,
        )
    )

    private val child22 = DefaultMutableTreeNode(
        CodeHealthFinding(
            tooltip = "Excess number of function arguments",
            filePath = "src/main/File2.kt",
            focusLine = 6,
            displayName = "functionName",
            nodeType = NodeType.FUNCTION_FINDING,
        )
    )

    private val nonExistentFinding = CodeHealthFinding(
        tooltip = "Non-existent node",
        filePath = "src/main/File3.kt",
        focusLine = 1,
        displayName = "Non-existent",
        nodeType = NodeType.FILE_FINDING
    )

    @Before
    fun setupTree() {
        node1.add(child11)
        node1.add(child12)
        node2.add(child21)
        node2.add(child22)

        root.add(node1)
        root.add(node2)

        tree = JTree(DefaultTreeModel(root))
    }

    @After
    fun clearTree() {
        root.removeAllChildren()
        node1.removeAllChildren()
        node2.removeAllChildren()
    }

    @Test
    fun `should return correct health node when matching filePath exists`() {
        val result = findHealthNodeForPath(root, "src/main/File2.kt")

        assertNotNull(result)
        assertEquals(child21, result)
    }

    @Test
    fun `should return null when no matching filePath exists`() {
        val result = findHealthNodeForPath(root, "src/main/noPath.kt")

        assertNull(result)
    }

    @Test
    fun `should select the correct node in the tree`() {
        val path = "src/main/File1.kt"

        selectNode(tree, path)

        val expectedPath = TreePath(child11.path)
        assertNotNull(tree.selectionModel.selectionPath)
        assertEquals(expectedPath, tree.selectionModel.selectionPath)
    }

    @Test
    fun `should do nothing if no matching node is found`() {
        val path = "src/main/noMatch.kt"

        selectNode(tree, path)

        assertNull(tree.selectionModel.selectionPath)
    }

    @Test
    fun `should return first child when selected node is a health node`() {
        val selectedNode = child11.userObject as CodeHealthFinding

        val result = getSelectedNode(node1, selectedNode)

        assertNotNull(result)
        assertEquals(child11, result)
    }

    @Test
    fun `should return correct child node when display name matches`() {
        val selectedNode = child22.userObject as CodeHealthFinding

        val result = getSelectedNode(node2, selectedNode)

        assertNotNull(result)
        assertEquals(child22, result)
    }

    @Test
    fun `should return null if no child matches display name`() {
        val result = getSelectedNode(node1, nonExistentFinding)

        assertNull(result)
    }

    @Test
    fun `should return parent node when present`() {
        val selectedNode = child22.userObject as CodeHealthFinding

        val result = getParentNode(root, selectedNode)

        assertNotNull(result)
        assertEquals(node2, result)
    }

    @Test
    fun `should return null when parent node is not present`() {
        val result = getParentNode(root, nonExistentFinding)

        assertNull(result)
    }
}