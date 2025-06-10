<p align="center"><a href="https://plugins.jetbrains.com/plugin/7086"> <img src="logo.png" alt="AceJumpLogo"></a></p>

<p align="center">
 	<a href="https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub" title="JetBrains on GitHub"><img src="https://jb.gg/badges/team.svg"></a>
 	<a href="https://github.com/acejump/AceJump/actions/workflows/build.yml" title="Build Plugin"><img src="https://github.com/acejump/AceJump/actions/workflows/build.yml/badge.svg"></a>
 	<a href="https://plugins.jetbrains.com/plugin/7086-acejump" title="JetBrains Plugin"><img src="https://img.shields.io/jetbrains/plugin/v/7086-acejump.svg"></a>
 	<a href="LICENSE" title="License"><img src="https://img.shields.io/badge/License-GPL%20v3-blue.svg"></a>
 	<a href="https://twitter.com/search?q=AceJump&f=live" title="Twitter"><img src="https://img.shields.io/twitter/url/http/shields.io.svg?style=social"></a>
</p>

[AceJump](https://plugins.jetbrains.com/plugin/7086) is a plugin for the [IntelliJ Platform](https://github.com/JetBrains/intellij-community/) that lets you jump to any symbol in the editor with just a few keystrokes. Press the keyboard shortcut for `AceAction` (<kbd>Ctrl</kbd>+<kbd>;</kbd> by default) to activate AceJump. Type any string in the editor, followed by one of the illustrated tags, to jump its position:

![](https://cloud.githubusercontent.com/assets/175716/20177444/124fb534-a74d-11e6-8912-1d220ae27091.png)

Press the AceJump shortcut a second time to activate **Declaration Mode**, which is equivalent to the `Navigate To` action in the IDE. Press the AceJump shortcut three times before completing a tag to activate **Target Mode**. Once **Target Mode** is activated, jumping to a tag will select an entire word. **Target Mode** can also be activated directly by pressing the shortcut for `AceTargetAction` (<kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>;</kbd> by default).

![](https://cloud.githubusercontent.com/assets/175716/20177362/a9976398-a74c-11e6-955d-df029c7b329b.png)

Press the AceJump shortcut for **Line Mode**(<kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>;</kbd> by default), to target the beginning, first non-whitespace, and last character of every line in the editor). Then jump to one by completing the tag.

![](https://cloud.githubusercontent.com/assets/175716/20533565/f7d04d1e-b0ab-11e6-8b89-f7b10a98752d.png)

Press the AceJump shortcut, followed by <kbd>→</kbd> to target the last, <kbd>←</kbd> to target the first, or <kbd>↑</kbd>, to target the first non-whitespace characters of every line in the editor.

![](https://cloud.githubusercontent.com/assets/175716/20177472/4f0ba956-a74d-11e6-97ba-b296eacdd396.png)

AceJump supports other modes as well (e.g. **Jump End Mode**), you can explore the action options under **Settings | Plugins | Browse Repositories... | 🔍 "AceJump"**.

![](https://github.com/user-attachments/assets/7ede3fcb-7b64-4501-beb9-e9929f647558)

## Tips

- Press <kbd>Tab</kbd> when searching to jump to the next group of matches in the editor.

- If you make a mistake searching, just press <kbd>Backspace</kbd> to restart from scratch.

- If no matches can be found on-screen, AceJump will scroll to the next match it can find.
  
- Note that search is [smart case](http://ideavim.sourceforge.net/vim/usr_27.html#vim.27%2E1) sensitive, however tag selection is *not* case sensitive.

- Holding down <kbd>Shift</kbd> when typing the last tag character will select all text from the current cursor position to that destination.

- Pressing <kbd>Enter</kbd> or <kbd>Shift</kbd>+<kbd>Enter</kbd> during a search will cycle through tagged results on screen.

  - To select a location and continue editing, just press <kbd>Esc</kbd>.

  - To use this feature with IdeaVim, you must be in Vim's Insert Mode (to be fixed at a later point).

- Keep typing! AceJump will accept multiple sequential characters before tag selection.

- Press the AceJump shortcut multiple times to cycle between modes.

- **Word Mode** action that will tag all visible words as soon as it is activated.

- **Declaration Mode** will jump to a token's declaration, if it exists.

- To rebind any keyboard shortcuts visit **Settings | Keymap | 🔍 "AceJump"**

## Installing

AceJump can be [installed directly from the IDE](https://www.jetbrains.com/help/idea/managing-plugins.html#install), via **Settings | Plugins | Browse Repositories... | 🔍 "AceJump"**.

## Configuring

[IdeaVim](https://plugins.jetbrains.com/plugin/164) users can choose to activate AceJump with a single keystroke (<kbd>f</kbd>, <kbd>F</kbd> and <kbd>g</kbd> are arbitrary) by running:

```
echo -e '

" Press `f` to activate AceJump
map f <Action>(AceAction)
" Press `F` to activate Target Mode
map F <Action>(AceTargetAction)
" Press `g` to activate Line Mode
map g <Action>(AceLineAction)

' >> ~/.ideavimrc
```

To customize AceJump's behavior further with additional actions, see the `<action>` tags in [plugin.xml](src/main/resources/META-INF/plugin.xml). The following example shows how to activate AceJump before or after the caret.

```
" Press `S` in normal mode to activate AceJump mode before the caret
nmap S <Action>(AceBackwardAction)

" Press `s` in normal mode to activate AceJump mode after the caret
nmap s <Action>(AceForwardAction)
```

To change the default keyboard shortcuts, open **File \| Settings \| Keymap \| 🔍 "AceJump" \| AceJump \|** <kbd>Enter⏎</kbd>.

![Keymap](https://cloud.githubusercontent.com/assets/175716/11760350/911aed4c-a065-11e5-8f17-49bc97ad1dad.png)

## Building

*Prerequisites: [JDK 8 or higher](http://openjdk.java.net/install/).*

To build AceJump, clone and run the Gradle task [`buildPlugin`](https://github.com/JetBrains/gradle-intellij-plugin#tasks) like so:

* `git clone https://github.com/acejump/AceJump && cd AceJump`
* For Linux and Mac OS: `./gradlew buildPlugin`
* For Windows: `gradlew.bat buildPlugin`

The build artifact will be placed in `build/distributions/`.

*Miscellaneous: AceJump is built using [Gradle](https://gradle.com/) with the [Gradle Kotlin DSL](https://docs.gradle.org/5.1/userguide/kotlin_dsl.html) and the [gradle-intellij-plugin](https://github.com/JetBrains/gradle-intellij-plugin).*

## Extending

AceJump can be used by other [IntelliJ Platform](https://plugins.jetbrains.com/docs/intellij/welcome.html) plugins. To do so, add the following snippet to your `build.gradle.kts` file:

```kotlin
intellij {
  plugins.set("AceJump:<LATEST_VERSION>")
}
```

Callers who pass an instance of [`Editor`](https://github.com/JetBrains/intellij-community/blob/master/platform/editor-ui-api/src/com/intellij/openapi/editor/Editor.java) into `SessionManager.start(editor)` will receive a [`Session`](src/main/kotlin/org/acejump/session/Session.kt) instance in return. Sessions are disposed after use.

To use AceJump externally, please see the following example:

```kotlin
import org.acejump.session.SessionManager
import org.acejump.session.AceJumpListener
import org.acejump.boundaries.StandardBoundaries.*
import org.acejump.search.Pattern.*

val aceJumpSession = SessionManager.start(editorInstance)

aceJumpSession.addAceJumpListener(object: AceJumpListener {
  override fun finished() {
    // ...
  }
})

// Sessions provide these endpoints for external consumers:

/*1.*/ aceJumpSession.markResults(sortedSetOf(/*...*/)) // Pass a set of offsets
/*2.*/ aceJumpSession.startRegexSearch("[aeiou]+", WHOLE_FILE) // Search for regex
/*3.*/ aceJumpSession.startRegexSearch(ALL_WORDS, VISIBLE_ON_SCREEN) // Search for Pattern
```

Custom boundaries for search (i.e. current line before caret etc.) can also be defined using the [Boundaries](src/main/kotlin/org/acejump/boundaries/Boundaries.kt) interface.

## Contributing

AceJump is supported by community members like you. Contributions are highly welcome!

If you would like to [contribute](https://github.com/acejump/AceJump/pulls?q=is%3Apr), here are a few of the ways you can help improve AceJump:

* [Improve test coverage](https://github.com/acejump/AceJump/issues/139)
* [Add action to repeat last search](https://github.com/acejump/AceJump/issues/316)
* [Add configurable RegEx modes](https://github.com/acejump/AceJump/issues/215)
* [Add font family and size options](https://github.com/acejump/AceJump/issues/192)
* [Tag placement and visibility improvements](https://github.com/acejump/AceJump/issues/323)
* [Animated documentation](https://github.com/acejump/AceJump/issues/145)
* [Fold text between matches](https://github.com/acejump/AceJump/issues/255)

To start [IntelliJ IDEA CE](https://github.com/JetBrains/intellij-community) with AceJump installed, run `./gradlew runIde -PluginDev [-x test]`.

To just run [the tests](src/test/kotlin/AceTest.kt), execute `./gradlew test` - this is usually much faster than starting an IDE.

For documentation on plugin development, see the [IntelliJ Platform SDK](http://www.jetbrains.org/intellij/sdk/docs/).

## Release notes

Please [see here](/CHANGES.md) for a detailed list of changes.

## Comparison

AceJump is inspired by prior work, but adds several improvements, including:

* **Ergonomic** tagging: Tries to minimize finger and eye travel on most common keyboards layouts and languages.
* **Full-text** search: If a string is not visible on the screen, AceJump will scroll to the next occurrence.
* **Smart tag** rendering: Tags will occupy nearby whitespace if available, rather than block adjacent text.
* **Target mode**: Jump and select a full word in one rapid motion. (<kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>;</kbd>)
* **Line Mode**: Jump to the first, last, or first non-whitespace character of any line on-screen (<kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>;</kbd>).
* **Word Mode**: Jump to the first character of any visible word on-screen in two keystrokes or less.
* **Declaration Mode**: Jump to the declaration of a token (if it is available) rather than the token itself.
* **Unicode support**: Unicode search and selection, e.g. to search for "拼音", activate AceJump and type: <kbd>p</kbd><kbd>y</kbd>

The following plugins have a similar UI for navigating text and web browsing:

| Source Code                                                           |                                                        Download                                                        |                                                           Application                                                           | Actively Maintained |                                 Language                                 |
|:----------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------------------------------------------:|:-------------------:|:------------------------------------------------------------------------:|
| AceJump                                                               |                                 [⬇](https://plugins.jetbrains.com/plugin/7086-acejump)                                 |                                           [IntelliJ Platform](https://jetbrains.com)                                            | :heavy_check_mark:  |                     [Kotlin](http://kotlinlang.org/)                     |
| [IdeaVim-EasyMotion](https://github.com/AlexPl292/IdeaVim-EasyMotion) |                                  [⬇](https://github.com/AlexPl292/IdeaVim-EasyMotion)                                  |                                           [IntelliJ Platform](https://jetbrains.com)                                            | :heavy_check_mark:  |                     [Kotlin](http://kotlinlang.org/)                     |
| [KJump](https://github.com/a690700752/KJump)                          |                                 [⬇](https://plugins.jetbrains.com/plugin/10149-kjump)                                  |                                           [IntelliJ Platform](https://jetbrains.com)                                            | :heavy_check_mark:  |                     [Kotlin](http://kotlinlang.org/)                     |
| [AceJump-Lite](https://github.com/EeeMt/AceJump-Lite)                 |                              [⬇](https://plugins.jetbrains.com/plugin/9803-acejump-lite)                               |                                           [IntelliJ Platform](https://jetbrains.com)                                            |         :x:         |                       [Java](https://www.java.com)                       |
| [emacsIDEAs](https://github.com/whunmr/emacsIDEAs)                    |                               [⬇](https://plugins.jetbrains.com/plugin/7163-emacsideas)                                |                                           [IntelliJ Platform](https://jetbrains.com)                                            |         :x:         |                       [Java](https://www.java.com)                       |
| [TraceJump](https://github.com/acejump/tracejump)                     |                                       [⬇](https://github.com/acejump/tracejump)                                        |                                                             Desktop                                                             |         :x:         |                     [Kotlin](http://kotlinlang.org/)                     |
| [ace-jump-mode](https://github.com/winterTTr/ace-jump-mode)           |                                         [⬇](https://melpa.org/#/ace-jump-mode)                                         |                                          [emacs](https://www.gnu.org/software/emacs/)                                           |         :x:         |    [Emacs Lisp](https://www.gnu.org/software/emacs/manual/eintr.html)    |
| [avy](https://github.com/abo-abo/avy)                                 |                                              [⬇](https://melpa.org/#/avy)                                              |                                          [emacs](https://www.gnu.org/software/emacs/)                                           | :heavy_check_mark:  |    [Emacs Lisp](https://www.gnu.org/software/emacs/manual/eintr.html)    |
| [EasyMotion](https://github.com/easymotion/vim-easymotion)            |                                     [⬇](https://vimawesome.com/plugin/easymotion)                                      |                                                   [Vim](http://www.vim.org/)                                                    |         :x:         |       [Vimscript](http://learnvimscriptthehardway.stevelosh.com/)        |
| [eyeliner.nvim](https://github.com/jinh0/eyeliner.nvim)               |                     [⬇](https://github.com/jinh0/eyeliner.nvim?tab=readme-ov-file#-installation)                       |                                                  [NeoVim](https://neovim.io/)                                                   | :heavy_check_mark:  |                       [Lua](https://www.lua.org/)                        |
| [Hop](https://github.com/phaazon/hop.nvim)                            |                                 [⬇](https://github.com/phaazon/hop.nvim#installation)                                  |                                                  [NeoVim](https://neovim.io/)                                                   | :heavy_check_mark:  |                       [Lua](https://www.lua.org/)                        |
| [leap.nvim](https://github.com/ggandor/leap.nvim)                     |                                 [⬇](https://github.com/ggandor/leap.nvim#installation)                                 |                                                  [NeoVim](https://neovim.io/)                                                   | :heavy_check_mark:  |                    [Fennel](https://fennel-lang.org)                     |
| [lightspeed.nvim](https://github.com/ggandor/lightspeed.nvim)         |                              [⬇](https://github.com/ggandor/lightspeed.nvim#installation)                              |                                                  [NeoVim](https://neovim.io/)                                                   |         :x:         |                    [Fennel](https://fennel-lang.org)                     |
| [Sublime EasyMotion](https://github.com/tednaleid/sublime-EasyMotion) |                                   [⬇](https://packagecontrol.io/packages/EasyMotion)                                   |                                             [Sublime](https://www.sublimetext.com/)                                             |         :x:         |                    [Python](https://www.python.org/)                     |
| [AceJump](https://github.com/ice9js/ace-jump-sublime)                 |                                    [⬇](https://packagecontrol.io/packages/AceJump)                                     |                                             [Sublime](https://www.sublimetext.com/)                                             |         :x:         |                    [Python](https://www.python.org/)                     |
| [Jumpy](https://github.com/DavidLGoldberg/jumpy)                      |                                          [⬇](https://atom.io/packages/jumpy)                                           |                                                    [Atom](https://atom.io/)                                                     | :heavy_check_mark:  |              [TypeScript](https://www.typescriptlang.org/)               |
| [Jumpy2](https://github.com/DavidLGoldberg/jumpy2)                    |                     [⬇](https://marketplace.visualstudio.com/items?itemName=DavidLGoldberg.jumpy2)                     |                                      [Visual Studio Code](https://code.visualstudio.com/)                                       | :heavy_check_mark:  |              [TypeScript](https://www.typescriptlang.org/)               |
| [Find-Jump](https://github.com/msafi/xvsc/tree/master/findJump)       |                       [⬇](https://marketplace.visualstudio.com/items?itemName=mksafi.find-jump)                        |                                      [Visual Studio Code](https://code.visualstudio.com/)                                       |         :x:         |              [TypeScript](https://www.typescriptlang.org/)               |
| [MetaGo](https://github.com/metaseed/metaGo)                          |                        [⬇](https://marketplace.visualstudio.com/items?itemName=metaseed.metago)                        |                                      [Visual Studio Code](https://code.visualstudio.com/)                                       | :heavy_check_mark:  |              [TypeScript](https://www.typescriptlang.org/)               |
| [VSCodeVim](https://github.com/VSCodeVim/Vim)                         |                         [⬇](https://marketplace.visualstudio.com/items?itemName=vscodevim.vim)                         |                                      [Visual Studio Code](https://code.visualstudio.com/)                                       | :heavy_check_mark:  |              [TypeScript](https://www.typescriptlang.org/)               |
| [CodeAceJumper](https://github.com/lucax88x/CodeAceJumper)            |                    [⬇](https://marketplace.visualstudio.com/items?itemName=lucax88x.codeacejumper)                     |                                      [Visual Studio Code](https://code.visualstudio.com/)                                       |         :x:         |              [TypeScript](https://www.typescriptlang.org/)               |
| [AceJump](https://github.com/jsturtevant/ace-jump)                    |                      [⬇](https://marketplace.visualstudio.com/items?itemName=jsturtevant.AceJump)                      |                                         [Visual Studio](https://www.visualstudio.com/)                                          |         :x:         | [C#](https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/) |
| [EasyMotion](https://github.com/jaredpar/EasyMotion)                  |                    [⬇](https://marketplace.visualstudio.com/items?itemName=JaredParMSFT.EasyMotion)                    |                                         [Visual Studio](https://www.visualstudio.com/)                                          |         :x:         | [C#](https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/) |
| [tmux-fingers](https://github.com/Morantron/tmux-fingers)             |                        [⬇](https://github.com/Morantron/tmux-fingers#using-tmux-plugin-manager)                        |                                              [tmux](https://github.com/tmux/tmux)                                               | :heavy_check_mark:  |                  [Crystal](https://crystal-lang.org/)                    |
| [tmux-thumb](https://github.com/Morantron/tmux-fingers)               |                        [⬇](https://github.com/fcsonline/tmux-thumbs#using-tmux-plugin-manager)                         |                                              [tmux](https://github.com/tmux/tmux)                                               | :heavy_check_mark:  |                    [Rust](https://www.rust-lang.org/)                    |
| [tmux-jump](https://github.com/schasse/tmux-jump)                     |                             [⬇](https://github.com/schasse/tmux-jump#installation-via-tpm)                             |                                              [tmux](https://github.com/tmux/tmux)                                               | :heavy_check_mark:  |                    [Ruby](https://www.ruby-lang.org)                     |
| [tmux-copycat](https://github.com/tmux-plugins/tmux-copycat)          | [⬇](https://github.com/tmux-plugins/tmux-copycat?tab=readme-ov-file#installation-with-tmux-plugin-manager-recommended) |                                              [tmux](https://github.com/tmux/tmux)                                               |         :x:         |                   [Shell](https://www.shellscript.sh/)                   |
| [cVim](https://github.com/1995eaton/chromium-vim)                     |                  [⬇](https://chrome.google.com/webstore/detail/cvim/ihlenndgcmojhcghmfjfneahoeklbjjh)                  |                                             [Chrome](https://www.google.com/chrome)                                             |         :x:         |                [JavaScript](https://www.javascript.com/)                 |
| [SurfingKeys](https://github.com/brookhong/Surfingkeys)               |              [⬇](https://chrome.google.com/webstore/detail/surfingkeys/gfbliohnnapiefjpjlpjnehglfpaknnc)               | [Chrome](https://www.google.com/chrome)/[Firefox](https://www.mozilla.org/firefox)/[Edge](https://microsoftedge.microsoft.com/) | :heavy_check_mark:  |                [JavaScript](https://www.javascript.com/)                 |
| [Vimium](https://github.com/philc/vimium)                             |                 [⬇](https://chrome.google.com/webstore/detail/vimium/dbepggeogbaibhgnhhndojpepiihcmeb)                 | [Chrome](https://www.google.com/chrome)/[Firefox](https://www.mozilla.org/firefox)/[Edge](https://microsoftedge.microsoft.com/) | :heavy_check_mark:  |                [JavaScript](https://www.javascript.com/)                 |
| [Vimium-C](https://github.com/gdh1995/vimium-c)                       |    [⬇](https://microsoftedge.microsoft.com/addons/detail/vimium-c-all-by-keyboar/aibcglbfblnogfjhbcmmpobjhnomhcdo)     | [Chrome](https://www.google.com/chrome)/[Firefox](https://www.mozilla.org/firefox)/[Edge](https://microsoftedge.microsoft.com/) | :heavy_check_mark:  |              [TypeScript](https://www.typescriptlang.org/)               |
| [Vrome](https://github.com/jinzhu/vrome)                              |                 [⬇](https://chrome.google.com/webstore/detail/vrome/godjoomfiimiddapohpmfklhgmbfffjj)                  |                                             [Chrome](https://www.google.com/chrome)                                             |         :x:         |                 [CoffeeScript](http://coffeescript.org/)                 |
| [ViChrome](https://github.com/k2nr/ViChrome)                          |                [⬇](https://chrome.google.com/webstore/detail/vichrome/gghkfhpblkcmlkmpcpgaajbbiikbhpdi)                |                                             [Chrome](https://www.google.com/chrome)                                             |         :x:         |                 [CoffeeScript](http://coffeescript.org/)                 |
| [VimFx](https://github.com/akhodakivskiy/VimFx)                       |                                  [⬇](https://github.com/akhodakivskiy/VimFx/releases)                                  |                                           [Firefox](https://www.mozilla.org/firefox)                                            | :heavy_check_mark:  |                 [CoffeeScript](http://coffeescript.org/)                 |
| [Vimperator](https://github.com/vimperator/vimperator-labs/)          |                              [⬇](https://github.com/vimperator/vimperator-labs/releases)                               |                                           [Firefox](https://www.mozilla.org/firefox)                                            |         :x:         |                [JavaScript](https://www.javascript.com/)                 |
| [Pentadactyl](https://github.com/5digits/dactyl)                      |                                 [⬇](http://bug.5digits.org/pentadactyl/#sect-download)                                 |                                           [Firefox](https://www.mozilla.org/firefox)                                            |         :x:         |                [JavaScript](https://www.javascript.com/)                 |
| [Vim Vixen](https://github.com/ueokande/vim-vixen)                    |                                [⬇](https://addons.mozilla.org/firefox/addon/vim-vixen/)                                |                     [Firefox 57+](https://blog.mozilla.org/addons/2017/09/28/webextensions-in-firefox-57/)                      | :heavy_check_mark:  |                [JavaScript](https://www.javascript.com/)                 |
| [Tridactyl](https://github.com/tridactyl/tridactyl)                   |                              [⬇](https://addons.mozilla.org/firefox/addon/tridactyl-vim/)                              |                     [Firefox 57+](https://blog.mozilla.org/addons/2017/09/28/webextensions-in-firefox-57/)                      | :heavy_check_mark:  |              [TypeScript](https://www.typescriptlang.org/)               |
| [Vimari](https://github.com/guyht/vimari)                             |                                     [⬇](https://github.com/guyht/vimari/releases)                                      |                                             [Safari](https://www.apple.com/safari/)                                             |         :x:         |                [JavaScript](https://www.javascript.com/)                 |
| [Jump To Link](https://github.com/mrjackphil/obsidian-jump-to-link)   |                                  [⬇](https://obsidian.md/plugins?id=mrj-jump-to-link)                                  |                                                [Obsidian](https://obsidian.md/)                                                 | :heavy_check_mark:  |              [TypeScript](https://www.typescriptlang.org/)               |

## Acknowledgements

The following individuals have significantly improved AceJump through their contributions and feedback:

* [John Lindquist](https://github.com/johnlindquist) for creating AceJump and supporting it for many years.
* [Breandan Considine](https://github.com/breandan) for maintaining the project and adding some new features.
* [chylex](https://github.com/chylex) for numerous [performance optimizations](https://github.com/acejump/AceJump/pulls?q=is%3Apr+author%3Achylex), [bug fixes](https://github.com/acejump/AceJump/issues/348#issuecomment-739454920) and [refactoring](https://github.com/acejump/AceJump/pull/353).
* [Alex Plate](https://github.com/AlexPl292) for submitting [several PRs](https://github.com/acejump/AceJump/pulls?q=is%3Apr+author%3AAlexPl292).
* [Sven Speckmaier](https://github.com/svensp) for [improving](https://github.com/acejump/AceJump/pull/214) search latency.
* [Stefan Monnier](https://www.iro.umontreal.ca/~monnier/) for algorithmic advice and maintaining Emacs for several years.
* [Fool's Mate](https://www.fools-mate.de/) for the [icon](https://github.com/acejump/AceJump/issues/313) and graphic design.

AceJump is made possible by users just like you! If you enjoy using AceJump, please consider [Contributing](#contributing).

<!-- Badges -->
[jetbrains-team-page]: https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub
[jetbrains-team-svg]: http://jb.gg/badges/team.svg
[teamcity-build-status]: https://teamcity.jetbrains.com/viewType.html?buildTypeId=acejump_buildplugin&guest=1
[teamcity-status-svg]: https://teamcity.jetbrains.com/app/rest/builds/buildType:acejump_buildplugin/statusIcon.svg
[plugin-repo-page]: https://plugins.jetbrains.com/plugin/7086-acejump
[plugin-repo-svg]: https://img.shields.io/jetbrains/plugin/v/7086-acejump.svg
[plugin-download-svg]: https://img.shields.io/jetbrains/plugin/d/7086-acejump.svg
[twitter-url]: https://twitter.com/search?q=AceJump&f=live
[twitter-badge]: https://img.shields.io/twitter/url/http/shields.io.svg?style=social
[apache-license-svg]: https://img.shields.io/badge/License-GPL%20v3-blue.svg
