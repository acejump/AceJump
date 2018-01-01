# AceJump

[AceJump](https://plugins.jetbrains.com/plugin/7086) is a plugin for the [IntelliJ Platform](https://github.com/JetBrains/intellij-community/) that lets you jump to any symbol in the editor with just a few keystrokes. Press the keyboard shortcut for `AceAction` (<kbd>Ctrl</kbd>+<kbd>;</kbd> by default) to activate a tooltip overlay. Type any string in the editor, followed by one of illustrated tags, to jump its position:

![](https://cloud.githubusercontent.com/assets/175716/20177444/124fb534-a74d-11e6-8912-1d220ae27091.png)

Press the AceJump shortcut a second time before completing a tag to activate *Target Mode*. Once *Target Mode* is activated, jumping to a tag will select an entire word. *Target Mode* can also be activated directly by pressing the shortcut for `AceTargetAction` (<kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>;</kbd> by default).

![](https://cloud.githubusercontent.com/assets/175716/20177362/a9976398-a74c-11e6-955d-df029c7b329b.png)

Press the AceJump shortcut for line mode (<kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>;</kbd> by default), to target the beginning, first non-whitespace characters, and end of every line in the editor). Then jump to one by completing the tag.

![](https://cloud.githubusercontent.com/assets/175716/20533565/f7d04d1e-b0ab-11e6-8b89-f7b10a98752d.png)

Press the AceJump shortcut, followed by <kbd>→</kbd>, <kbd>←</kbd>, or <kbd>↑</kbd>, to target the last, first, or first non-whitespace characters of every line in the editor.

![](https://cloud.githubusercontent.com/assets/175716/20177472/4f0ba956-a74d-11e6-97ba-b296eacdd396.png)

AceJump tags are *not* case sensitive. Holding down <kbd>Shift</kbd> when typing the last tag character will select all text from the current cursor position to that destination.

## Tips

- Press <kbd>Tab</kbd> when searching to jump to the next group of matches in the editor.

- If you make a mistake searching, just press <kbd>Backspace</kbd> to restart from scratch.

- If no matches can be found on screen, AceJump will scroll to the next match it can find.

- If there is only one tagged result, pressing <kbd>Enter</kbd> will jump to that result.

- Keep typing! AceJump will accept multiple sequential characters before tag selection.

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

To change the default keyboard shortcuts, open **File \| Settings \| Keymap \| 🔍 "AceJump" \| AceJump \|** <kbd>Enter⏎</kbd>.

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

## Comparison

The following plugins have a similar UI for navigation.

| Plugin                                                                |   Application             |  Actively Maintained  | Language | 
| :---                                                                  |     :---:                 |     :---:             |     :---:             |
| AceJump                                                               |     [IntelliJ Platform](https://jetbrains.com)     |✓|[Kotlin](http://kotlinlang.org/)|
| [ace-jump-mode](https://github.com/winterTTr/ace-jump-mode)           |     [emacs](https://www.gnu.org/software/emacs/)                 |✕|[Emacs Lisp](https://www.gnu.org/software/emacs/manual/eintr.html)|
| [EasyMotion](https://github.com/easymotion/vim-easymotion)            |     [Vim](http://www.vim.org/)                   |✕|[Vimscript](http://learnvimscriptthehardway.stevelosh.com/)|
| [Sublime EasyMotion](https://github.com/tednaleid/sublime-EasyMotion) |     [Sublime](https://www.sublimetext.com/)               |✓|[Python](https://www.python.org/)|
| [Jumpy](https://github.com/DavidLGoldberg/jumpy)                      |     [Atom](https://atom.io/)                  |✓|[CoffeeScript](http://coffeescript.org/)|
| [VSCodeVim](https://github.com/VSCodeVim/Vim)                         |     [Visual Studio Code](https://code.visualstudio.com/)    |✓|[TypeScript](https://www.typescriptlang.org/)|
| [cVim](https://github.com/1995eaton/chromium-vim)                     |     [Chrome](https://www.google.com/chrome)                |✓|[JavaScript](https://www.javascript.com/)|
| [SurfingKeys](https://github.com/brookhong/Surfingkeys)               |     [Chrome](https://www.google.com/chrome)                |✓|[JavaScript](https://www.javascript.com/)|
| [Vimium](https://github.com/philc/vimium)                             |     [Chrome](https://www.google.com/chrome)                |✓|[CoffeeScript](http://coffeescript.org/)|
| [Vrome](https://github.com/jinzhu/vrome)                              |     [Chrome](https://www.google.com/chrome)                |✕|[CoffeeScript](http://coffeescript.org/)|
| [ViChrome](https://github.com/k2nr/ViChrome)                          |     [Chrome](https://www.google.com/chrome)                |✕|[CoffeeScript](http://coffeescript.org/)|
| [VimFx](https://github.com/akhodakivskiy/VimFx)                       |     [Firefox](https://www.mozilla.org/firefox)               |✓|[CoffeeScript](http://coffeescript.org/)|
| [Vimperator](https://github.com/vimperator/vimperator-labs/)          |     [Firefox](https://www.mozilla.org/firefox)               |✓|[JavaScript](https://www.javascript.com/)|
| [Pentadactyl](https://github.com/5digits/dactyl)          |     [Firefox](https://www.mozilla.org/firefox)               |✓|[JavaScript](https://www.javascript.com/)|
