package com.intellij.idea.reviewcomment.ui;

import javax.swing.*;

import com.intellij.idea.reviewcomment.model.Comment;
import com.intellij.idea.reviewcomment.model.Note;
import com.intellij.idea.reviewcomment.model.ReviewCommentsProvider;
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
                             @NotNull Consumer<Comment> consumer,
                             @NotNull Comment comment,
                             @NotNull Note note) {
        super(project, true);
        final ReviewCommentsProvider[] providers = ReviewCommentsProvider.Companion.getEP_NAME().getExtensions();
        myPanel = new EditCommentPanel(project, providers);
        myNote = note;
        myConsumer = c -> consumer.consume(c);
        myComment = comment;
        myPanel.setNote(comment, note);
        setTitle(note.isNew() ? "Add a review comment" : "Edit a review comment ");
        setOKActionEnabled(providers.length > 0);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel.getContent();
    }

    @Override
    protected void doOKAction() {
        final Note note = myPanel.getNote();
        final Comment comment =
                (myComment.getProvider() == null ? myComment.withProvider(myPanel.getSelectedProvider()) : myComment )
                    .toUpdated(myNote, note);
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

