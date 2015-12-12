package com.johnlindquist.acejump

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.ui.awt.RelativePoint
import java.awt.Point

fun guessBestLocation(editor: Editor): RelativePoint {
    var logicalPosition: VisualPosition = editor.getCaretModel().getVisualPosition()
    return getPointFromVisualPosition(editor, logicalPosition)
}
fun getPointFromVisualPosition(editor: Editor, logicalPosition: VisualPosition): RelativePoint {
    var p: Point = editor.visualPositionToXY(VisualPosition(logicalPosition.line, logicalPosition.column))
    return RelativePoint(editor.getContentComponent(), p)
}
