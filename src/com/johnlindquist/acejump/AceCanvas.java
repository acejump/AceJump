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
 * To change this template use File | Settings | File Templates.
 */
public class AceCanvas extends JComponent {
    private Vector<Pair<String, Point>> ballonInfos;
    private int lineHeight;
    private Pair<Color, Color> colorPair;


    public void setBallonInfos(@Nullable Vector<Pair<String, Point>> ballonInfos) {
        this.ballonInfos = ballonInfos;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);    //To change body of overridden methods use File | Settings | File Templates.
        if(ballonInfos == null) return;

        Font font = getFont();
        Graphics2D g2d = (Graphics2D) g;

        for (Pair<String, Point> ballonInfo : ballonInfos) {

            String text = ballonInfo.getFirst();
            Point originalPoint = ballonInfo.getSecond();
            Color defaultForeground = colorPair.getSecond();
            Color defaultBackground = colorPair.getFirst();

            g2d.setColor(defaultForeground);
            g2d.fillRect(originalPoint.x, originalPoint.y, getFontMetrics(font).stringWidth("w"), lineHeight + 1);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));


            g2d.setFont(font);
            g2d.setColor(defaultBackground);
            g2d.drawString(text, originalPoint.x, originalPoint.y + font.getSize());
        }

    }

    public void setLineHeight(int lineHeight) {
        this.lineHeight = lineHeight;
    }

    public void setBackgroundForegroundColors(Pair<Color, Color> colorPair) {
        this.colorPair = colorPair;
    }
}
