package com.intellij.idea.reviewcomment.model

import java.lang.IllegalStateException
import java.time.Instant

import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

data class Note(val timestamp: Instant?,
                val author: String,
                val comment: String) {

    fun getFormattedTimestamp(): String {
        return timestamp?.let { dateTimeFormatter.format(it) } ?: "now"
    }

    fun isNew(): Boolean = timestamp == null

     fun compareTo(other: Note): Int {
        val cmp = when {
            timestamp == null && other.timestamp == null -> 0
            timestamp == null && other.timestamp != null -> -1
            timestamp != null && other.timestamp == null -> 1
            else -> timestamp!!.compareTo(other.timestamp)
        }
        return when (cmp) {
            0 -> author.compareTo(other.author)
            else -> cmp
        }
    }
}

private val noteComparator = object : Comparator<Note> {
    override fun compare(a: Note, b: Note): Int {
        val cmp = when {
            a.timestamp == null && b.timestamp == null -> 0
            a.timestamp == null && b.timestamp != null -> -1
            a.timestamp != null && b.timestamp == null -> 1
            else -> a.timestamp!!.compareTo(b.timestamp)
        }
        return when (cmp) {
            0 -> a.author.compareTo(b.author)
            else -> cmp
        }
    }
}

data class Comment(val revision: String,
                   val line:Int,
                   val notes:List<Note>,
                   val resolved:Boolean = false): Comparable<Comment> {

    fun toUpdated(oldNote: Note, note:Note):Comment {
        checkIfResolved()

        val newNotes = notes.toMutableList()
        newNotes.remove(oldNote)
        newNotes.add(note)
        newNotes.sortWith(noteComparator)

        return Comment(revision, line, newNotes, resolved)
    }

    fun toResolved(): Comment {
        checkIfResolved()

        return Comment(revision, line, notes, true)
    }

    private fun checkIfResolved() {
        resolved && throw IllegalStateException("Comment $this is already resolved")
    }

    override fun compareTo(other: Comment): Int {
        val cmp = Integer.compare(line, other.line)
        if (cmp != 0) return cmp

        if (notes.isEmpty() || other.notes.isEmpty())
            return if (notes.isEmpty()) -1 else 1

        return notes[0].compareTo(other.notes[0])
    }
}
