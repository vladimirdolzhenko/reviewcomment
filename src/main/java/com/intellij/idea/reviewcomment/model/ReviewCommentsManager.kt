package com.intellij.idea.reviewcomment.model

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

fun getReviewCommentsRepository(project: Project): ReviewCommentsRepository {
    val service = ServiceManager.getService(project, ReviewCommentsManager::class.java)
    return service.getReviewCommentsRepository(project)
}

interface ReviewCommentsManager {

    fun getReviewCommentsRepository(project: Project): ReviewCommentsRepository

}