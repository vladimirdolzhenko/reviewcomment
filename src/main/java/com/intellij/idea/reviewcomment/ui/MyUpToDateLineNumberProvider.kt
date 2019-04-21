package com.intellij.idea.reviewcomment.ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.localVcs.UpToDateLineNumberProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ex.LineStatusTracker
import com.intellij.openapi.vcs.impl.LineStatusTrackerManager
import com.intellij.openapi.vcs.impl.LineStatusTrackerManagerI

class MyUpToDateLineNumberProvider (private val document: Document, project: Project): UpToDateLineNumberProvider {

    private val myLineStatusTrackerManagerI: LineStatusTrackerManagerI = LineStatusTrackerManager.getInstance(project)

    private fun getTracker(): LineStatusTracker<*>? {
        val tracker = myLineStatusTrackerManagerI.getLineStatusTracker(document)
        return if (tracker != null && tracker.isOperational()) tracker else null
    }

    override fun getLineCount(): Int = getTracker()?.vcsDocument?.lineCount ?: document.lineCount

    override fun isRangeChanged(start: Int, end: Int): Boolean = getTracker()?.isRangeModified(start, end) ?: false

    override fun isLineChanged(docLineNumber: Int): Boolean = getTracker()?.isLineModified(docLineNumber) ?: false

    override fun getLineNumber(docLineNumber: Int): Int {
        val tracker: LineStatusTracker<*>? = getTracker()
        return tracker?.transferLineToVcs(docLineNumber, false) ?: docLineNumber
    }

    fun getFromVSCLineNumber(vcsLineNumber: Int): Int {
        val tracker: LineStatusTracker<*>? = getTracker()
        return tracker?.transferLineFromVcs(vcsLineNumber, false) ?: vcsLineNumber
    }

}