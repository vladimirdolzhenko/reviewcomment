/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.idea.reviewcomment.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

import com.intellij.idea.reviewcomment.model.Note;
import com.intellij.idea.reviewcomment.model.ReviewCommentsRepository;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;

public class EditCommentPanel extends JPanel {
  private final JLabel myCommentLabel;
  private final JLabel myTimestampLabel;
  private final EditorTextField myCommentTextArea;
  private final JPanel myAdditionalControlsPanel;

  private final Project myProject;
  private final ReviewCommentsRepository myCommentsRepository;

  public EditCommentPanel(final Project project, ReviewCommentsRepository commentsRepository) {
    super(new GridBagLayout());
    myProject = project;
    myCommentsRepository = commentsRepository;
    final GridBagConstraints gb =
        new GridBagConstraints(0, 0, 1, 1, 0, 0,
            GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
            new Insets(1, 1, 1, 1), 0, 0);

    gb.weightx = 0;
    gb.fill = GridBagConstraints.NONE;

    final JLabel timestampLabel = new JLabel("Timestamp:");
    add(timestampLabel, gb);
    ++ gb.gridx;
    gb.weightx = 1;
    gb.fill = GridBagConstraints.HORIZONTAL;
    myTimestampLabel = new JLabel("timestamp");
    add(myTimestampLabel, gb);

    ++ gb.gridy;
    gb.gridx = 0;
    gb.weightx = 0;
    gb.fill = GridBagConstraints.NONE;
    gb.anchor = GridBagConstraints.NORTHWEST;

    myCommentLabel = new JLabel("Author");
    add(myCommentLabel, gb);
    ++ gb.gridx;
    gb.weightx = 1;
    gb.weighty = 1;
    gb.fill = GridBagConstraints.BOTH;
    myCommentTextArea = createEditorField(project, 4);
    myCommentTextArea.setOneLineMode(false);
    add(myCommentTextArea, gb);
    myCommentLabel.setLabelFor(myCommentTextArea);

    ++ gb.gridy;
    gb.gridx = 0;
    gb.gridwidth = 2;
    gb.weighty = 0;
    myAdditionalControlsPanel = new JPanel();
    final BoxLayout layout = new BoxLayout(myAdditionalControlsPanel, BoxLayout.X_AXIS);
    myAdditionalControlsPanel.setLayout(layout);

    setPreferredSize(new Dimension(300, 150));
    setMinimumSize(new Dimension(300, 150));
  }

  public void setNote(Note note) {
    myCommentTextArea.setText(note.getComment());
    myTimestampLabel.setText(note.getFormattedTimestamp());
    myCommentLabel.setText(note.getAuthor() + ":");

    final boolean sameUser = myCommentsRepository.getCurrentUser().equals(note.getAuthor());
    myCommentTextArea.setEnabled(sameUser);
  }

  public String getComment() {
    return myCommentTextArea.getText();
  }

  public JComponent getContent() {
    return this;
  }

  public void requestFocus() {
    myCommentTextArea.requestFocus();
  }

  public JComponent getPreferredFocusedComponent() {
    return myCommentTextArea;
  }

  private static EditorTextField createEditorField(final Project project, final int defaultLines) {
    final EditorTextFieldProvider service = ServiceManager.getService(project, EditorTextFieldProvider.class);
    final EditorTextField editorField = service.getEditorField(FileTypes.PLAIN_TEXT.getLanguage(),
        project, Collections.emptyList());
    final int height = editorField.getFontMetrics(editorField.getFont()).getHeight();
    editorField.getComponent().setMinimumSize(new Dimension(100, (int)(height * 1.3)));
    return editorField;
  }
}