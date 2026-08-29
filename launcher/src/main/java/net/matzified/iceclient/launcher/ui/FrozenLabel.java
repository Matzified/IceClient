package net.matzified.iceclient.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * 🧊 Futuristic Frozen Square Overlay Label.
 * Renders custom geometric ice typography with a translucent frosted glass square overlay,
 * diagonal ice crystal facet shimmers, corner brackets, and neon cyan glowing borders.
 */
public class FrozenLabel extends JLabel {

    private Color glowColor = new Color(56, 189, 248);
    private Color textColor = Color.WHITE;
    private boolean squareOverlay = true;

    public FrozenLabel(String text) {
        this(text, 20);
    }

    public FrozenLabel(String text, int fontSize) {
        super(text);
        setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        setOpaque(false);
    }

    public void setGlowColor(Color color) {
        this.glowColor = color;
        repaint();
    }

    public void setSquareOverlay(boolean enabled) {
        this.squareOverlay = enabled;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Frozen Square Glass Overlay
        if (squareOverlay && w > 10 && h > 10) {
            // Multi-stop Frosted Ice Glass Backdrop
            GradientPaint glassGrad = new GradientPaint(0, 0, new Color(14, 22, 38, 220), 0, h, new Color(9, 14, 26, 240));
            g2.setPaint(glassGrad);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));

            // Diagonal Frost Shimmer Lines
            g2.setColor(new Color(56, 189, 248, 20));
            g2.setStroke(new BasicStroke(1.5f));
            for (int i = -h; i < w + h; i += 24) {
                g2.drawLine(i, h, i + h, 0);
            }

            // Top Frosted Glass Highlight
            g2.setColor(new Color(186, 230, 253, 50));
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h / 2f, 8, 8));

            // Outer Glowing Cyan Frozen Border
            g2.setColor(new Color(56, 189, 248, 140));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 10, 10));

            // Corner Ice Accent Brackets
            int bracketLen = Math.min(10, Math.min(w, h) / 3);
            g2.setColor(new Color(56, 189, 248, 240));
            g2.setStroke(new BasicStroke(2.0f));
            // Top-Left
            g2.drawLine(2, 2, 2 + bracketLen, 2);
            g2.drawLine(2, 2, 2, 2 + bracketLen);
            // Top-Right
            g2.drawLine(w - 2 - bracketLen, 2, w - 2, 2);
            g2.drawLine(w - 2, 2, w - 2, 2 + bracketLen);
            // Bottom-Left
            g2.drawLine(2, h - 2 - bracketLen, 2, h - 2);
            g2.drawLine(2, h - 2, 2 + bracketLen, h - 2);
            // Bottom-Right
            g2.drawLine(w - 2 - bracketLen, h - 2, w - 2, h - 2);
            g2.drawLine(w - 2, h - 2 - bracketLen, w - 2, h - 2);
        }

        // 2. Custom Futuristic Ice Text with Ambient Cyan Drop-Glow
        String text = getText();
        if (text != null && !text.isEmpty()) {
            FontMetrics fm = g2.getFontMetrics(getFont());
            int x = 14;
            if (getHorizontalAlignment() == SwingConstants.CENTER) {
                x = (w - fm.stringWidth(text)) / 2;
            } else if (getHorizontalAlignment() == SwingConstants.RIGHT) {
                x = w - fm.stringWidth(text) - 14;
            }
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();

            // Glowing Outer Cyan Text Shadow
            g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 90));
            g2.drawString(text, x - 1, y);
            g2.drawString(text, x + 1, y);
            g2.drawString(text, x, y - 1);
            g2.drawString(text, x, y + 1);
            g2.drawString(text, x, y + 2);

            // Crisp White/Cyan Ice Fill Text
            g2.setColor(textColor);
            g2.drawString(text, x, y);
        }

        g2.dispose();
    }
}
