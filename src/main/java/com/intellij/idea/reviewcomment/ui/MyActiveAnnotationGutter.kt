package com.intellij.idea.reviewcomment.ui

import com.intellij.icons.AllIcons
import com.intellij.idea.reviewcomment.model.Comment
import com.intellij.idea.reviewcomment.model.Note
import com.intellij.idea.reviewcomment.model.ReviewCommentsRepository
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.actions.ActiveAnnotationGutter
import com.intellij.openapi.vcs.annotate.AnnotationSource
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap
import java.awt.Color
import java.awt.Cursor

import java.util.ArrayList

internal class MyActiveAnnotationGutter(private val myProject: Project,
                                        private val myCommentsRepo: ReviewCommentsRepository,
                                        private val myFile: VirtualFile,
                                        private val myVcsRevisionNumber: VcsRevisionNumber) : ActiveAnnotationGutter {
    private var myComments: TIntObjectHashMap<MutableList<Comment>>? = null

    init {
        fetchComments()
    }

    private fun fetchComments() {
        val comments = myCommentsRepo.getUnresolvedComments(myProject, myFile)

        if (comments.isNullOrEmpty()) return

        val map = TIntObjectHashMap<MutableList<Comment>>()
        for (comment in comments) {
            var list: MutableList<Comment>? = map.get(comment.line)
            if (list == null) {
                list = ArrayList()
                map.put(comment.line, list)
            }
            list.add(comment)
        }
        myComments = map

        // TODO: how to fire gutter to be updated?
    }

    override fun doAction(lineNum: Int) = openDialog(lineNum, false)

    private fun openDialog(lineNum: Int, forceNew: Boolean) {
        val comments = getComments(lineNum)
        val numberOfNotes = getNumberOfNotes(comments)

        val note: Note
        val comment: Comment
        if (numberOfNotes == 0 || forceNew) {
            note = Note(null, myCommentsRepo.currentUser, "")
            comment = if (forceNew && comments != null)
                comments[0]
            else
                Comment(myVcsRevisionNumber.asString(), lineNum, emptyList(), false)
        } else {
            // pick up last note of last comment for this line
            comment = comments!![comments.size - 1]
            val notes = comment.notes
            note = notes[notes.size - 1]
        }

        openDialog(myFile, comment, note)
    }

    private fun openDialog(virtualFile: VirtualFile, comment: Comment, note: Note) {
        val dialog = EditCommentDialog(myProject, myCommentsRepo,
                { newComment -> myCommentsRepo.updateComment(myProject, virtualFile, comment, newComment)
                    { fetchComments() }
                }, comment, note)
        dialog.show()
    }

    private fun getNumberOfNotes(comments: List<Comment>?): Int
        = comments?.stream()?.mapToInt { (_, _, notes) -> notes.size }?.sum() ?: 0

    override fun getCursor(lineNum: Int): Cursor
        = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

    override fun getLineText(line: Int, editor: Editor): String? {
        val notes = getNumberOfNotes(getComments(line))
        return when (notes) {
            0 ->    "     "
            1 ->    " !!  "
            else -> " !!! "
        }
    }

    override fun getToolTip(line: Int, editor: Editor): String? {
        val comments = getComments(line) ?: return "leave a review comment"
        val numberOfNotes = getNumberOfNotes(comments)

        if (numberOfNotes < 5) {
            return html {
                var first = true
                for (comment in comments) {
                    for (note in comment.notes) {
                        if (!first) br {}
                        text(note.author + ":" + note.getFormattedTimestamp())
                        hr {}
                        pre { text(note.comment) }
                        first = false
                    }
                }
            }.toString()
        }

        return "$numberOfNotes review comments"
    }

    private fun getComments(line: Int): List<Comment>? = myComments?.get(line) ?: null

    private fun isCommented(line: Int): Boolean = getComments(line) != null

    override fun getStyle(line: Int, editor: Editor): EditorFontType? = null

    override fun getColor(line: Int, editor: Editor): ColorKey? = AnnotationSource.LOCAL.color

    override fun getBgColor(line: Int, editor: Editor): Color? = if (isCommented(line)) Color.ORANGE else null

    override fun getPopupActions(line: Int, editor: Editor): List<AnAction> {
        val comments = getComments(line)

        val actions = ArrayList<AnAction>()
        run {
            val leaveCommentTxt = "leave a review comment"
            val action = object : AnAction(leaveCommentTxt, leaveCommentTxt, AllIcons.General.Add) {
                override fun actionPerformed(e: AnActionEvent) {
                    openDialog(line, true)
                }
            }
            actions.add(action)
        }
        if (comments == null) {
            return actions
        }

        for (comment in comments) {
            for (note in comment.notes) {
                val description = note.getFormattedTimestamp() + ": " + note.author
                actions.add(object : AnAction(description, description, Icons.Actions.COMMENTED) {
                    override fun actionPerformed(e: AnActionEvent) {
                        openDialog(myFile, comment, note)
                    }
                })
            }

            val resolveCommentTxt = "resolve comment"
            val action = object : AnAction(resolveCommentTxt, resolveCommentTxt, AllIcons.Actions.Cancel) {
                override fun actionPerformed(e: AnActionEvent) {
                    myCommentsRepo.updateComment(myProject, myFile, comment, comment.toResolved())
                        { fetchComments() }
                }
            }
            actions.add(action)
        }

        return actions
    }

    override fun gutterClosed() {}
}