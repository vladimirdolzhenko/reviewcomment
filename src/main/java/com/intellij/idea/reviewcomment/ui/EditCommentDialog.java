package com.intellij.idea.reviewcomment.ui;

import javax.swing.*;

import java.time.Instant;

import com.intellij.idea.reviewcomment.model.Comment;
import com.intellij.idea.reviewcomment.model.Note;
import com.intellij.idea.reviewcomment.model.ReviewCommentsRepository;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

public class EditCommentDialog extends DialogWrapper {
    private final EditCommentPanel myPanel;
    private final Consumer<Comment> myConsumer;
    private final Comment myComment;
    private final Note myNote;

    public EditCommentDialog(@NotNull Project project,
                             @NotNull ReviewCommentsRepository repository,
                             @NotNull Consumer<Comment> consumer,
                             @NotNull Comment comment,
                             @NotNull Note note) {
        super(project, true);
        myPanel = new EditCommentPanel(project, repository);
        myNote = note;
        myConsumer = c -> {
            consumer.consume(c);
            // is there any way to recalculate gutter items ?
            //DaemonCodeAnalyzer.getInstance(project).restart(psiFile);
        };
        myComment = comment;
        myPanel.setNote(note);
        final boolean sameUser = repository.getCurrentUser().equals(note.getAuthor());
        setTitle(sameUser
                ? note.isNew() ? "Add a review comment" : "Edit a review comment "
                : "View a review comment");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel.getContent();
    }

    @Override
    protected void doOKAction() {
        final String commentText = myPanel.getComment();
        final Instant timestamp = myNote.getTimestamp() != null ? myNote.getTimestamp() : Instant.now();
        final Note note = new Note(timestamp, myNote.getAuthor(), commentText);
        final Comment comment = myComment.toUpdated(myNote, note);
        myConsumer.consume(comment);
        super.doOKAction();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myPanel.getPreferredFocusedComponent();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "ReviewComment.EditCommentDialog";
    }
}

