## About

PoC of IDEA plugin to perform code review: viewing and entering github "line notes"

(see [IDEA-64794](https://youtrack.jetbrains.com/issue/IDEA-64794) )

A simple implementation stores comments as json file in a project folder (under `.idea` folder).

#### Extensions
Review comments provider:
```
<extensions defaultExtensionNs="com.intellij">
    <idea.reviewcomment.model.ReviewCommentsProvider
            implementation="..."/>
</extensions>
```

Review comments notifier:
```
<extensions defaultExtensionNs="com.intellij">
    <idea.reviewcomment.model.ReviewCommentNotifier
            implementation="..."/>
</extensions>
```


## Overview

To get review comments project has to have VCS enabled. To enable review comment
you need to click on a gutter (to the left of editor) and turn them on:

![](https://raw.githubusercontent.com/vladimirdolzhenko/reviewcomment/master/images/enable_review_comments?raw=true)

After that you can click on gutter and new comment editor will be opened :
![](https://raw.githubusercontent.com/vladimirdolzhenko/reviewcomment/master/images/new_comment_editor.png?raw=true)

Or edit existed
![](https://raw.githubusercontent.com/vladimirdolzhenko/reviewcomment/master/images/comment_editor.png?raw=true)

Another way to add a new comment is to use pop-up menu that appears on a right
click (menu has also shows existed comment notes):

![](https://raw.githubusercontent.com/vladimirdolzhenko/reviewcomment/master/images/popup.png?raw=true)

_Note_: a single comment appears as `!!` while multiple comments are shown as `!!!`

![](https://raw.githubusercontent.com/vladimirdolzhenko/reviewcomment/master/images/tooltip.png?raw=true)

## Known issues

* (*TBD*) No aggregation on comments for previous revisions
* When _review comments_ gutter is opened it is shown that _Annotate_ is **on** (see [AnnotateVcsVirtualFileAction](https://github.com/JetBrains/intellij-community/blob/191.6707/platform/vcs-impl/src/com/intellij/openapi/vcs/actions/AnnotateVcsVirtualFileAction.java#L82))
* It is not clear how to handle abandoned comments (line has been commented but dropped during further development)
* It is not available (so far) to have review comments gutter turned on for all files