package com.intellij.idea.reviewcomment.model

import com.google.common.io.Files
import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class SimpleReviewCommentsProviderTest {

    lateinit var project:Project
    lateinit var projectRootFile:MockVirtualFile
    lateinit var provider:SimpleReviewCommentsProvider

    @Before
    fun setup() {
        project = mock(Project::class.java)
        Mockito.`when`(project.name).thenReturn("reviewCommentTest")
        provider = SimpleReviewCommentsProvider()
    }

    private fun populateRoot(projectRootFile: MockVirtualFile, path: String):VirtualFile {
        var p = projectRootFile
        for (item in path.split(VFS_SEPARATOR_CHAR)) {
            val f = MockVirtualFile(item)
            p.addChild(f)
            p = f
        }
        return p
    }

    private fun populateWorkspaceFile() {
        val path = "project.iws"
        val projectFile = populateRoot(projectRootFile, path)
        Mockito.`when`(project.workspaceFile).thenReturn(projectFile)
        Mockito.`when`(project.basePath).thenReturn(projectRootFile.name)
    }

    @Test
    fun `get current user`() {
        assertEquals("me", provider.getCurrentUser())
    }

    @Test
    fun `load comments data for non commented file`() {
        projectRootFile = MockVirtualFile("someproject")
        populateWorkspaceFile()

        val someFile = populateRoot(projectRootFile, "src/java/q/A.java")
        val comments = provider.getComments(project, someFile)
        assertTrue { comments.isEmpty() }
    }

    @Test
    fun `load comments from existed file`() {
        val resource = SimpleReviewCommentsProviderTest::class.java.getResource("/comments.json")
        projectRootFile = MockVirtualFile(File(resource.file).parent)
        populateWorkspaceFile()

        val someFile = populateRoot(projectRootFile, "demos/src/main/java/com/ObjectReallocation.java")
        val comments = provider.getComments(project, someFile)
        assertEquals(2, comments?.size)

        val firstComment = comments?.first()
        assertEquals(24, firstComment?.line)
        assertEquals(2, firstComment?.notes?.size)
        assertEquals("xxx", firstComment?.notes?.get(0)?.comment)
        assertEquals("xxx2", firstComment?.notes?.get(1)?.comment)

        val lastComment = comments?.last()
        assertEquals(30, lastComment?.line)
        assertEquals(1, lastComment?.notes?.size)
        assertEquals("wtf", lastComment?.notes?.get(0)?.comment)
    }

    @Test
    fun `save and load comments`() {
        val tempDir = Files.createTempDir()
        projectRootFile = MockVirtualFile(tempDir!!.absolutePath)
        populateWorkspaceFile()

        val comment = Comment(provider, "3414123", 10,
                listOf(Note(Instant.now(), "vd", "a simple comment")))

        val someFile = populateRoot(projectRootFile, "src/main/java/com/Foo.java")
        provider.updateComment(project, someFile, null, comment){}

        // load from another provider
        val provider2 = SimpleReviewCommentsProvider()

        val comments = provider2.getComments(project, someFile)
        assertEquals(1, comments?.size)

        val firstComment = comments?.first()
        assertEquals(comment.revision, firstComment.revision)
        assertEquals(comment.line, firstComment.line)
        assertEquals(comment.notes, firstComment.notes)
        assertEquals(comment.resolved, firstComment.resolved)
    }

}