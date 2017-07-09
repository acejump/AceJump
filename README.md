# AceJump

[AceJump](https://plugins.jetbrains.com/plugin/7086) is a plugin for the [IntelliJ Platform](https://github.com/JetBrains/intellij-community/) that lets you jump to any symbol in the editor with just a few keystrokes. Press the keyboard shortcut for `AceAction` (<kbd>Ctrl</kbd>+<kbd>;</kbd> by default) to activate a tooltip overlay. Type any visible string in the editor, followed by one of illustrated tags, to jump its position:

![](https://cloud.githubusercontent.com/assets/175716/20177444/124fb534-a74d-11e6-8912-1d220ae27091.png)

Press the AceJump shortcut a second time before completing a tag to activate *Target Mode*. Once *Target Mode* is activated, jumping to a tag will select an entire word. *Target Mode* can also be activated directly by pressing the shortcut for `AceTargetAction` (assigned to <kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>;</kbd> by default).

![](https://cloud.githubusercontent.com/assets/175716/20177362/a9976398-a74c-11e6-955d-df029c7b329b.png)

Press the AceJump shortcut for line mode (<kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>;</kbd> by default), to target the beginning, first non-whitespace characters, and end of every visible line in the editor). Then jump to one by completing the tag.

![](https://cloud.githubusercontent.com/assets/175716/20533565/f7d04d1e-b0ab-11e6-8b89-f7b10a98752d.png)

Press the AceJump shortcut, followed by <kbd>→</kbd>, <kbd>←</kbd>, <kbd>↑</kbd>, or <kbd>↑</kbd>, to target the last, first, or first non-whitespace characters of every visible line in the editor.

![](https://cloud.githubusercontent.com/assets/175716/20177472/4f0ba956-a74d-11e6-97ba-b296eacdd396.png)

AceJump tags are *not* case sensitive. Holding down <kbd>Shift</kbd> when typing the last tag character will select all text from the current cursor position to that destination.

## Tips

Press <kbd>Tab</kbd> when searching to jump to the next group of matches in the editor.

If you mis-type a character, just press <kbd>Backspace</kbd> to restart from scratch.

If no matches can be found on screen, AceJump will scroll to the next match it can find.

If there is only one tagged result, pressing <kbd>Enter</kbd> will jump to that result.

## Installing

Install directly from the IDE, via **File \| Settings \| Plugins \| Browse Repositories... \| 🔍 "AceJump"**.

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

You can change the default keyboard shortcut via **File \| Settings \| Keymap \| 🔍 "AceJump" \| AceJump \|** <kbd>Enter⏎</kbd>.

![Keymap](https://cloud.githubusercontent.com/assets/175716/11760350/911aed4c-a065-11e5-8f17-49bc97ad1dad.png)

## Building

To build AceJump, clone then run the Gradle task `buildPlugin`:

* `git clone https://github.com/johnlindquist/AceJump && cd AceJump`
* `./gradlew buildPlugin`

The build artifact will be located in `build/distributions/`.

## Contributing

AceJump is supported by community members like you. Contributions are highly welcome!

To start IntelliJ IDEA CE with AceJump installed, run `./gradlew runIdea`. 

For documentation on plugin development, see the [IntelliJ Platform SDK](www.jetbrains.org/intellij/sdk/docs/).

AceJump is written in [Kotlin](https://kotlinlang.org/).

## Release notes

Please [see here](/CHANGES.md) for a detailed list of changes.

## Acknowledgements

- [Vimium](https://vimium.github.io/) - A Chrome plugin with a similar UI.
- [Vimperator](http://www.vimperator.org/) - A Firefox plugin with a similar UI.
- [ace-jump-mode](https://www.emacswiki.org/emacs/AceJump) - An emacs plugin with a similar UI.
- [EasyMotion](https://github.com/easymotion/vim-easymotion) - A Vim plugin with a similar UI.
- [Jumpy](https://github.com/DavidLGoldberg/jumpy) - An Atom plugin with a similar UI.