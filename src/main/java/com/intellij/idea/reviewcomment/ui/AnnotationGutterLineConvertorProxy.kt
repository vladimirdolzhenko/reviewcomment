package com.intellij.idea.reviewcomment.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.localVcs.UpToDateLineNumberProvider
import com.intellij.openapi.vcs.actions.ActiveAnnotationGutter

/**
 * based on {@link com.intellij.openapi.vcs.actions.AnnotationGutterLineConvertorProxy}
 * but fixed {@link com.intellij.openapi.vcs.actions.AnnotationGutterLineConvertorProxy#getPopupActions}
 */
class AnnotationGutterLineConvertorProxy(
        private val myGetUpToDateLineNumber: UpToDateLineNumberProvider,
        private val aDelegate: ActiveAnnotationGutter) :
        com.intellij.openapi.vcs.actions.AnnotationGutterLineConvertorProxy(myGetUpToDateLineNumber, aDelegate) {

    override fun getPopupActions(line: Int, editor: Editor): List<AnAction>? {
        // TODO: fixed mapping from editor line to original one
        // subject to fix ?
        val currentLine = myGetUpToDateLineNumber.getLineNumber(line)
        return if (canBeAnnotated(currentLine)) delegate.getPopupActions(currentLine, editor) else null
    }

    private fun canBeAnnotated(currentLine: Int): Boolean {
        return currentLine > UpToDateLineNumberProvider.ABSENT_LINE_NUMBER
    }
}
