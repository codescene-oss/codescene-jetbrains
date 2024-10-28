package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.util.cancelPendingReviews
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class FileEditorLifecycleListener : FileEditorManagerListener {
    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        cancelPendingReviews(file, source.project)
    }

//    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
//        if (file.extension == "md" && codeSmellNames.contains(file.nameWithoutExtension)) {
//            customMarkdownEditorHandler(source, file)
//        }
//    }
//
//    private fun customMarkdownEditorHandler(source: FileEditorManager, file: VirtualFile) {
//        ApplicationManager.getApplication().invokeLater {
//            source.getEditors(file)[0].let { editor ->
//                if (editor is MarkdownEditorWithPreview) {
//                    Log.debug("Changing view mode for documentation files to preview")
//                    editor.layout = TextEditorWithPreview.Layout.SHOW_PREVIEW
//                }
//            }
//        }
//    }
}