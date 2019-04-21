package com.intellij.idea.reviewcomment.model

import com.google.gson.Gson
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.*

private data class ProviderComment(val revision: String,
                                   val line:Int,
                                   val numberOfLines: Int,
                                   val notes:List<Note>,
                                   val resolved:Boolean) : Comparable<ProviderComment> {
    fun toComment(provider: ReviewCommentsProvider) = Comment(provider = provider,
            revision = revision, line = line, numberOfLines = numberOfLines, notes = notes, resolved = resolved)

    override fun compareTo(other: ProviderComment): Int {
        val cmp = Integer.compare(line, other.line)
        if (cmp != 0) return cmp

        if (notes.isEmpty() || other.notes.isEmpty())
            return if (notes.isEmpty()) -1 else 1

        return noteComparator.compare(notes[0], other.notes[0])
    }
}

private data class DataHolder(val project: String,
                              val comments: Map<String, SortedSet<ProviderComment>>)

class SimpleReviewCommentsProvider: ReviewCommentsProvider {
    private var comments:MutableMap<String, SortedSet<ProviderComment>> = mutableMapOf()

    override fun getName(): String = "Simple JSON"

    override fun getCurrentUser(): String = "me"

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
        return comments[path].orEmpty().map { it.toComment(this) }
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

    private fun toProviderComment(comment: Comment)
            = ProviderComment(revision = comment.revision, line = comment.line, numberOfLines = comment.numberOfLines,
                notes = comment.notes, resolved = comment.resolved)

    override fun updateComment(project: Project,
                               file: VirtualFile,
                               oldComment: Comment?,
                               comment: Comment,
                               onUpdateConsumer: (Any) -> Unit) {
        val path = toRelativePath(project, file)
        val set = comments.getOrPut(path) { TreeSet() }
        oldComment?.let { set.remove(toProviderComment(it)) }
        set.add(toProviderComment(comment))
        onUpdateConsumer(comment)

        persistData(project)
    }

}
