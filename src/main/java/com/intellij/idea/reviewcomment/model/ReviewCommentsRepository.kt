package com.intellij.idea.reviewcomment.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.*

class ReviewCommentsRepository(private val project: Project) {

    private var comments: MutableMap<VirtualFile, SortedSet<Comment>> = mutableMapOf()

    fun refresh(file: VirtualFile) {
        val providers = ReviewCommentsProvider.EP_NAME.extensions
        if (providers.isEmpty()) return

        val allComments = mutableListOf<Comment>()
        for (provider in providers) {
            val comments: Collection<Comment> = provider.getComments(project, file)
            allComments.addAll(comments)
        }

        val values = allComments.toSortedSet()
        comments[file] = values
    }

    fun getUnresolvedComments(file: VirtualFile) = comments[file]
                ?.filter { !it.resolved && it.notes.isNotEmpty() }
                ?.toSortedSet() ?: emptySet<Comment>()

    fun updateComment(file: VirtualFile,
                      oldComment: Comment?,
                      comment: Comment,
                      onUpdateConsumer: (Any) -> Unit) {
        val set = comments.getOrPut(file) { TreeSet() }
        oldComment?.let { set.remove(it) }
        set.add(comment)
        for (provider in ReviewCommentsProvider.EP_NAME.extensions) {
            provider.updateComment(project, file, oldComment, comment) { onUpdateConsumer(comment) }
        }

        for (notifier in ReviewCommentNotifier.EP_NAME.extensions) {
            notifier.newComment(file, comment)
        }
    }

    fun close(file:VirtualFile) {
        comments.remove(file)
    }
}