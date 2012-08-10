package com.johnlindquist.acejump;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: John
 * Date: 8/10/12
 * Time: 9:45 AM
 */
public class AceCanvas extends JComponent {
    private Vector<Pair<String, Point>> ballonInfos;
    private Pair<Color, Color> colorPair;
    private float lineSpacing;
    private int lineHeight;


    public void setBallonInfos(@Nullable Vector<Pair<String, Point>> ballonInfos) {
        this.ballonInfos = ballonInfos;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);    //To change body of overridden methods use File | Settings | File Templates.
        if (ballonInfos == null) return;

        Font font = getFont();
        Graphics2D g2d = (Graphics2D) g;
        int fontWidth = getFontMetrics(font).stringWidth("w");
        int fontHeight = font.getSize();
        int rectHeight = lineHeight;
        int lineDiff = 0;
        if (fontHeight > lineHeight) {
            rectHeight = fontHeight;
            lineDiff = fontHeight - lineHeight;
        }

        for (Pair<String, Point> ballonInfo : ballonInfos) {

            String text = ballonInfo.getFirst();
            Point originalPoint = ballonInfo.getSecond();
            Color defaultForeground = colorPair.getSecond();
            Color defaultBackground = colorPair.getFirst();

            int hOffset = (int) (fontHeight * lineSpacing - fontHeight);
            originalPoint.translate(0, hOffset);

            g2d.setColor(defaultForeground);


            g2d.fillRect(originalPoint.x, originalPoint.y + lineDiff, fontWidth, rectHeight);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));


            g2d.setFont(font);
            g2d.setColor(defaultBackground);
            g2d.drawString(text, originalPoint.x, originalPoint.y + fontHeight + lineDiff);
        }

    }

    public void setBackgroundForegroundColors(Pair<Color, Color> colorPair) {
        this.colorPair = colorPair;
    }


    public void setLineSpacing(float lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public void setLineHeight(int lineHeight) {
        this.lineHeight = lineHeight;
    }
}
