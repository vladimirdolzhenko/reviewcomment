package com.intellij.idea.reviewcomment.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.*
import java.util.concurrent.ConcurrentHashMap

typealias ProjectComments = MutableMap<VirtualFile, SortedSet<Comment>>

class ReviewCommentsRepository {

    private var commentsPerProject: MutableMap<Project, ProjectComments> = mutableMapOf()

    private fun comments(project: Project) = commentsPerProject.getOrPut(project) { ConcurrentHashMap() }

    fun refresh(project: Project, file: VirtualFile) {
        val providers = ReviewCommentsProvider.EP_NAME.extensions
        if (providers.isEmpty()) return

        val allComments = mutableListOf<Comment>()
        for (provider in providers) {
            val comments: Collection<Comment> = provider.getComments(project, file)
            allComments.addAll(comments)
        }

        val values = allComments.toSortedSet()
        comments(project)[file] = values
    }

    fun getUnresolvedComments(project: Project, file: VirtualFile) = comments(project)[file]
                ?.filter { !it.resolved && it.notes.isNotEmpty() }
                ?.toSortedSet()

    fun updateComment(project: Project,
                      file: VirtualFile,
                      oldComment: Comment?,
                      comment: Comment,
                      onUpdateConsumer: (Any) -> Unit) {
        val set = comments(project).getOrPut(file) { TreeSet() }
        oldComment?.let { set.remove(it) }
        set.add(comment)
        for (provider in ReviewCommentsProvider.EP_NAME.extensions) {
            provider.updateComment(project, file, oldComment, comment) { onUpdateConsumer(comment) }
        }

        for (notifier in ReviewCommentNotifier.EP_NAME.extensions) {
            notifier.newComment(file, comment)
        }
    }
}