package com.intellij.idea.reviewcomment.ui

import com.intellij.idea.reviewcomment.model.ReviewCommentsRepository
import com.intellij.idea.reviewcomment.model.getReviewCommentsRepository
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.LineMarkerRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.localVcs.UpToDateLineNumberProvider
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.AbstractVcsHelper
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiUtilBase
import java.awt.Color
import java.awt.Graphics
import java.awt.Rectangle
import java.util.function.Consumer

private val LINE_MARKERS_IN_EDITOR_KEY = Key.create<MutableList<RangeHighlighter>>("ReviewComments.LINE_MARKERS_IN_EDITOR_KEY")

class ReviewCommentsHighlighter : StartupActivity {

    override fun runActivity(project: Project) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment) return
        val commentsRepo = getReviewCommentsRepository(project)

        val commentsListener = Consumer<VirtualFile> {
            val selectedEditor = FileEditorManager.getInstance(project)
                    .getSelectedEditor(it)
            val fileEditor = selectedEditor as? TextEditor ?: return@Consumer

            rebuildLineMarkers(fileEditor.editor, project, commentsRepo)
        }

        val listener = object : FileEditorManagerListener {
            override fun fileOpenedSync(source: FileEditorManager,
                                        file: VirtualFile,
                                        editors: Pair<Array<FileEditor>, Array<FileEditorProvider>>)
                    = loadAndShowComments(project, commentsRepo, source, file)

            override fun fileClosed(source: FileEditorManager, file: VirtualFile) = commentsRepo.close(file)
        }

        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                listener)

        val eventMulticaster = EditorFactory.getInstance().eventMulticaster
        val documentListener = object: DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val editors = EditorFactory.getInstance().getEditors(event.document, project)
                for (editor in editors) {
                    rebuildLineMarkers(editor, project, commentsRepo)
                }
            }
        }
        eventMulticaster.addDocumentListener(documentListener, project)

        // on project close entire comments repo is disposed
        commentsRepo.addListener(commentsListener)

        val fileEditorManager = FileEditorManager.getInstance(project)
        val allEditors = fileEditorManager.allEditors
        for (allEditor in allEditors) {
            allEditor.file?.let {
                loadAndShowComments(project, commentsRepo, fileEditorManager, it)
            }
        }

    }

    private fun loadAndShowComments(project: Project, repository:ReviewCommentsRepository,
                                    source: FileEditorManager, virtualFile: VirtualFile) {
        (!AbstractVcs.fileInVcsByFileStatus(project, virtualFile)) && return

        // TODO: is feature/setting enabled ?

        val fileEditors = source.getEditors(virtualFile)
        for (fileEditor in fileEditors) {
            if (fileEditor !is TextEditor) continue

            val editor = fileEditor.editor

            val upToDateLineNumbers = MyUpToDateLineNumberProvider(editor.document, project)

            try {
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
                            addReviewGutterItems(project, repository, virtualFile, upToDateLineNumbers, editor)
                        }
                    }
                }
                ProgressManager.getInstance().run(refreshTask)
            } catch (ex: VcsException) {
                VcsBalloonProblemNotifier.showOverVersionControlView(project,
                        "failed to annotate file ${virtualFile.path} from vcs\n ${ex.message}",
                        MessageType.ERROR)
            }
        }
    }

    private fun addReviewGutterItems(project: Project,
                                     repository: ReviewCommentsRepository,
                                     virtualFile: VirtualFile,
                                     upToDateLineNumbers: MyUpToDateLineNumberProvider,
                                     editor: Editor) {
        addLineMarkers(project, repository, virtualFile,
                editor = editor, upToDateLineNumbers = upToDateLineNumbers)
    }

    private val lineMarkerRenderer = object : LineMarkerRenderer{
        val color = Color.ORANGE

        override fun paint(editor: Editor, g: Graphics, r: Rectangle) {
            g.color = color
            g.fillRect(r.x, r.y, 3, r.height)
        }
    }

    private fun rebuildLineMarkers(editor: Editor, project: Project, commentsRepo: ReviewCommentsRepository) {
        val upToDateLineNumbers = MyUpToDateLineNumberProvider(editor.document, project)
        removeLineMarkers(editor)
        val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project)
        val virtualFile = psiFile?.virtualFile ?: return

        addLineMarkers(project = project,
                repository = commentsRepo,
                virtualFile = virtualFile,
                editor = editor,
                upToDateLineNumbers = upToDateLineNumbers)
    }

    private fun addLineMarkers(project: Project,
                               repository: ReviewCommentsRepository,
                               virtualFile: VirtualFile,
                               editor: Editor,
                               upToDateLineNumbers: MyUpToDateLineNumberProvider) {
        val comments = repository.getUnresolvedComments(virtualFile)
        comments.isEmpty() && return

        val document = editor.document
        val lineMarkers = editor.getUserData(LINE_MARKERS_IN_EDITOR_KEY) ?: mutableListOf()
        for (comment in comments) {
            val startLine = upToDateLineNumbers.getFromVSCLineNumber(comment.line)
            val endOfCommentsLine = comment.line + comment.numberOfLines - 1
            val endLine = upToDateLineNumbers.getFromVSCLineNumber(endOfCommentsLine)

            (startLine >= document.lineCount || endLine >= document.lineCount) && continue

            (startLine <= UpToDateLineNumberProvider.ABSENT_LINE_NUMBER
                    || endLine <= UpToDateLineNumberProvider.ABSENT_LINE_NUMBER) && continue

            val startOffset = document.getLineStartOffset(startLine)
            val endOffset = document.getLineEndOffset(endLine)

            val highlighter = editor.markupModel.addRangeHighlighter(startOffset, endOffset,
                    HighlighterLayer.ADDITIONAL_SYNTAX + 3, null,
                    HighlighterTargetArea.LINES_IN_RANGE)
            highlighter.lineMarkerRenderer = lineMarkerRenderer
            highlighter.gutterIconRenderer = MyGutterIconRenderer(
                    myProject = project,
                    myComment = comment,
                    myCommentsRepo = repository,
                    myFile = virtualFile)

            lineMarkers.add(highlighter)
        }
        editor.putUserData<MutableList<RangeHighlighter>>(LINE_MARKERS_IN_EDITOR_KEY, lineMarkers)
    }

    private fun removeLineMarkers(editor: Editor) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        val lineMarkers = editor.getUserData(LINE_MARKERS_IN_EDITOR_KEY) ?: emptyList<RangeHighlighter>()
        if (!lineMarkers.isNullOrEmpty()) {
            for (lineMarker in lineMarkers) {
                editor.markupModel.removeHighlighter(lineMarker)
                lineMarker.dispose()
            }
        }
        editor.putUserData<MutableList<RangeHighlighter>>(LINE_MARKERS_IN_EDITOR_KEY, null)
    }
}