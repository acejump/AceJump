# AceJump

[AceJump](https://plugins.jetbrains.com/plugin/7086) is a plugin for the [IntelliJ Platform](https://github.com/JetBrains/intellij-community/) that lets you jump to any symbol in the editor with just a few keystrokes.

![Jump Points](https://cloud.githubusercontent.com/assets/175716/11760345/6029c136-a065-11e5-83fd-5ba09b6a97f8.png)

Hitting the keyboard shortcut for AceJump (<kbd>Ctrl</kbd>+<kbd>;</kbd> by default) will activate a tooltip overlay. Press any of the illustrated key combinations in sequence, and the cursor will immediately jump to that location in the editor.

## Installing

AceJump can be installed by the unzipping the contents of `AceJump.zip` into:

- `$HOME/.IdeaIC`&lt;Major Version&gt;`/config/plugins/` if you are using IntelliJ IDEA Community, or
- `$HOME/.IntellijIdea`&lt;Major Version&gt;`/config/plugins/` if you are using IntelliJ IDEA Ultimate

You can also install AceJump directly from the IDE, via **File \| Settings \| Plugins \| Browse Repositories... \| 🔍 "AceJump"**.

![Install](https://cloud.githubusercontent.com/assets/175716/11760310/cb4657e6-a064-11e5-8e07-837c2c0c40eb.png)

## Configuring

You can change the default keyboard shortcut, by visiting **File \| Settings \| Keymap \| 🔍 "AceJump" \| AceJump \|** <kbd>Enter</kbd>.

![Keymap](https://cloud.githubusercontent.com/assets/175716/11760350/911aed4c-a065-11e5-8f17-49bc97ad1dad.png)

## Building

In order to build AceJump from the source, clone this repository and run `./gradlew buildPlugin`.

## History

- 2.0.6 Fixing "lost focus" bugs mentioned here: https://github.com/johnlindquist/AceJump/issues/41
- 2.0.5 Fixing "backspace" bugs mentioned here: https://github.com/johnlindquist/AceJump/issues/20
- 2.0.4 Fixing "code folding" bugs mentioned here: https://github.com/johnlindquist/AceJump/issues/24
- 2.0.3 More work on Ubuntu focus bug
- 2.0.2 Fixed bug when there's only 1 search result
- 2.0.1 Fixing Ubuntu focus bug
- 2.0.0 Major release: Added "target mode", many speed increases, mutli-char search implemented
- 1.1.0 Switching to Kotlin for the code base
- 1.0.4 Fixing https://github.com/johnlindquist/AceJump/issues/9 and https://github.com/johnlindquist/AceJump/issues/6
- 1.0.3 Fixed minor visual lag when removing the "jumpers" from the editor
- 1.0.2 Cleaning up minor bugs (npe when editor not in focus, not removing layers)
- 1.0.1 Adding a new jump: "Enter" will take you to the first non-whitespace char in a new line (compare to "Home" which takes you to a new line)
- 1.0.0 Cleaned up code base for release
