package com.intellij.idea.reviewcomment.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.*
import java.util.function.Consumer

typealias Comments = SortedSet<Comment>
class ReviewCommentsRepository(private val project: Project) {

    private val myComments: MutableMap<VirtualFile, Comments> = mutableMapOf()
    private val myListeners: MutableList<Consumer<VirtualFile>> = mutableListOf()

    fun addListener(listener:Consumer<VirtualFile>) = myListeners.add(listener)

    fun removeListener(listener:Consumer<VirtualFile>) = myListeners.remove(listener)

    fun refresh(file: VirtualFile) {
        val providers = ReviewCommentsProvider.EP_NAME.extensions
        if (providers.isEmpty()) return

        val allComments = mutableListOf<Comment>()
        for (provider in providers) {
            val comments = provider.getComments(project, file)
            allComments.addAll(comments)
        }

        val values = allComments.toSortedSet()
        myComments[file] = values
    }

    fun getUnresolvedComments(file: VirtualFile) = myComments[file]
                ?.filter { !it.resolved && it.notes.isNotEmpty() }
                ?.toSortedSet() ?: emptySet<Comment>()

    fun updateComment(file: VirtualFile,
                      oldComment: Comment?,
                      comment: Comment) {
        val comments = myComments.getOrPut(file) { TreeSet() }
        oldComment?.let { comments.remove(it) }
        comments.add(comment)
        for (provider in ReviewCommentsProvider.EP_NAME.extensions) {
            provider.updateComment(project, file, oldComment, comment) {
                for (listener in myListeners) {
                    listener.accept(file)
                }
            }
        }

        for (notifier in ReviewCommentNotifier.EP_NAME.extensions) {
            notifier.newComment(file, comment)
        }
    }

    fun close(file:VirtualFile) {
        myComments.remove(file)
    }
}