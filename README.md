AceJump
=======


AceJump allows you to quickly navigate the cursor to any position visible in the editor.

Simply hit "ctrl+;", type a character, then type the matching character to Ace Jump.

Hit HOME (Mac: cmd+LEFT) or END (Mac: cmd+RIGHT) to jump to firs or last char on a line

See a demo in action here: http://johnlindquist.com/2012/08/14/ace_jump.html



History
=======

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
