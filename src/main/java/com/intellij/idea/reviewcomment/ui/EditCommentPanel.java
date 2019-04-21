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
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import com.intellij.idea.reviewcomment.model.Comment;
import com.intellij.idea.reviewcomment.model.Note;
import com.intellij.idea.reviewcomment.model.ReviewCommentsProvider;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;

public class EditCommentPanel extends JPanel {
  private final JLabel myCommentLabel;
  private final JLabel myTimestampLabel;
  private final EditorTextField myCommentTextArea;
  private final JPanel myAdditionalControlsPanel;

  private final Project myProject;
  private final CollectionComboBoxModel<ReviewCommentsProvider> myProviderComboModel;
  private final ComboBox<ReviewCommentsProvider> myProviderCombobox;
  private Note myNote;

  public EditCommentPanel(final Project project, ReviewCommentsProvider[] providers) {
    super(new GridBagLayout());
    myProject = project;
    final GridBagConstraints gb =
        new GridBagConstraints(0, 0, 1, 1, 0, 0,
            GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
            new Insets(1, 1, 1, 1), 0, 0);

    gb.weightx = 0;
    gb.fill = GridBagConstraints.NONE;

    final JLabel providerLabel = new JLabel("Review source:");
    add(providerLabel, gb);
    ++ gb.gridx;
    gb.weightx = 1;
    gb.fill = GridBagConstraints.HORIZONTAL;

    myProviderComboModel = new CollectionComboBoxModel(Arrays.asList(providers));
    myProviderComboModel.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        contentsChanged(e);
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
        contentsChanged(e);
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
        updateAuthor();
      }
    });

    myProviderCombobox = new ComboBox<>(myProviderComboModel);
    myProviderCombobox.setEditable(false);
    myProviderCombobox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
        final ReviewCommentsProvider provider = (ReviewCommentsProvider) value;
        final String name = provider != null ? provider.getName() : "";
        return super.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
      }
    });
    add(myProviderCombobox, gb);

    ++ gb.gridy;
    gb.gridx = 0;
    gb.weightx = 0;
    gb.fill = GridBagConstraints.NONE;
    gb.anchor = GridBagConstraints.NORTHWEST;

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

  public void setNote(Comment comment, Note note) {
    myNote = note;
    myCommentTextArea.setText(note.getComment());
    myTimestampLabel.setText(note.getFormattedTimestamp());
    if (!note.isNew()) {
      myProviderComboModel.setSelectedItem(comment.getProvider());
      myProviderCombobox.setEnabled(false);
    } else if (myProviderComboModel.getSize() > 0) {
      // TODO: it has to be preferred provider per project settings
      myProviderComboModel.setSelectedItem(myProviderComboModel.getElementAt(0));
    }
    updateAuthor();
  }

  private void updateAuthor() {
    final ReviewCommentsProvider selectedProvider = getSelectedProvider();
    if (selectedProvider == null || myNote == null) return;

    final String author = myNote.getAuthor();
    final String currentUser = selectedProvider.getCurrentUser();
    if (author != null) {
      myCommentLabel.setText(author + ":");
      final boolean sameUser = currentUser.equals(author);
      myCommentTextArea.setEnabled(sameUser);
    } else {
      myCommentLabel.setText(currentUser + ":");
    }
  }

  public ReviewCommentsProvider getSelectedProvider() {
    return myProviderComboModel.getSelected();
  }

  public Note getNote() {
    final ReviewCommentsProvider selectedProvider = getSelectedProvider();
    if (selectedProvider == null) {
      return null;
    }
    final Instant timestamp = myNote.isNew() ? Instant.now() : myNote.getTimestamp();
    final Note note = new Note(timestamp, selectedProvider.getCurrentUser(), myCommentTextArea.getText());
    return note;
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