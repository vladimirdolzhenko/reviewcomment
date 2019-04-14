package com.intellij.idea.reviewcomment.ui;

import com.intellij.idea.reviewcomment.model.ReviewCommentsRepository;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorGutter;
import com.intellij.openapi.localVcs.UpToDateLineNumberProvider;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl;
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReviewCommentsAction extends ToggleAction implements DumbAware {
    private final ReviewCommentsRepository repository = new ReviewCommentsRepository();
    private final Map<VirtualFile, FileAnnotation> annotatedFiles = new HashMap<>();

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        return annotatedFiles.containsKey(virtualFile);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final EditorGutter gutter = editor.getGutter();
        if (state && !annotatedFiles.containsKey(virtualFile)) {
            final UpToDateLineNumberProvider upToDateLineNumbers =
                    new UpToDateLineNumberProviderImpl(editor.getDocument(), project);

            final AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(virtualFile);

            final AnnotationProvider annotationProvider = activeVcs.getAnnotationProvider();

            try {
                final FileAnnotation fileAnnotation = annotationProvider.annotate(virtualFile);
                final VcsRevisionNumber vcsRevisionNumber = fileAnnotation.getCurrentRevision();

                final Ref<Boolean> newCommentsUpdated = new Ref<>();
                final Ref<VcsException> exceptionRef = new Ref<>();

                final Task.Backgroundable refreshTask = new Task.Backgroundable(project,
                        "Loading review comments [ " + virtualFile.getName() + " ]", true) {
                    @Override
                    public void run(final @NotNull ProgressIndicator indicator) {
                        try {
                            repository.refresh(project, virtualFile);
                            newCommentsUpdated.set(Boolean.TRUE);
                        } catch (Exception e) {
                            exceptionRef.set(new VcsException(e));
                        }
                    }

                    @Override
                    public void onCancel() {
                        onSuccess();
                    }

                    @Override
                    public void onSuccess() {
                        if (!exceptionRef.isNull()) {
                            AbstractVcsHelper.getInstance(project).showErrors(
                                    Collections.singletonList(exceptionRef.get()),
                                    VcsBundle.message("message.title.annotate"));
                        }

                        if (!newCommentsUpdated.isNull()) {
                            final MyActiveAnnotationGutter annotationGutter =
                                    new MyActiveAnnotationGutter(project, repository, virtualFile, vcsRevisionNumber);

                            final AnnotationGutterLineConvertorProxy proxy =
                                    new AnnotationGutterLineConvertorProxy(upToDateLineNumbers, annotationGutter);
                            gutter.registerTextAnnotation(proxy, proxy);
                            annotatedFiles.put(virtualFile, fileAnnotation);
                        }
                    }
                };
                ProgressManager.getInstance().run(refreshTask);

                fileAnnotation.setCloser(() -> UIUtil.invokeLaterIfNeeded(() -> {
                    annotatedFiles.remove(virtualFile);
                    if (project.isDisposed()) return;
                    gutter.closeAllAnnotations();
                }));

            } catch (VcsException ex) {
                VcsBalloonProblemNotifier.showOverVersionControlView(project,
                        "failed to annotate file " + virtualFile.getPath()
                                + " from vcs\n" + ex.getMessage(), MessageType.ERROR);
            }
        } else {
            final FileAnnotation fileAnnotation = annotatedFiles.remove(virtualFile);
            if (fileAnnotation != null) {
                fileAnnotation.close();
            }
        }
    }

}
