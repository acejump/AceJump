# Changelog

### 3.4.1

Fixes a regression affecting tag alignment when line spacing is greater than 1.0.

### 3.4.0

Restores original scroll position if tab search cancelled. Minor improvements to latency and tag painting.

### 3.3.6

Fix for https://github.com/johnlindquist/AceJump/issues/129.

### 3.3.5

Minor bugfix release. Improve handling of window resizing.

### 3.3.4

Add a settings page. (Settings > Tools > AceJump)

### 3.3.3

Improve latency and fix a bug in line selection mode.

### 3.3.2

AceJump now persists target mode state when scrolling or tabbing.

### 3.3.1

Fixes a minor regression where tags are not displaying correctly.

### 3.3.0

AceJump now searches the entire document. Press TAB to get the next set of results!

### 3.2.7

Minor fixes and stability improvements.

### 3.2.6

Fixes an error affecting older versions of the IntelliJ Platform.

### 3.2.5

AceJump 3 now supports older IntelliJ Platform and Kotlin versions.

### 3.2.4

Tagging improvements (tags now shorter on average) and visual updates.

### 3.2.3

Fixes a critical issue affecting users with multiple editor windows open.

### 3.2.2

Adds scrolling support and fixes some line spacing issues.

### 3.2.1

AceJump now synchronizes font style changes in real-time.

### 3.2.0

Support Back/Forward navigation in the IntelliJ Platform.

### 3.1.8

Fixes some errors that occur when the user closes an editor prematurely.

### 3.1.6

Fixes a rare tag collision scenario and UninitializedPropertyAccess exception

### 3.1.5

Allow users to enter target mode directly by pressing Ctrl+Alt+;

### 3.1.4

Fixes the "Assertion Failed" exception popup

### 3.1.3

Fixes an error affecting some users during startup.

### 3.1.2

Fixes an Android Studio regression.

### 3.1.1

Hotfix for broken target mode.

### 3.1.0

Removes the search box, lots of small usability improvements.

### 3.0.7

No longer tags "folded" regions and minor alignment adjustments.

### 3.0.6

Fixes alignment issues, removes top and bottom alignments until there is a better way to visually differentiate adjacent tags.

### 3.0.5

Hotfix for target mode.

### 3.0.4

Adds "line mode" - press [Ctrl+Shift+;] to activate.

### 3.0.3

Updates to tag placement and performance improvements.

### 3.0.2

Fixes target mode and default shortcut activation for Mac users.

### 3.0.1

Fixes target-mode issues affecting users with non-default shortcuts and adds support for Home/End.

### 3.0.0

Major rewrite of AceJump. Introducing:

* Realtime search: Just type the word where you want to jump and AceJump will do the rest.
* Smart tag placement: Tags now occupy nearby whitespace if available, rather than block text.
* Keyboard-aware tagging: Tries to minimize finger travel distance on QWERTY keyboards.
* Colorful highlighting: AceJump will now highlight the editor text, as you type.

### 2.0.13

Fix a regression affecting *Target Mode* and line-based navigation: https://github.com/johnlindquist/AceJump/commit/cc3a23a3bd6754d11100f15f3dddc4d8529926df#diff-a483c757116bde46e566a8b01520a807L51</dd>

### 2.0.12

Fix ClassCastException when input letter not present: https://github.com/johnlindquist/AceJump/issues/73

### 2.0.11

One hundred percent all natural Kotlin.

### 2.0.10

Support 2016.2, remove upper version limit, update internal Kotlin version

### 2.0.9

Compile on Java 7 to address: https://github.com/johnlindquist/AceJump/issues/61

### 2.0.8

Compile on Java 6 to address: https://github.com/johnlindquist/AceJump/issues/59

### 2.0.7

Language update for Kotlin 1.0 release.

### 2.0.6

Fixing "lost focus" bugs mentioned here: https://github.com/johnlindquist/AceJump/issues/41

### 2.0.5

Fixing "backspace" bugs mentioned here: https://github.com/johnlindquist/AceJump/issues/20

### 2.0.4

Fixing "code folding" bugs mentioned here: https://github.com/johnlindquist/AceJump/issues/24

### 2.0.3

More work on Ubuntu focus bug

### 2.0.2

Fixed bug when there's only 1 search result

### 2.0.1

Fixing Ubuntu focus bug

### 2.0.0

Major release: Added "target mode", many speed increases, multi-char search implemented

### 1.1.0

Switching to Kotlin for the code base

### 1.0.4

Fixing https://github.com/johnlindquist/AceJump/issues/9 and https://github.com/johnlindquist/AceJump/issues/6

### 1.0.3

Fixed minor visual lag when removing the "jumpers" from the editor

### 1.0.2

Cleaning up minor bugs (npe when editor not in focus, not removing layers)

### 1.0.1

Adding a new jump: "Enter" will take you to the first non-whitespace char in a new line (compare to "Home" which takes you to a new line)

### 1.0.0

Cleaned up code base for release
