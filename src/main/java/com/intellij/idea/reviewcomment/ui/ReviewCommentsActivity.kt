package com.intellij.idea.reviewcomment.ui

import com.intellij.idea.reviewcomment.model.ReviewCommentsRepository
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vcs.AbstractVcsHelper
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier
import com.intellij.openapi.vfs.VirtualFile

class ReviewCommentsActivity : StartupActivity {

    override fun runActivity(project: Project) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment) return

        val repository = ReviewCommentsRepository(project)

        var listener = object : FileEditorManagerListener {
            override fun fileOpenedSync(source: FileEditorManager,
                                        file: VirtualFile,
                                        editors: Pair<Array<FileEditor>, Array<FileEditorProvider>>)
                    = loadAndShowComments(project, repository, source, file)

            override fun fileClosed(source: FileEditorManager, file: VirtualFile) = repository.close(file)
        }

        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                listener)

        val fileEditorManager = FileEditorManager.getInstance(project)
        val allEditors = fileEditorManager.allEditors
        for (allEditor in allEditors) {
            allEditor.file?.let {
                loadAndShowComments(project, repository, fileEditorManager, it)
            }
        }

    }

    private fun loadAndShowComments(project: Project, repository:ReviewCommentsRepository,
                                    source: FileEditorManager, virtualFile: VirtualFile) {
        // TODO: is feature/setting enabled ?

        val fileEditors = source.getEditors(virtualFile)
        for (fileEditor in fileEditors) {
            if (fileEditor !is TextEditor) continue

            val editor = fileEditor.editor

            val gutter = editor!!.gutter

            val upToDateLineNumbers = UpToDateLineNumberProviderImpl(editor!!.document, project)

            val activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(virtualFile)

            val annotationProvider = activeVcs!!.annotationProvider!!

            try {
                val fileAnnotation = annotationProvider.annotate(virtualFile)
                val vcsRevisionNumber = fileAnnotation.currentRevision

                val newCommentsUpdated = Ref<Boolean>()
                val exceptionRef = Ref<VcsException>()

                val refreshTask = object : Task.Backgroundable(project,
                        "Loading review comments [ " + virtualFile.name + " ]", true) {
                    override fun run(indicator: ProgressIndicator) {
                        try {
                            repository.refresh(virtualFile)
                            newCommentsUpdated.set(java.lang.Boolean.TRUE)
                        } catch (e: Exception) {
                            exceptionRef.set(VcsException(e))
                        }

                    }

                    override fun onCancel() {
                        onSuccess()
                    }

                    override fun onSuccess() {
                        if (!exceptionRef.isNull) {
                            AbstractVcsHelper.getInstance(project).showErrors(
                                    listOf(exceptionRef.get()),
                                    VcsBundle.message("message.title.annotate"))
                        }

                        if (!newCommentsUpdated.isNull) {
                            val annotationGutter = MyActiveAnnotationGutter(project, repository, virtualFile, vcsRevisionNumber!!)

                            val proxy = AnnotationGutterLineConvertorProxy(upToDateLineNumbers, annotationGutter)
                            gutter.registerTextAnnotation(proxy, proxy)
                        }
                    }
                }
                ProgressManager.getInstance().run(refreshTask)
            } catch (ex: VcsException) {
                VcsBalloonProblemNotifier.showOverVersionControlView(project,
                        "failed to annotate file " + virtualFile.getPath()
                                + " from vcs\n" + ex.message, MessageType.ERROR)
            }
        }
    }
}