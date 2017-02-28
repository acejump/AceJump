# AceJump

[AceJump](https://plugins.jetbrains.com/plugin/7086) is a plugin for the [IntelliJ Platform](https://github.com/JetBrains/intellij-community/) that lets you jump to any symbol in the editor with just a few keystrokes. Press the keyboard shortcut for AceJump (<kbd>Ctrl</kbd>+<kbd>;</kbd> by default) to activate a tooltip overlay. Type any visible string in the editor, followed by one of illustrated tags, to jump its position:

![](https://cloud.githubusercontent.com/assets/175716/20177444/124fb534-a74d-11e6-8912-1d220ae27091.png)

Press the AceJump shortcut a second time before completing a tag to activate *Target Mode*. Once *Target Mode* is activated, jumping to a tag will select an entire word. You can also enter *Target Mode* directly by pressing the shortcut assigned to `AceTargetMode` (<kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>;</kbd> by default).

![](https://cloud.githubusercontent.com/assets/175716/20177362/a9976398-a74c-11e6-955d-df029c7b329b.png)

Press the AceJump shortcut for line mode (<kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>;</kbd> by default), to target the beginning, first non-whitespace characters, and end of every visible line in the editor). Then jump to one by completing the tag.

![](https://cloud.githubusercontent.com/assets/175716/20533565/f7d04d1e-b0ab-11e6-8b89-f7b10a98752d.png)

Press the AceJump shortcut, followed by <kbd>→</kbd>, <kbd>←</kbd>, <kbd>↑</kbd>, or <kbd>↑</kbd>, to target the last, first, or first non-whitespace characters of every visible line in the editor.

![](https://cloud.githubusercontent.com/assets/175716/20177472/4f0ba956-a74d-11e6-97ba-b296eacdd396.png)

Press <kbd>Shift</kbd> when completing the jump to select all text from the current cursor position to the destination.

If you mistype a character while searching, just press <kbd>Backspace</kbd> to restart from scratch.

## Installing

AceJump can be installed by the unzipping the contents of `AceJump.zip` into:

- `$HOME/.IdeaIC`&lt;Major Version&gt;`/config/plugins/` if you are using IntelliJ IDEA Community, or
- `$HOME/.IntellijIdea`&lt;Major Version&gt;`/config/plugins/` if you are using IntelliJ IDEA Ultimate

You can also install AceJump directly from the IDE, via **File \| Settings \| Plugins \| Browse Repositories... \| 🔍 "AceJump"**.

![Install](https://cloud.githubusercontent.com/assets/175716/11760310/cb4657e6-a064-11e5-8e07-837c2c0c40eb.png)

## Configuring

If you are using [IdeaVim](https://plugins.jetbrains.com/plugin/164), copy and paste the following command into your terminal to activate AceJump with a single keystroke (the keys <kbd>f</kbd>, <kbd>F</kbd> and <kbd>g</kbd> are user-configurable):

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

You can change the default keyboard shortcut via **File \| Settings \| Keymap \| 🔍 "AceJump" \| AceJump \|** <kbd>Enter⏎</kbd>.

![Keymap](https://cloud.githubusercontent.com/assets/175716/11760350/911aed4c-a065-11e5-8f17-49bc97ad1dad.png)

## Building

To build AceJump from the source, clone this repository and run `./gradlew buildPlugin`.

## Contributing

To start an instance of IntelliJ IDEA with AceJump installed, run ` ./gradlew runIdea`.

## History

- 3.2.0 Support Back/Forward navigation in the IntelliJ Platform.
- 3.1.8 Fixes some errors that occur when the user closes an editor prematurely.
- 3.1.6 Fixes a rare tag collision scenario and UninitializedPropertyAccess exception
- 3.1.5 Allow users to enter target mode directly by pressing Ctrl+Alt+;
- 3.1.4 Fixes the "Assertion Failed" exception popup
- 3.1.3 Fixes an error affecting some users during startup.
- 3.1.2 Fixes an Android Studio regression.
- 3.1.1 Hotfix for broken target mode.
- 3.1.0 Removes the search box, lots of small usability improvements.
- 3.0.7 No longer tags "folded" regions and minor alignment adjustments.
- 3.0.6 Fixes alignment issues, removes top and bottom alignments until there is a better way to visually differentiate adjacent tags.
- 3.0.5 Hotfix for target mode.
- 3.0.4 Adds "line mode" - press [Ctrl+Shift+;] to activate.
- 3.0.3 Updates to tag placement and performance improvements.
- 3.0.2 Fixes target mode and default shortcut activation for Mac users.
- 3.0.1 Fixes target-mode issues affecting users with non-default shortcuts and adds support for Home/End.

>#### 3.0.0 Major rewrite of AceJump. Introducing:
>
>* Realtime search: Just type the word where you want to jump and AceJump will 
do the rest.
>* Smart tag placement: Tags now occupy nearby whitespace if available, rather
than block text.
>* Keyboard-aware tagging: Tries to minimize finger travel distance on QWERTY 
keyboards.
>* Colorful highlighting: AceJump will now highlight the editor text, 
as you type.

- 2.0.13  Fix a regression affecting *Target Mode* and line-based navigation: https://github.com/johnlindquist/AceJump/commit/cc3a23a3bd6754d11100f15f3dddc4d8529926df#diff-a483c757116bde46e566a8b01520a807L51</dd>
- 2.0.12 Fix ClassCastException when input letter not present: https://github.com/johnlindquist/AceJump/issues/73
- 2.0.11 One hundred percent all natural Kotlin.
- 2.0.10 Support 2016.2, remove upper version limit, update internal Kotlin version
- 2.0.9 Compile on Java 7 to address: https://github.com/johnlindquist/AceJump/issues/61
- 2.0.8 Compile on Java 6 to address: https://github.com/johnlindquist/AceJump/issues/59
- 2.0.7 Language update for Kotlin 1.0 release.
- 2.0.6 Fixing "lost focus" bugs mentioned here: https://github.com/johnlindquist/AceJump/issues/41
- 2.0.5 Fixing "backspace" bugs mentioned here: https://github.com/johnlindquist/AceJump/issues/20
- 2.0.4 Fixing "code folding" bugs mentioned here: https://github.com/johnlindquist/AceJump/issues/24
- 2.0.3 More work on Ubuntu focus bug
- 2.0.2 Fixed bug when there's only 1 search result
- 2.0.1 Fixing Ubuntu focus bug
- 2.0.0 Major release: Added "target mode", many speed increases, multi-char search implemented
- 1.1.0 Switching to Kotlin for the code base
- 1.0.4 Fixing https://github.com/johnlindquist/AceJump/issues/9 and https://github.com/johnlindquist/AceJump/issues/6
- 1.0.3 Fixed minor visual lag when removing the "jumpers" from the editor
- 1.0.2 Cleaning up minor bugs (npe when editor not in focus, not removing layers)
- 1.0.1 Adding a new jump: "Enter" will take you to the first non-whitespace char in a new line (compare to "Home" which takes you to a new line)
- 1.0.0 Cleaned up code base for release

## Acknowledgements

- [Vimium](https://vimium.github.io/) - A Chrome plugin with a similar UI.
- [Vimperator](http://www.vimperator.org/) - A Firefox plugin with a similar UI.
- [ace-jump-mode](http://www.vimperator.org/) - An emacs plugin with a similar UI.