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

To get review comments project has to have VCS enabled.

You can comment multiple lines (a just single line) with a right click on the line :

![](https://raw.githubusercontent.com/vladimirdolzhenko/reviewcomment/master/images/popup.png?raw=true)


Select a review source (if there are multiple):

![](https://raw.githubusercontent.com/vladimirdolzhenko/reviewcomment/master/images/new_comment_editor.png?raw=true)


Or edit existed:

![](https://raw.githubusercontent.com/vladimirdolzhenko/reviewcomment/master/images/comment_editor.png?raw=true)

Quick comments overview:

![](https://raw.githubusercontent.com/vladimirdolzhenko/reviewcomment/master/images/tooltip.png?raw=true)

Or open comments for edit/view via popup menu:

![](https://raw.githubusercontent.com/vladimirdolzhenko/reviewcomment/master/images/comments_popup.png?raw=true)


There is a highlight for multiline comment. Comment that has more than one note looks a bit different to a single note comment:

![](https://raw.githubusercontent.com/vladimirdolzhenko/reviewcomment/master/images/review_gutter_icons.png?raw=true)


## Known issues

* non VCS files lead to mislead error message
* (*TBD*) No aggregation on comments for previous revisions
* (*TBD*) No setting to turn off/on (always on) review comments
* It is not clear how to handle abandoned comments (line has been commented but dropped during further development)
