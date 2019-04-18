package com.intellij.idea.reviewcomment.model

import java.lang.IllegalStateException
import java.time.Instant

import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

data class Note(val timestamp: Instant? = null,
                val author: String? = null,
                val comment: String = "") {
    init {
        timestamp == null && author != null
                && throw IllegalStateException("Only new note could not have author")
        timestamp != null && author == null
                && throw IllegalStateException("note has to have author when it has timestamp")
    }

    fun getFormattedTimestamp(): String
            = timestamp?.let { dateTimeFormatter.format(it) } ?: "now"

    fun isNew(): Boolean = timestamp == null
}

val noteComparator = object : Comparator<Note> {
    override fun compare(a: Note, b: Note): Int {
        val cmp = when {
            a.timestamp == null && b.timestamp == null -> 0
            a.timestamp == null && b.timestamp != null -> -1
            a.timestamp != null && b.timestamp == null -> 1
            else -> a.timestamp!!.compareTo(b.timestamp)
        }
        return when (cmp) {
            0 -> a.author?.compareTo(b.author!!) ?: 0
            else -> cmp
        }
    }
}

data class Comment(
        val provider: ReviewCommentsProvider? = null,
        val revision: String,
        val line:Int,
        val notes:List<Note> = emptyList(),
        val resolved:Boolean = false): Comparable<Comment> {

    fun withProvider(provider: ReviewCommentsProvider):Comment {
        return Comment(provider, revision, line, notes, resolved)
    }

    fun toUpdated(oldNote: Note, note:Note):Comment {
        checkIfResolved()

        val newNotes = notes.toMutableList()
        newNotes.remove(oldNote)
        newNotes.add(note)
        newNotes.sortWith(noteComparator)

        return Comment(provider, revision, line, newNotes, resolved)
    }

    fun toResolved(): Comment {
        checkIfResolved()

        return Comment(provider, revision, line, notes, true)
    }

    private fun checkIfResolved() {
        resolved && throw IllegalStateException("Comment $this is already resolved")
    }

    override fun compareTo(other: Comment): Int {
        val cmp = Integer.compare(line, other.line)
        if (cmp != 0) return cmp

        if (notes.isEmpty() || other.notes.isEmpty())
            return if (notes.isEmpty()) -1 else 1

        return noteComparator.compare(notes[0], other.notes[0])
    }
}
