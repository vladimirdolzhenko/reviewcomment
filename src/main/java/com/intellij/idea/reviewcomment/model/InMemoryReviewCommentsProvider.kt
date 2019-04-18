package com.intellij.idea.reviewcomment.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.*

class InMemoryReviewCommentsProvider : ReviewCommentsProvider {
    private var comments: MutableMap<VirtualFile, SortedSet<Comment>> = mutableMapOf()

    override fun getName(): String = "In memory storage"

    override fun getCurrentUser(): String = "user"

    override fun getComments(project: Project, file: VirtualFile) = comments[file].orEmpty()

    override fun updateComment(project: Project,
                               file: VirtualFile,
                               oldComment: Comment?,
                               comment: Comment,
                               onUpdateConsumer: (Any) -> Unit) {
        val set = comments.getOrPut(file) { TreeSet() }
        oldComment?.let { set.remove(it) }
        set.add(comment)
        onUpdateConsumer(comment)
    }
}