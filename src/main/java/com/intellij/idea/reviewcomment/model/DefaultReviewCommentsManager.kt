package com.intellij.idea.reviewcomment.model

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

class DefaultReviewCommentsManager : ReviewCommentsManager {
    private val repos:MutableMap<Project, ReviewCommentsRepository> = mutableMapOf()

    override fun getReviewCommentsRepository(project: Project): ReviewCommentsRepository {
        return repos.computeIfAbsent(project) {
            Disposer.register(project, Disposable { repos.remove(project) })
            ReviewCommentsRepository(it)
        }
    }

}