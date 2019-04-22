package com.intellij.idea.reviewcomment.model

import org.junit.Test
import java.lang.IllegalStateException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

class CommentTest {
    @Test
    fun newNote() {
        val newNote = Note()
        assertNull(newNote.author)

        try {
            Note(timestamp = Instant.ofEpochMilli(1L))
            fail("invalid note")
        } catch (e: IllegalStateException) {
            // ok
        }

        try {
            Note(author = "me")
            fail("invalid note")
        } catch (e: IllegalStateException) {
            // ok
        }
    }

    @Test
    fun commentCompare() {
        val initialComment1 = Comment(revision = "rev1", line = 5,
                notes = listOf(Note(comment = "a"), Note(comment = "a")))

        val initialComment2 = Comment(revision = "rev2", line = 5,
                notes = listOf(Note(comment = "a"), Note(comment = "a")))

        assertEquals(0, initialComment1.compareTo(initialComment2))
        assertEquals(0, initialComment2.compareTo(initialComment1))
    }

    @Test
    fun simple() {
        val initialComment = Comment(revision = "rev1", line = 5)

        assertEquals(0, initialComment.notes.size)
        assertEquals(false, initialComment.resolved)

        val comment1 = initialComment.toUpdated(Note(comment = ""),
                Note(comment = "xxx1"))

        assertEquals(1, comment1.notes.size)

        val comment2 = comment1.toUpdated(Note(comment = ""),
                Note(comment = "xxx2"))

        assertEquals(2, comment2.notes.size)
        assertEquals("xxx1", comment2.notes[0].comment)
        assertEquals("xxx2", comment2.notes[1].comment)

        val comment3 = comment2.toUpdated(Note(comment = ""),
                Note(Instant.ofEpochMilli(1L), "me", "commited msg"))

        assertEquals(3, comment3.notes.size)
        assertEquals("xxx1", comment3.notes[0].comment)
        assertEquals("xxx2", comment3.notes[1].comment)
        assertEquals("commited msg", comment3.notes[2].comment)
        assertEquals(false, comment3.resolved)

        val comment4 = comment3.toUpdated(Note(Instant.ofEpochMilli(1L), "me", "commited msg"),
                Note(Instant.ofEpochMilli(1L), "me", "committed msg (typo)"))

        assertEquals(3, comment4.notes.size)
        assertEquals("xxx1", comment4.notes[0].comment)
        assertEquals("xxx2", comment4.notes[1].comment)
        assertEquals("committed msg (typo)", comment4.notes[2].comment)
        assertEquals(false, comment4.resolved)

        val finalComment = comment4.toResolved()
        assertEquals(3, finalComment.notes.size)
        assertEquals("xxx1", finalComment.notes[0].comment)
        assertEquals("xxx2", finalComment.notes[1].comment)
        assertEquals("committed msg (typo)", finalComment.notes[2].comment)
        assertEquals(true, finalComment.resolved)


        try {
            finalComment.toUpdated(Note(null, "", ""),
                    Note(Instant.ofEpochMilli(1L), "me", "committed msg"))
            fail("final comment is resolved")
        } catch (e: IllegalStateException) {
            // ok
        }
    }
}