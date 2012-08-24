package com.johnlindquist.acejump;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.ui.awt.RelativePoint;

import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: johnlindquist
 * Date: 8/24/12
 * Time: 3:40 PM
 */
public class AceUtil {
    public static RelativePoint guessBestLocation(Editor editor) {
        VisualPosition logicalPosition = editor.getCaretModel().getVisualPosition();
        return getPointFromVisualPosition(editor, logicalPosition);
    }

    protected static RelativePoint getPointFromVisualPosition(Editor editor, VisualPosition logicalPosition) {
        Point p = editor.visualPositionToXY(new VisualPosition(logicalPosition.line, logicalPosition.column));
        return new RelativePoint(editor.getContentComponent(), p);
    }
}
