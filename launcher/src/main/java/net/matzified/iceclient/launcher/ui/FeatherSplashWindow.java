package net.matzified.iceclient.launcher.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class FeatherSplashWindow extends JWindow {

    private JProgressBar progressBar;
    private JLabel statusLabel;
    private Image crystalLogo;

    public FeatherSplashWindow() {
        setSize(480, 280);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);

        try (InputStream is = getClass().getResourceAsStream("/assets/logo.png")) {
            if (is != null) {
                crystalLogo = ImageIO.read(is);
            }
        } catch (Exception ignored) {}

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Deep Dark Midnight Background Gradient (OG Feather Client Style)
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(15, 20, 32),
                        0, getHeight(), new Color(9, 12, 20)
                );
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));

                // Glowing Cyan Outer Border
                g2.setColor(new Color(56, 189, 248, 120));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));

                // Draw Center Crystal Logo
                if (crystalLogo != null) {
                    int logoW = 72;
                    int logoH = 72;
                    int logoX = (getWidth() - logoW) / 2;
                    int logoY = 35;
                    g2.drawImage(crystalLogo, logoX, logoY, logoW, logoH, null);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(120, 40, 25, 40));

        JPanel contentBox = new JPanel();
        contentBox.setLayout(new BoxLayout(contentBox, BoxLayout.Y_AXIS));
        contentBox.setOpaque(false);

        JLabel titleLbl = new JLabel("ICE CLIENT", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel verLbl = new JLabel("v1.0.0 (1.21 - 1.21.11 Fabric)", SwingConstants.CENTER);
        verLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        verLbl.setForeground(new Color(148, 163, 184));
        verLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = new JLabel("Loading Settings...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusLabel.setForeground(new Color(56, 189, 248));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar = new JProgressBar(0, 100) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(22, 28, 42));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));

                int progressWidth = (int) ((getWidth() * getPercentComplete()));
                if (progressWidth > 0) {
                    GradientPaint gp = new GradientPaint(0, 0, new Color(56, 189, 248), getWidth(), 0, new Color(2, 132, 199));
                    g2.setPaint(gp);
                    g2.fill(new RoundRectangle2D.Float(0, 0, progressWidth, getHeight(), 8, 8));
                }

                g2.dispose();
            }
        };
        progressBar.setPreferredSize(new Dimension(400, 8));
        progressBar.setMaximumSize(new Dimension(400, 8));
        progressBar.setBorderPainted(false);
        progressBar.setOpaque(false);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentBox.add(titleLbl);
        contentBox.add(verLbl);
        contentBox.add(Box.createRigidArea(new Dimension(0, 12)));
        contentBox.add(statusLabel);
        contentBox.add(Box.createRigidArea(new Dimension(0, 8)));
        contentBox.add(progressBar);

        mainPanel.add(contentBox, BorderLayout.CENTER);
        add(mainPanel);
    }

    public void updateProgress(int percent, String message) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(percent);
            statusLabel.setText(message);
        });
    }
}
