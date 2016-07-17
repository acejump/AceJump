package com.johnlindquist.acejump

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.ui.awt.RelativePoint
import java.awt.Point

fun guessBestLocation(editor: Editor): RelativePoint {
    val logicalPosition: VisualPosition = editor.caretModel.visualPosition
    return getPointFromVisualPosition(editor, logicalPosition)
}

fun getPointFromVisualPosition(editor: Editor, logicalPosition: VisualPosition): RelativePoint {
    val p: Point = editor.visualPositionToXY(VisualPosition(logicalPosition.line, logicalPosition.column))
    return RelativePoint(editor.contentComponent, p)
}

/*todo: I hate this, but I keep debating whether I wanted to allow numbers and special chars in the "AllowedCharacters" set. Strict mapping to my USA keyboard :(*/
fun getLowerCaseStringFromChar(keyChar: Char): String {
    val s = keyChar.toString()
    when (s) {
        "!" -> return "1"
        "@" -> return "2"
        "#" -> return "3"
        "$" -> return "4"
        "%" -> return "5"
        "^" -> return "6"
        "&" -> return "7"
        "*" -> return "8"
        "(" -> return "9"
        ")" -> return "0"
        "_" -> return "-"
        "+" -> return "="
        "{" -> return "["
        "}" -> return "]"
        "|" -> return "\\"
        ":" -> return ";"
        "<" -> return ","
        ">" -> return "."
        "?" -> return "/"
    }

    return s.toLowerCase()
}
