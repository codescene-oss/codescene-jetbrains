package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class ConstructorOverInjectionCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Constructor Over Injection"
    override val id = "codeVision.codescene.constructorOverInjection"
    override val name = "com.codescene.codeVision.constructorOverInjection"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}