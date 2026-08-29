package net.matzified.iceclient.launcher.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Clean, sharp, authentic typography label inspired by modern client design systems (Lunar/Feather).
 * Features crisp subpixel antialiasing and subtle text contrast without noisy overlays.
 */
public class FrozenLabel extends JLabel {

    private Color accentColor = new Color(56, 189, 248);
    private Color textColor = Color.WHITE;

    public FrozenLabel(String text) {
        this(text, 18);
    }

    public FrozenLabel(String text, int fontSize) {
        super(text);
        setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        setForeground(Color.WHITE);
        setOpaque(false);
    }

    public void setAccentColor(Color color) {
        this.accentColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

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

            // Subtle, clean contrast shadow
            g2.setColor(new Color(0, 0, 0, 100));
            g2.drawString(text, x, y + 1);

            // Clean text
            g2.setColor(textColor);
            g2.drawString(text, x, y);
        }

        g2.dispose();
    }
}
