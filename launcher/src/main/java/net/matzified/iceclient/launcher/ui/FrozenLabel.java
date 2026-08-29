package net.matzified.iceclient.launcher.ui;

import javax.swing.*;
import java.awt.*;

public class FrozenLabel extends JLabel {

    private Color customGlowColor = new Color(56, 189, 248);
    private Color textColor = Color.WHITE;

    public FrozenLabel(String text) {
        super(text);
        setFont(new Font("Segoe UI", Font.BOLD, 22));
    }

    public FrozenLabel(String text, int fontSize) {
        super(text);
        setFont(new Font("Segoe UI", Font.BOLD, fontSize));
    }

    public void setGlowColor(Color color) {
        this.customGlowColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String text = getText();
        if (text != null && !text.isEmpty()) {
            FontMetrics fm = g2.getFontMetrics(getFont());
            int x = 0;
            if (getHorizontalAlignment() == SwingConstants.CENTER) {
                x = (getWidth() - fm.stringWidth(text)) / 2;
            } else if (getHorizontalAlignment() == SwingConstants.RIGHT) {
                x = getWidth() - fm.stringWidth(text);
            }
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            // Draw glowing icy cyan shadow behind white text
            Color activeGlow = (customGlowColor != null) ? customGlowColor : new Color(56, 189, 248);
            g2.setColor(activeGlow);
            g2.drawString(text, x - 1, y - 1);
            g2.drawString(text, x + 1, y - 1);
            g2.drawString(text, x - 1, y + 1);
            g2.drawString(text, x + 1, y + 1);
            g2.drawString(text, x, y + 2);

            // Draw crisp white text on top
            g2.setColor(textColor);
            g2.drawString(text, x, y);
        }

        g2.dispose();
    }
}
