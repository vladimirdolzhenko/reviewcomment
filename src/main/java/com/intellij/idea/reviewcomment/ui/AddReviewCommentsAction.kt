package com.intellij.idea.reviewcomment.ui

import com.intellij.idea.reviewcomment.model.Comment
import com.intellij.idea.reviewcomment.model.Note
import com.intellij.idea.reviewcomment.model.getReviewCommentsRepository
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.psi.PsiDocumentManager

class AddReviewCommentsAction:
        DumbAwareAction("Add a review comment", "Add a review comment", Icons.Actions.COMMENT) {

    override fun update(e: AnActionEvent) {
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val virtualFile = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE)
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return

        e.presentation.isVisible = AbstractVcs.fileInVcsByFileStatus(project, virtualFile)

        val selectionStart = editor.selectionModel.selectionStart
        val selectionEnd = editor.selectionModel.selectionEnd

        var enabled = false
        if (selectionStart in 0..selectionEnd) {
            val upToDateLineNumbers = MyUpToDateLineNumberProvider(editor.document, project)

            val lineNumStart = document.getLineNumber(selectionStart)
            val lineNumberEnd = document.getLineNumber(selectionEnd)
            val rangeChanged = upToDateLineNumbers.isRangeChanged(start = lineNumStart, end = lineNumberEnd)
            enabled = !rangeChanged
        }

        e.presentation.isEnabled = enabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE)
        val virtualFile = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return
        val selectionStart = editor.selectionModel.selectionStart
        val selectionEnd = editor.selectionModel.selectionEnd

        if (selectionStart in 0..selectionEnd) {
            val upToDateLineNumbers = MyUpToDateLineNumberProvider(editor.document, project)

            val lineNumStart = upToDateLineNumbers.getLineNumber(document.getLineNumber(selectionStart))
            val lineNumberEnd = upToDateLineNumbers.getLineNumber(document.getLineNumber(selectionEnd))
            val numberOfLines = lineNumberEnd - lineNumStart + 1

            val commentsRepo = getReviewCommentsRepository(project)
            val activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(virtualFile) ?: return

            val annotationProvider = activeVcs.annotationProvider ?: return
            val fileAnnotation = annotationProvider.annotate(virtualFile)
            val revisionNumber = fileAnnotation.currentRevision?.asString() ?: return

            val note = Note()
            val comment = Comment(revision = revisionNumber, line = lineNumStart, numberOfLines = numberOfLines)
            val dialog = EditCommentDialog(project,
                    { newComment -> commentsRepo.updateComment(virtualFile, comment, newComment) },
                    comment, note)
            dialog.show()
        }
    }

}
