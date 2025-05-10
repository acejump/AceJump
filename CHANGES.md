# Changelog

## Unreleased

## 3.8.22

- Tags now paint rightwards when left is out of bounds, thanks to [@ivw](https://github.com/ivw)

## 3.8.21

- Relax version upper bound

## 3.8.20

- Add support for 2024.2, fixing [#463](https://github.com/acejump/AceJump/issues/463)
- Speedup tagging when "Search whole file" is disabled, thanks to [@chylex](https://github.com/chylex)

## 3.8.19

- Enable support for 2024.1, fixing ([#457](https://github.com/acejump/AceJump/issues/457))

## 3.8.18

- Disable tagging and jumping to folded regions ([#453](https://github.com/acejump/AceJump/issues/453)), thanks to [@chylex](https://github.com/chylex)
- Update hint styling and show mode ([#394](https://github.com/acejump/AceJump/issues/394)) when "Show hint with search text" is enabled
- Fixes "Char sequence is empty" ([#404](https://github.com/acejump/AceJump/issues/404)) when "Map Unicode to ASCII" is enabled

## 3.8.17

- Add buttons to reset colors to default values in Settings, [#411](https://github.com/acejump/AceJump/issues/411), thanks to [@chylex](https://github.com/chylex)
- Unbundle conflicting Kotlin Standard Library version, [#449](https://github.com/acejump/AceJump/issues/449), thanks to [@chylex](https://github.com/chylex)
- Fix some instances of "Read access not allowed", [#447](https://github.com/acejump/AceJump/issues/447), thanks to [@h0tk3y](https://github.com/h0tk3y)

## 3.8.16

- Fix issue with unselectable tags, [#446](https://github.com/acejump/AceJump/issues/446)

## 3.8.15

- Forbid jumping to offscreen tags, [#442](https://github.com/acejump/AceJump/issues/442)

## 3.8.14

- Fixes NoSuchFieldError: Companion on older platform versions, [#432](https://github.com/acejump/AceJump/issues/432), [#434](https://github.com/acejump/AceJump/issues/434), [#435](https://github.com/acejump/AceJump/issues/432), [#437](https://github.com/acejump/AceJump/issues/437), [#438](https://github.com/acejump/AceJump/issues/438), thanks to [@wuruofan](https://github.com/wuruofan)

## 3.8.13

- Fixes color settings not being persisted, [#431](https://github.com/acejump/AceJump/issues/431)

## 3.8.12

- Fixes tag cycling issue with Enter/Shift+Enter, [#429](https://github.com/acejump/AceJump/issues/429)

## 3.8.11

- Fixes UI issue affecting mode cycling order, [#426](https://github.com/acejump/AceJump/issues/426)

## 3.8.10

- Fixes regression in 3.8.9 breaking cross-tab selection, [#417](https://github.com/acejump/AceJump/issues/417)

## 3.8.9

- Add ids to editor action handlers, [#410](https://github.com/acejump/AceJump/pull/410), thanks to [@AlexPl292](https://github.com/AlexPl292)
- Update API to IJ-2022.3 and JDK to 17

## 3.8.8

- Add AZERTY keyboard layout, [#398](https://github.com/acejump/AceJump/pull/398), thanks to [@delphinaubin](https://github.com/delphinaubin)
- Add bounded toggle mode to start jump mode before or after the caret, [#401](https://github.com/acejump/AceJump/pull/401), thanks to [@colossatr0n](https://github.com/colossatr0n)
- Remove only the highlighters added by AceJump when jump session ends, [#407](https://github.com/acejump/AceJump/pull/407), thanks to [@huoguangjin](https://github.com/huoguangjin)

## 3.8.7

- Fixes Unicode-ASCII regression, [#399](https://github.com/acejump/AceJump/issues/399)

## 3.8.6

- Adds AZERTY keyboard layout, [#398](https://github.com/acejump/AceJump/pull/398), thanks to [@delphinaubin](https://github.com/delphinaubin)

## 3.8.5

- Improves tag order for non-QWERTY layouts, [#385](https://github.com/acejump/AceJump/issues/385)
- Restores <kbd>Tab</kbd>/<kbd>Shift</kbd>+<kbd>Tab</kbd> functionality, [#356](https://github.com/acejump/AceJump/issues/356)
- Fixes tag cycling with <kbd>Enter</kbd>/<kbd>Shift</kbd>+<kbd>Enter</kbd>, [#380](https://github.com/acejump/AceJump/issues/380), thanks [@AlexPl292](https://github.com/AlexPl292)

## 3.8.4

- Fixes Declaration Mode in Rider, [#379](https://github.com/acejump/AceJump/issues/379), thanks to [@igor-akhmetov](https://github.com/igor-akhmetov) for helping diagnose!
- Fixes highlight offset on high-DPI screens, [#362](https://github.com/acejump/AceJump/issues/362), thanks to [@chylex](https://github.com/chylex) for [the PR](https://github.com/acejump/AceJump/pull/384)!

## 3.8.3

- Displays regular expression for regex-based queries
- Fixes a bug when current search text was enabled causing word and line mode tags to not be displayed, [#376](https://github.com/acejump/AceJump/issues/376)

## 3.8.2

- Add option to display current search text, [#375](https://github.com/acejump/AceJump/issues/375)
- Fixes a bug where editor was not focused, [#374](https://github.com/acejump/AceJump/issues/374)
- Thanks to [@SaiKai](https://github.com/SaiKai) for the PRs!

## 3.8.1

- Hotfix for stale cache, [#373](https://github.com/acejump/AceJump/issues/373)

## 3.8.0
- Allow jumping between splitters in the editor, [#371](https://github.com/acejump/AceJump/pull/371)
- Adds support for unicode search and selection, [#368](https://github.com/acejump/AceJump/issues/368)

## 3.7.1
- Fix settings display issue, [#363](https://github.com/acejump/AceJump/issues/363)
- Update AceJump extension API to include tag information, [#357](https://github.com/acejump/AceJump/pull/357)
- Allow defining jump mode with boundaries, [#358](https://github.com/acejump/AceJump/pull/358)
- Use Kotlin classes for actions, [#359](https://github.com/acejump/AceJump/pull/359)
- Thanks to [@AlexPl292](https://github.com/AlexPl292) for the PRs!

## 3.7.0
- Improvements to tag latency
- Redesign settings panel
  - Add missing configuration for definition mode color
  - Adds option to switch between straight and rounded tag corners
  - Adds option to only consider visible area
  - Add customizable jump mode cycling
- Jump-to-End mode jumps to the end of a word
- Fixes toggle keys not resetting mode when pressed twice
- Increase limit for what is considered a large file
- Major refactoring, [#350](https://github.com/acejump/AceJump/pull/353)
- [Many bug fixes](https://github.com/acejump/AceJump/issues/348#issuecomment-739454920): [#338](https://github.com/acejump/AceJump/issues/338), [#336](https://github.com/acejump/AceJump/issues/336), [#329](https://github.com/acejump/AceJump/issues/329), [#327](https://github.com/acejump/AceJump/issues/327), [#310](https://github.com/acejump/AceJump/issues/310), [#233](https://github.com/acejump/AceJump/issues/233), [#228](https://github.com/acejump/AceJump/issues/228), [#187](https://github.com/acejump/AceJump/issues/187), [#147](https://github.com/acejump/AceJump/issues/147), [#132](https://github.com/acejump/AceJump/issues/132), [#71](https://github.com/acejump/AceJump/issues/71)
- Huge thanks to [@chylex](https://github.com/chylex) for [all the PRs](https://github.com/acejump/AceJump/pulls?q=is%3Apr+author%3Achylex)!

## 3.6.3

- Vote for your favorite <a href="https://twitter.com/breandan/status/1274169810411274241">AceJump logo</a>!
- Fixes potential bug.
- Increases test coverage.

## 3.6.2

- Fixes [#226](https://github.com/acejump/AceJump/issues/226). Thanks [@AlexPl292](https://github.com/AlexPl292)!
- Update Pinyin engine.

## 3.6.1

- Fixes [#324](https://github.com/acejump/AceJump/issues/324). Thanks [@AlexPl292](https://github.com/AlexPl292)!
- Fixes [#325](https://github.com/acejump/AceJump/issues/325).
- Fixes Pinyin support.

## 3.6.0

- Adds support for Chinese [#314](https://github.com/acejump/AceJump/issues/314).
- Fixes constantly loading settings page [#303](https://github.com/acejump/AceJump/issues/303).
- Honor camel humps [#315](https://github.com/acejump/AceJump/issues/315). Thanks to [@clojj](https://github.com/clojj).
- Support dynamic application reloading [#322](https://github.com/acejump/AceJump/issues/322).

## 3.5.9

- Fix a build configuration error affecting plugins which depend on AceJump. Fixes [#305](https://github.com/acejump/AceJump/issues/305).

## 3.5.8

- Tagging improvements
- Support for external plugin integration
- Fixes [#304](https://github.com/acejump/AceJump/issues/304), [#255](https://github.com/acejump/AceJump/issues/255)

## 3.5.7

- <kbd>Tab</kbd>/<kbd>Enter</kbd> will now scroll horizontally if results are not visible.
- Fixes [#294](https://github.com/acejump/AceJump/issues/294) "Access is allowed from event dispatch thread only" error

## 3.5.6

- Key prioritization for most common keyboard layouts and fixes for a number of minor issues.
- Fixes: Index OOB [#242](https://github.com/acejump/AceJump/issues/242), Missing editor [#249](https://github.com/acejump/AceJump/issues/249), [#275](https://github.com/acejump/AceJump/issues/275), Forgotten block caret [#278](https://github.com/acejump/AceJump/issues/278), QWERTZ layout [#273](https://github.com/acejump/AceJump/issues/273)

## 3.5.5

- <kbd>Enter</kbd> will now escape exit from AceJump when there is a single visible tag. [#274](https://github.com/acejump/AceJump/issues/274)
- <kbd>Shift</kbd>+<kbd>Tab</kbd> to scroll to previous occurrences now works properly. [#179](https://github.com/acejump/AceJump/issues/179)
- Fixes an error with sticky block caret mode. [#269](https://github.com/acejump/AceJump/issues/269)

## 3.5.4

- Introduces cyclical selection: press Enter or Shift + Enter to cycle through tags on the screen. Press Escape to return to the editor.

## 3.5.3

- Fixes for two regressions affecting caret color and shift-selection.

## 3.5.2

- Various improvements to settings page, including a keyboard layout selector.
- Shorter tags on average, AceJump tries to use a single-character tag more often.
- Tag characters are now prioritized by user-defined order from the settings page.
- Fixes an issue when running the plugin on platform version 2018.3 and above.

## 3.5.1

- Now supports searching for CaPiTaLiZeD letters (typing capital letters in the query will force a case-sensitive search).
- **Declaration Mode**: Press the AceJump shortcut a second time to activate Declaration Mode, which will jump to the declaration of a variable in the editor.
- Keep hitting the AceJump shortcut to cycle between modes (default, declaration, target, disabled).
- Bug fix: AceJump settings should now properly persist after restarting the IDE.

## 3.5.0

- Adds two new features. "**Word-Mode**" and quick tag selection.
- **Word Mode** removes search and addresses latency issues raised in [#161](https://github.com/acejump/AceJump/issues/161). To learn more about **Word Mode**, see the [readme](https://github.com/johnlindquist/AceJump#tips).
- Pressing <kbd>Enter</kbd> during a search will jump to the next visible match (or closest match, if next is not visible), as per [#133](https://github.com/acejump/AceJump/issues/133).

## 3.4.3

- Stability improvements and tagging optimizations. Fixes [#206](https://github.com/acejump/AceJump/issues/206), [#202](https://github.com/acejump/AceJump/issues/202).

## 3.4.2

- Fixes [a regression](https://github.com/johnlindquist/AceJump/issues/197) affecting older platform versions.

## 3.4.1

- Fixes a regression affecting tag alignment when line spacing is greater than 1.0. Minor speed improvements.

## 3.4.0

- Restores original scroll position if tab search cancelled. Minor improvements to latency and tag painting.

## 3.3.6

- Fix for [#129](https://github.com/acejump/AceJump/issues/129).

## 3.3.5

- Minor bugfix release. Improve handling of window resizing.

## 3.3.4

- Add a settings page. (Settings > Tools > AceJump)

## 3.3.3

- Improve latency and fix a bug in line selection mode.

## 3.3.2

- AceJump now persists target mode state when scrolling or tabbing.

## 3.3.1

- Fixes a minor regression where tags are not displaying correctly.

## 3.3.0

- AceJump now searches the entire document. Press TAB to get the next set of results!

## 3.2.7

- Minor fixes and stability improvements.

## 3.2.6

- Fixes an error affecting older versions of the IntelliJ Platform.

## 3.2.5

- AceJump 3 now supports older IntelliJ Platform and Kotlin versions.

## 3.2.4

- Tagging improvements (tags now shorter on average) and visual updates.

## 3.2.3

- Fixes a critical issue affecting users with multiple editor windows open.

## 3.2.2

- Adds scrolling support and fixes some line spacing issues.

## 3.2.1

- AceJump now synchronizes font style changes in real-time.

## 3.2.0

- Support Back/Forward navigation in the IntelliJ Platform.

## 3.1.8

- Fixes some errors that occur when the user closes an editor prematurely.

## 3.1.6

- Fixes a rare tag collision scenario and UninitializedPropertyAccess exception

## 3.1.5

- Allow users to enter target mode directly by pressing Ctrl+Alt+;

## 3.1.4

- Fixes the "Assertion Failed" exception popup

## 3.1.3

- Fixes an error affecting some users during startup.

## 3.1.2

- Fixes an Android Studio regression.

## 3.1.1

- Hotfix for broken target mode.

## 3.1.0

- Removes the search box, lots of small usability improvements.

## 3.0.7

- No longer tags "folded" regions and minor alignment adjustments.

## 3.0.6

- Fixes alignment issues, removes top and bottom alignments until there is a better way to visually differentiate adjacent tags.

## 3.0.5

- Hotfix for target mode.

## 3.0.4

- Adds *Line Mode* - press <kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>;</kbd> to activate.

## 3.0.3

- Updates to tag placement and performance improvements.

## 3.0.2

- Fixes target mode and default shortcut activation for Mac users.

## 3.0.1

- Fixes target-mode issues affecting users with non-default shortcuts and adds support for Home/End.

## 3.0.0

- Major rewrite of AceJump. Introducing:
    * Realtime search: Just type the word where you want to jump and AceJump will do the rest.
    * Smart tag placement: Tags now occupy nearby whitespace if available, rather than block text.
    * Keyboard-aware tagging: Tries to minimize finger travel distance on QWERTY keyboards.
    * Colorful highlighting: AceJump will now highlight the editor text, as you type.

## 2.0.13

- Fix a regression affecting *Target Mode* and line-based navigation.

## 2.0.12

- Fix ClassCastException when input letter not present: [#73](https://github.com/acejump/AceJump/issues/73)

## 2.0.11

- One hundred percent all natural Kotlin.

## 2.0.10

- Support 2016.2, remove upper version limit, update internal Kotlin version

## 2.0.9

- Compile on Java 7 to address: [#61](https://github.com/acejump/AceJump/issues/61)

## 2.0.8

- Compile on Java 6 to address: [#59](https://github.com/acejump/AceJump/issues/59)

## 2.0.7

- Language update for Kotlin 1.0 release.

## 2.0.6

- Fixing "lost focus" bugs mentioned here: [#41](https://github.com/acejump/AceJump/issues/41)

## 2.0.5

- Fixing "backspace" bugs mentioned here: [#20](https://github.com/acejump/AceJump/issues/20)

## 2.0.4

- Fixing "code folding" bugs mentioned here: [#24](https://github.com/acejump/AceJump/issues/24)

## 2.0.3

- More work on Ubuntu focus bug

## 2.0.2

- Fixed bug when there's only 1 search result

## 2.0.1

- Fixing Ubuntu focus bug

## 2.0.0

- Major release: Added "target mode", many speed increases, multi-char search implemented

## 1.1.0

- Switching to Kotlin for the code base

## 1.0.4

- Fixing [#9](https://github.com/acejump/AceJump/issues/9) and [#6](https://github.com/acejump/AceJump/issues/6)

## 1.0.3

- Fixed minor visual lag when removing the "jumpers" from the editor

## 1.0.2

- Cleaning up minor bugs (npe when editor not in focus, not removing layers)

## 1.0.1

- Adding a new jump: "Enter" will take you to the first non-whitespace char in a new line (compare to "Home" which takes you to a new line)

## 1.0.0

- Cleaned up code base for release
