package net.matzified.iceclient.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public class VectorIcon extends JComponent {

    public enum IconType {
        HOME, PROFILES, STORE, MODS, ACCOUNTS
    }

    private final IconType type;
    private Color color = new Color(148, 163, 184);

    public VectorIcon(IconType type) {
        this.type = type;
        setPreferredSize(new Dimension(28, 28));
        setMaximumSize(new Dimension(28, 28));
    }

    public void setIconColor(Color c) {
        this.color = c;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type) {
            case HOME:
                // Rocket / Home Vector Shape
                Path2D home = new Path2D.Float();
                home.moveTo(w * 0.5, h * 0.18);
                home.lineTo(w * 0.82, h * 0.45);
                home.lineTo(w * 0.72, h * 0.45);
                home.lineTo(w * 0.72, h * 0.82);
                home.lineTo(w * 0.28, h * 0.82);
                home.lineTo(w * 0.28, h * 0.45);
                home.lineTo(w * 0.18, h * 0.45);
                home.closePath();
                g2.draw(home);
                break;

            case PROFILES:
                // Gamepad / Controller Vector Shape
                g2.draw(new RoundRectangle2D.Float(w * 0.15f, h * 0.28f, w * 0.7f, h * 0.48f, 10, 10));
                // D-Pad cross
                g2.drawLine((int)(w * 0.3), (int)(h * 0.52), (int)(w * 0.42), (int)(h * 0.52));
                g2.drawLine((int)(w * 0.36), (int)(h * 0.44), (int)(w * 0.36), (int)(h * 0.60));
                // Action Buttons
                g2.fillOval((int)(w * 0.62), (int)(h * 0.44), 4, 4);
                g2.fillOval((int)(w * 0.72), (int)(h * 0.52), 4, 4);
                break;

            case STORE:
                // Store Grid / Puzzle Vector Shape
                g2.draw(new RoundRectangle2D.Float(w * 0.18f, h * 0.18f, w * 0.28f, h * 0.28f, 4, 4));
                g2.draw(new RoundRectangle2D.Float(w * 0.54f, h * 0.18f, w * 0.28f, h * 0.28f, 4, 4));
                g2.draw(new RoundRectangle2D.Float(w * 0.18f, h * 0.54f, w * 0.28f, h * 0.28f, 4, 4));
                g2.draw(new RoundRectangle2D.Float(w * 0.54f, h * 0.54f, w * 0.28f, h * 0.28f, 4, 4));
                break;

            case MODS:
                // Folder Vector Shape
                Path2D folder = new Path2D.Float();
                folder.moveTo(w * 0.18, h * 0.30);
                folder.lineTo(w * 0.42, h * 0.30);
                folder.lineTo(w * 0.50, h * 0.40);
                folder.lineTo(w * 0.82, h * 0.40);
                folder.lineTo(w * 0.82, h * 0.78);
                folder.lineTo(w * 0.18, h * 0.78);
                folder.closePath();
                g2.draw(folder);
                break;

            case ACCOUNTS:
                // User / Shield Vector Shape
                g2.drawOval((int)(w * 0.34), (int)(h * 0.20), (int)(w * 0.32), (int)(h * 0.32));
                Path2D userBody = new Path2D.Float();
                userBody.moveTo(w * 0.20, h * 0.80);
                userBody.curveTo(w * 0.20, h * 0.60, w * 0.80, h * 0.60, w * 0.80, h * 0.80);
                g2.draw(userBody);
                break;
        }

        g2.dispose();
    }
}
