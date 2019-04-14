package com.intellij.idea.reviewcomment.model

import com.intellij.openapi.vfs.VirtualFile

class SimpleReviewCommentNotifier: ReviewCommentNotifier {
    override fun newComment(file: VirtualFile, comment: Comment) {
        println("file $file commented with $comment")
    }
}