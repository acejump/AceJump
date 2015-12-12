# AceJump

[AceJump](https://plugins.jetbrains.com/plugin/7086) is a plugin for the [IntelliJ Platform](https://github.com/JetBrains/intellij-community/) that lets you rapidly navigate to any textual position in the editor with just a few keystrokes.

![Jump Locations](https://cloud.githubusercontent.com/assets/175716/11759145/f50fcab6-a042-11e5-8662-c67deef1900a.png)

Hitting the keyboard shortcut for AceJump (<kbd>Ctrl</kbd>+<kbd>;</kbd> by default) will activate a tooltip overlay. Press any of the illustrated key combinations in sequence, and the cursor will immediately jump to that location in the editor.

## Installing

AceJump can be installed by the unzipping the contents of `AceJump.zip` into:

- `$HOME/.IdeaIC`&lt;MAJOR VERSION&gt;`/config/plugins/` if you are using IntelliJ IDEA Community, or
- `$HOME/.IntellijIdea`&lt;MAJOR VERSION&gt;`/config/plugins/` if you are using IntelliJ IDEA Ultimate

Alternately, you can install AceJump directly from IntelliJ IDEA, through **File \| Settings \| Plugins \| Browse Repositories... \| 🔍 "AceJump"**.

![Install](https://cloud.githubusercontent.com/assets/175716/11759317/3e581f2c-a046-11e5-9456-c186c6adee18.png)

## Configuring

You can configure the keyboard shortcut bound to AceJump, by visiting **File \| Settings \| Keymap \| 🔍 "AceJump" \| AceJump \|** <kbd>Enter</kbd>.

![Keymap](https://cloud.githubusercontent.com/assets/175716/11759286/7efe7ebe-a045-11e5-9585-420aed8232a4.png)

## Building

In order to build AceJump, clone this repository in IntelliJ IDEA (**VCS \| Checkout from Version Control \| GitHub**).

![Yes](https://cloud.githubusercontent.com/assets/175716/11759555/92cfa288-a04a-11e5-870a-86105515879e.png)

After cloning the repository, select, "Yes", to create an IntelliJ IDEA project for the AceJump sources.

![Import](https://cloud.githubusercontent.com/assets/175716/11759574/14898906-a04b-11e5-88e4-df6b86da715b.png)

Step through the **Import Project** wizard, leaving all of the default settings. Reuse the existing `AceJump.iml` file.

![Reuse](https://cloud.githubusercontent.com/assets/175716/11759599/a3656532-a04b-11e5-838a-e11adf520997.png)

If you are new to plugin development, follow the IntelliJ Platform SDK guide to [Setting Up a Development Environment](http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/setting_up_environment.html). Recommend using at least IntelliJ Platform SDK 143+.

![Deployment](https://cloud.githubusercontent.com/assets/175716/11759627/a563878c-a04c-11e5-8420-f55d75a71c04.png)

Now press **Build \| Prepare Plugin Module 'AceJump' for Deployment**. This will compile `AceJump.jar` in the project directory.
