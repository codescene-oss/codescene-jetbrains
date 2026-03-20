package com.codescene.jetbrains.platform.icons

import com.codescene.jetbrains.platform.icons.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.platform.util.PlatformConstants.codeSceneWindowFileNames
import com.intellij.ide.IconProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightVirtualFile
import javax.swing.Icon

internal class CodeSceneFileIconProvider : IconProvider() {
    override fun getIcon(
        element: PsiElement,
        flags: Int,
    ): Icon? {
        val vFile: VirtualFile? = element.containingFile?.virtualFile

        if (vFile is LightVirtualFile && codeSceneWindowFileNames.contains(vFile.name)) {
            return CODESCENE_TW
        }

        return null
    }
}
