package com.intellij.idea.reviewcomment.ui

import com.intellij.icons.AllIcons
import com.intellij.idea.reviewcomment.model.Comment
import com.intellij.idea.reviewcomment.model.Note
import com.intellij.idea.reviewcomment.model.ReviewCommentsRepository
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class MyGutterIconRenderer(private val myProject: Project,
                           private val myFile: VirtualFile,
                           private val myComment: Comment,
                           private val myCommentsRepo: ReviewCommentsRepository
) : GutterIconRenderer(), DumbAware {

    private fun getNumberOfNotes(): Int = myComment.notes.size

    private fun openDialog() {
        val note = Note()
        openDialog(myComment, note)
    }

    private fun openDialog(comment: Comment, note: Note) {
        val dialog = EditCommentDialog(myProject,
                { newComment -> myCommentsRepo.updateComment(myFile, comment, newComment) },
                comment, note)
        dialog.show()
    }

    override fun getPopupMenuActions(): ActionGroup? {
        val popupMenuActionsList = getPopupMenuActionsList()
        popupMenuActionsList.isEmpty() && return null

        val popupAction = DefaultActionGroup()
        for (action in popupMenuActionsList) {
            popupAction.add(action)
        }
        return popupAction
    }

    private fun getPopupMenuActionsList(): List<AnAction> {
        val actions = mutableListOf<AnAction>()

        run {
            val leaveCommentTxt = "add a review comment"
            val action = object : AnAction(leaveCommentTxt, leaveCommentTxt, AllIcons.General.Add) {
                override fun actionPerformed(e: AnActionEvent) {
                    openDialog()
                }
            }
            actions.add(action)
        }


        for (note in myComment.notes) {
            val description = noteDescription(note)
            actions.add(object : AnAction(description, description, Icons.Actions.COMMENT) {
                override fun actionPerformed(e: AnActionEvent) {
                    openDialog(myComment, note)
                }
            })
        }

        val resolveCommentTxt = "resolve comment"
        val action = object : AnAction(resolveCommentTxt, resolveCommentTxt, AllIcons.Actions.Cancel) {
            override fun actionPerformed(e: AnActionEvent) {
                myCommentsRepo.updateComment(myFile, myComment, myComment.toResolved())
            }
        }
        actions.add(action)

        return actions
    }

    override fun getClickAction(): AnAction {
        val lastNote = myComment.notes.last()
        val description = noteDescription(lastNote)
        val action = object : AnAction(description, description, icon) {
            override fun actionPerformed(e: AnActionEvent) {
                openDialog(myComment, lastNote)
            }
        }
        return action
    }

    private fun noteDescription(note: Note) =
            note.getFormattedTimestamp() + ": " + note.author

    override fun getTooltipText(): String? {
        val numberOfNotes = getNumberOfNotes()

        if (numberOfNotes < 5) {
            return html {
                var first = true
                for (note in myComment.notes) {
                    if (!first) br {}
                    text(note.author + ":" + note.getFormattedTimestamp())
                    hr {}
                    pre { text(note.comment) }
                    first = false
                }
            }.toString()
        }

        return "$numberOfNotes review comments"
    }

    override fun isNavigateAction(): Boolean = true

    override fun getAlignment(): Alignment {
        return Alignment.RIGHT
    }

    override fun getIcon(): Icon {
        return when (getNumberOfNotes()) {
            1 -> Icons.Actions.COMMENT
            else -> Icons.Actions.COMMENTS
        }
    }

    override fun hashCode(): Int = myComment.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other !is MyGutterIconRenderer) return false
        return myComment == other.myComment
    }
}