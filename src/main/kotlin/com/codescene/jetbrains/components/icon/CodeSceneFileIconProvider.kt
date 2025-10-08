package com.codescene.jetbrains.components.icon

import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.util.Constants.codeSceneWindowFileNames
import com.intellij.ide.IconProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightVirtualFile
import javax.swing.Icon

internal class CodeSceneFileIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        val vFile: VirtualFile? = element.containingFile?.virtualFile

        if (vFile is LightVirtualFile && codeSceneWindowFileNames.contains(vFile.name))
            return CODESCENE_TW

        return null
    }
}