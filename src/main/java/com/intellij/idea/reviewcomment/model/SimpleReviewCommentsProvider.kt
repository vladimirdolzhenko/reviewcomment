package com.intellij.idea.reviewcomment.model

import com.google.gson.Gson
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.*


class SimpleReviewCommentsProvider: ReviewCommentsProvider {
    private var comments:MutableMap<String, SortedSet<Comment>> = HashMap()

    override fun getCurrentUser(): String {
        return "me"
    }

    private fun toRelativePath(project: Project, file: VirtualFile): String {
        var projectFile:VirtualFile? = project.workspaceFile
        // project workspace could be file-based or folder-based
        if (projectFile!!.parent.name == ".idea") projectFile = projectFile.parent

        val relativePath = VfsUtilCore.getRelativePath(file, projectFile!!.parent)
        return relativePath!!
    }

    override fun getComments(project: Project, file: VirtualFile): Collection<Comment> {
        loadDataIfNeeded(project)
        val path = toRelativePath(project, file)
        return comments[path].orEmpty()
    }

    private fun loadDataIfNeeded(project: Project) {
        if (!comments.isEmpty()) return

        val storage = storageFile(project)
        if (!storage.exists()) return

        val json = storage.readText()

        val gson = Gson()
        gson.fromJson(json, DataHolder::class.java).let {
            comments.putAll(it.comments)
        }
    }

    private fun persistData(project: Project) {
        val gson = Gson()

        val json = gson.toJson(DataHolder(
                project = project.name,
                comments = comments
        ))

        val storage = storageFile(project)
        storage.writeText(json)
    }

    private fun storageFile(project: Project): File {
        var path = File(project.basePath)

        var projectFile = project.workspaceFile!!
        // project workspace could be file-based or folder-based
        if (projectFile.parent.name == ".idea") path = File(path, projectFile.parent.name)
        return File(path, "comments.json")
    }

    override fun updateComment(project: Project,
                               file: VirtualFile,
                               oldComment: Comment?,
                               comment: Comment,
                               onUpdateConsumer: (Any) -> Unit) {
        val path = toRelativePath(project, file)
        val set = comments.getOrPut(path) { TreeSet() }
        oldComment?.let { set.remove(it) }
        set.add(comment)
        onUpdateConsumer(comment)

        persistData(project)
    }

    data class DataHolder(val project: String,
                          val comments: Map<String, SortedSet<Comment>>)
}
