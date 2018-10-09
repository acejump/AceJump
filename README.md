# AceJump

[![][jetbrains-team-svg]][jetbrains-team-page]
[![][teamcity-status-svg]][teamcity-build-status]
[![][plugin-repo-svg]][plugin-repo-page]

[AceJump](https://plugins.jetbrains.com/plugin/7086) is a plugin for the [IntelliJ Platform](https://github.com/JetBrains/intellij-community/) that lets you jump to any symbol in the editor with just a few keystrokes. Press the keyboard shortcut for `AceAction` (<kbd>Ctrl</kbd>+<kbd>;</kbd> by default) to activate AceJump. Type any string in the editor, followed by one of illustrated tags, to jump its position:

![](https://cloud.githubusercontent.com/assets/175716/20177444/124fb534-a74d-11e6-8912-1d220ae27091.png)

Press the AceJump shortcut a second time before completing a tag to activate **Target Mode**. Once **Target Mode** is activated, jumping to a tag will select an entire word. **Target Mode** can also be activated directly by pressing the shortcut for `AceTargetAction` (<kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>;</kbd> by default).

![](https://cloud.githubusercontent.com/assets/175716/20177362/a9976398-a74c-11e6-955d-df029c7b329b.png)

Press the AceJump shortcut for **Line Mode**(<kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>;</kbd> by default), to target the beginning, first non-whitespace characters, and end of every line in the editor). Then jump to one by completing the tag.

![](https://cloud.githubusercontent.com/assets/175716/20533565/f7d04d1e-b0ab-11e6-8b89-f7b10a98752d.png)

Press the AceJump shortcut, followed by <kbd>→</kbd> to target the last, <kbd>←</kbd> to target the first, or <kbd>↑</kbd>, to target the first non-whitespace characters of every line in the editor.

![](https://cloud.githubusercontent.com/assets/175716/20177472/4f0ba956-a74d-11e6-97ba-b296eacdd396.png)

AceJump tags are *not* case sensitive. Holding down <kbd>Shift</kbd> when typing the last tag character will select all text from the current cursor position to that destination.

## Tips

- Press <kbd>Tab</kbd> when searching to jump to the next group of matches in the editor.

- If you make a mistake searching, just press <kbd>Backspace</kbd> to restart from scratch.

- If no matches can be found on screen, AceJump will scroll to the next match it can find.

- Pressing <kbd>Enter</kbd> during a search will jump to the next (or nearest) result.

- Keep typing! AceJump will accept multiple sequential characters before tag selection.

- **Word Mode** is a new action that will tag all visible words as soon as it is activated.

  - To bind a keyboard shortcut to **Word Mode**, open **Settings | Keymap | 🔍 "AceJump"**

## Installing

Install directly from the IDE, via **File | Settings | Plugins | Browse Repositories... | 🔍 "AceJump"**.

![Install](https://cloud.githubusercontent.com/assets/175716/11760310/cb4657e6-a064-11e5-8e07-837c2c0c40eb.png)

## Configuring

[IdeaVim](https://plugins.jetbrains.com/plugin/164) users can choose to activate AceJump with a single keystroke (<kbd>f</kbd>, <kbd>F</kbd> and <kbd>g</kbd> are arbitrary) by running:

```
echo -e '

" Press `f` to activate AceJump
map f :action AceAction<CR>
" Press `F` to activate Target Mode
map F :action AceTargetAction<CR>
" Press `g` to activate Line Mode
map g :action AceLineAction<CR>

' >> ~/.ideavimrc
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

*Miscellaneous: AceJump uses the [Gradle Kotlin DSL](https://github.com/gradle/kotlin-dsl) with the [gradle-intellij-plugin](https://github.com/JetBrains/gradle-intellij-plugin).*

*For more information, please refer to the [Gradle Kotlin DSL Docs](https://gradle.github.io/kotlin-dsl-docs/) (currently alpha).*

## Contributing

AceJump is supported by community members like you. Contributions are highly welcome!

If you would like to [contribute](https://github.com/acejump/AceJump/pulls), here are a few of the ways you can help improve AceJump:

* [Improve test coverage](https://github.com/acejump/AceJump/issues/139)
* [Multi-monitor support](https://github.com/acejump/AceJump/issues/144)
* [Animated documentation](https://github.com/acejump/AceJump/issues/145)
* [Speed up tagging on large files](https://github.com/acejump/AceJump/issues/161)
* [Support user-configurable keyboard layouts](https://github.com/acejump/AceJump/issues/172)

To start [IntelliJ IDEA CE](https://github.com/JetBrains/intellij-community) with AceJump installed, run `./gradlew runIde -PluginDev`.

For documentation on plugin development, see the [IntelliJ Platform SDK](www.jetbrains.org/intellij/sdk/docs/).

If you enjoy using AceJump, you might also enjoy reading [the code](src/main/kotlin/org/acejump/label/Solver.kt).

## Release notes

Please [see here](/CHANGES.md) for a detailed list of changes.

## Comparison

AceJump is inspired by prior work, but adds several improvements, including:

* **Target mode**: Jump and select an full word in one rapid motion.
* **Line Mode**: Jump to the first, last, or first non-whitespace character of a line.
* **Word Mode**: Jump to the first character of any visible word on-screen in two keystrokes or less.
* **Declaration Mode**: Jump to the declaration of a token in the editor instead of the token.
* **Real-time** search: Type any string in the editor, and AceJump will highlight and tag matches instantly.
* **Full text** search: If a string is not visible on the screen, AceJump will scroll to the next occurrence.
* **Smart tag** rendering: Tags will occupy nearby whitespace if available, rather than block text.
* **Keyboard-friendly** tagging: AceJump tries to minimize finger travel distance on QWERTY keyboards.

The following plugins have a similar UI for navigating text and web browsing:

| Source Code                                                          | Download                                                                                   |   Application                                                                                   |  Actively Maintained  | Language |
| :---                                                                 | :---:                                                                                      |     :---:                                                                                       |:---:                  |   :---:  |
| AceJump                                                              | [🔽](https://plugins.jetbrains.com/plugin/7086-acejump)                                      |     [IntelliJ Platform](https://jetbrains.com)                                                  |✅                      |[Kotlin](http://kotlinlang.org/)|
| [ace-jump-mode](https://github.com/winterTTr/ace-jump-mode)          | 🔌🔌🔌[🔽](https://melpa.org/#/ace-jump-mode)                                                      |     [emacs](https://www.gnu.org/software/emacs/)                                                |❌                      |[Emacs Lisp](https://www.gnu.org/software/emacs/manual/eintr.html)|
| [avy](https://github.com/abo-abo/avy)                                | [🔽](https://melpa.org/#/avy)                                                                |     [emacs](https://www.gnu.org/software/emacs/)                                                |✅                      |[Emacs Lisp](https://www.gnu.org/software/emacs/manual/eintr.html)|
| [EasyMotion](https://github.com/easymotion/vim-easymotion)           | [🔽](https://vimawesome.com/plugin/easymotion)                                               |     [Vim](http://www.vim.org/)                                                                  |❌                      |[Vimscript](http://learnvimscriptthehardway.stevelosh.com/)|
| [Sublime EasyMotion](https://github.com/tednaleid/sublime-EasyMotion)| [🔽](https://packagecontrol.io/packages/EasyMotion)                                          |     [Sublime](https://www.sublimetext.com/)                                                     |❌                      |[Python](https://www.python.org/)|
| [AceJump](https://github.com/ice9js/ace-jump-sublime)                | [🔽](https://packagecontrol.io/packages/AceJump)                                             |     [Sublime](https://www.sublimetext.com/)                                                     |✅                      |[Python](https://www.python.org/)|
| [Jumpy](https://github.com/DavidLGoldberg/jumpy)                     | [🔽](https://atom.io/packages/jumpy)                                                         |     [Atom](https://atom.io/)                                                                    |✅                      |[CoffeeScript](http://coffeescript.org/)|
| [Find-Jump](https://github.com/msafi/xvsc/tree/master/findJump)      | [🔽](https://marketplace.visualstudio.com/items?itemName=mksafi.find-jump)                   |     [Visual Studio Code](https://code.visualstudio.com/)                                        |❌                      |[TypeScript](https://www.typescriptlang.org/)|
| [VSCodeVim](https://github.com/VSCodeVim/Vim)                        | [🔽](https://marketplace.visualstudio.com/items?itemName=vscodevim.vim)                      |     [Visual Studio Code](https://code.visualstudio.com/)                                        |✅                      |[TypeScript](https://www.typescriptlang.org/)|
| [AceJump](https://github.com/jsturtevant/ace-jump)                   | [🔽](https://marketplace.visualstudio.com/items?itemName=jsturtevant.AceJump)                |     [Visual Studio](https://www.visualstudio.com/)                                              |❌                      |[C#](https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/)|
| [cVim](https://github.com/1995eaton/chromium-vim)                    | [🔽](https://chrome.google.com/webstore/detail/cvim/ihlenndgcmojhcghmfjfneahoeklbjjh)        |     [Chrome](https://www.google.com/chrome)                                                     |✅                      |[JavaScript](https://www.javascript.com/)|
| [SurfingKeys](https://github.com/brookhong/Surfingkeys)              | [🔽](https://chrome.google.com/webstore/detail/surfingkeys/gfbliohnnapiefjpjlpjnehglfpaknnc) |     [Chrome](https://www.google.com/chrome) / [Firefox](https://www.mozilla.org/firefox)        |✅                      |[JavaScript](https://www.javascript.com/)|
| [Vimium](https://github.com/philc/vimium)                            | [🔽](https://chrome.google.com/webstore/detail/vimium/dbepggeogbaibhgnhhndojpepiihcmeb)      |     [Chrome](https://www.google.com/chrome)                                                     |✅                      |[CoffeeScript](http://coffeescript.org/)|
| [Vrome](https://github.com/jinzhu/vrome)                             | [🔽](https://chrome.google.com/webstore/detail/vrome/godjoomfiimiddapohpmfklhgmbfffjj)       |     [Chrome](https://www.google.com/chrome)                                                     |❌                      |[CoffeeScript](http://coffeescript.org/)|
| [ViChrome](https://github.com/k2nr/ViChrome)                         | [🔽](https://chrome.google.com/webstore/detail/vichrome/gghkfhpblkcmlkmpcpgaajbbiikbhpdi)    |     [Chrome](https://www.google.com/chrome)                                                     |❌                      |[CoffeeScript](http://coffeescript.org/)|
| [VimFx](https://github.com/akhodakivskiy/VimFx)                      | [🔽](https://addons.mozilla.org/firefox/addon/vimfx)                                         |     [Firefox](https://www.mozilla.org/firefox)                                                  |✅                      |[CoffeeScript](http://coffeescript.org/)|
| [Vimperator](https://github.com/vimperator/vimperator-labs/)         | [🔽](https://addons.mozilla.org/firefox/addon/vimperator/)                                   |     [Firefox](https://www.mozilla.org/firefox)                                                  |❌                      |[JavaScript](https://www.javascript.com/)|
| [Pentadactyl](https://github.com/5digits/dactyl)                     | [🔽](https://addons.mozilla.org/firefox/addon/pentadactyl/)                                  |     [Firefox](https://www.mozilla.org/firefox)                                                  |❌                      |[JavaScript](https://www.javascript.com/)| 
| [Vim Vixen](https://github.com/ueokande/vim-vixen)                   | [🔽](https://addons.mozilla.org/firefox/addon/vim-vixen/)                                    |     [Firefox 57+](https://blog.mozilla.org/addons/2017/09/28/webextensions-in-firefox-57/)      |✅                      |[JavaScript](https://www.javascript.com/)|
| [Tridactyl](https://github.com/tridactyl/tridactyl)                  | [🔽](https://addons.mozilla.org/firefox/addon/tridactyl-vim/)                                |     [Firefox 57+](https://blog.mozilla.org/addons/2017/09/28/webextensions-in-firefox-57/)      |✅                      |[TypeScript](https://www.typescriptlang.org/)|
| [Vimari](https://github.com/guyht/vimari)                            | [🔽](https://github.com/guyht/vimari/releases)                                               |     [Safari](https://www.apple.com/safari/)                                                     |✅                      |[JavaScript](https://www.javascript.com/)|


<!-- Badges -->
[jetbrains-team-page]: https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub
[jetbrains-team-svg]: http://jb.gg/badges/team-flat-square.svg
[teamcity-build-status]: https://teamcity.jetbrains.com/viewType.html?buildTypeId=acejump_buildplugin&guest=1
[teamcity-status-svg]: https://teamcity.jetbrains.com/app/rest/builds/buildType:acejump_buildplugin/statusIcon.svg
[plugin-repo-page]: https://plugins.jetbrains.com/plugin/7086-acejump
[plugin-repo-svg]: https://img.shields.io/jetbrains/plugin/v/7086-acejump.svg
