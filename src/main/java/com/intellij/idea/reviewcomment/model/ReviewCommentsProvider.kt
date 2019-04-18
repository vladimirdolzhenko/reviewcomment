package com.intellij.idea.reviewcomment.model

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface ReviewCommentsProvider {

    companion object {
        val EP_NAME =
                ExtensionPointName.create<ReviewCommentsProvider>("com.intellij.idea.reviewcomment.model.ReviewCommentsProvider")

    }

    fun getName(): String

    fun getCurrentUser(): String

    fun getComments(project: Project, file: VirtualFile): Collection<Comment>

    fun updateComment(project: Project,
                      file: VirtualFile,
                      oldComment:Comment?,
                      comment: Comment,
                      onUpdateConsumer: (Any) -> Unit)

}

interface ReviewCommentNotifier {

    companion object {
        val EP_NAME =
                ExtensionPointName.create<ReviewCommentNotifier>("com.intellij.idea.reviewcomment.model.ReviewCommentNotifier")
    }

    fun newComment(file: VirtualFile, comment: Comment)

}