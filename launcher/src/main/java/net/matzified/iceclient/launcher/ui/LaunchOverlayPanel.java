package net.matzified.iceclient.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Full-window frosted-glass pre-launch overlay.
 * Displayed via JLayeredPane.MODAL_LAYER so it covers the entire launcher window.
 * Features central animated breathing aura, 4-stage pipeline indicators,
 * live percentage readout, and status messages in the bottom-right corner.
 */
public class LaunchOverlayPanel extends JPanel {

    private final JLabel  statusLabel;
    private final JLabel  percentLabel;
    private final JProgressBar progressBar;
    private final JButton cancelBtn;
    private Runnable onCancel;

    // Pulse animation state
    private float pulseAlpha    = 0f;
    private boolean pulseUp     = true;
    private float rotateAngle   = 0f;
    private final Timer pulseTimer;

    // Stage tracking (1: Manifest, 2: Assets, 3: Libraries, 4: Launching)
    private int currentStage = 1;

    public LaunchOverlayPanel() {
        setLayout(null);
        setOpaque(false);

        // ---------- Status label (bottom-right) ----------
        statusLabel = new JLabel("Initializing launch pipeline...");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(new Color(186, 230, 253));
        add(statusLabel);

        // ---------- Percentage label ----------
        percentLabel = new JLabel("0%", SwingConstants.RIGHT);
        percentLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        percentLabel.setForeground(new Color(56, 189, 248));
        add(percentLabel);

        // ---------- Progress bar ----------
        progressBar = new JProgressBar(0, 100);
        progressBar.setOpaque(false);
        progressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth(), h = c.getHeight();

                // Track
                g2.setColor(new Color(20, 25, 38));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                g2.setColor(new Color(40, 48, 68));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 10, 10));

                // Fill
                int filled = (int)(w * progressBar.getPercentComplete());
                if (filled > 0) {
                    GradientPaint gp = new GradientPaint(0, 0, new Color(56, 189, 248), filled, 0, new Color(14, 165, 233));
                    g2.setPaint(gp);
                    g2.fill(new RoundRectangle2D.Float(0, 0, filled, h, 10, 10));

                    // Glowing cap on the leading edge
                    g2.setColor(new Color(224, 242, 254, 220));
                    g2.fill(new RoundRectangle2D.Float(Math.max(0, filled - 8), 0, 8, h, 10, 10));
                }
                g2.dispose();
            }
            @Override protected void paintIndeterminate(Graphics g, JComponent c) { paintDeterminate(g, c); }
        });
        progressBar.setBorderPainted(false);
        progressBar.setValue(0);
        add(progressBar);

        // ---------- Cancel button ----------
        cancelBtn = new JButton("Cancel Launch") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color bg = getModel().isRollover() ? new Color(220, 38, 38) : new Color(127, 29, 29, 190);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                g2.setColor(new Color(248, 113, 113, 140));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));

                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };
        cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setContentAreaFilled(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> { if (onCancel != null) onCancel.run(); });
        add(cancelBtn);

        // ---------- Pulse animation timer ----------
        pulseTimer = new Timer(25, e -> {
            pulseAlpha += pulseUp ? 0.02f : -0.02f;
            if (pulseAlpha >= 1f) { pulseAlpha = 1f; pulseUp = false; }
            if (pulseAlpha <= 0f) { pulseAlpha = 0f; pulseUp = true;  }
            rotateAngle += 0.05f;
            repaint();
        });
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Progress bar — slim, centered near the bottom
        int pbW = Math.min(580, w - 140);
        int pbH = 10;
        int pbX = (w - pbW) / 2;
        int pbY = h - 110;
        progressBar.setBounds(pbX, pbY, pbW, pbH);

        // Percentage label — right above progress bar
        percentLabel.setBounds(pbX + pbW - 60, pbY - 22, 60, 18);

        // Status label — bottom-right
        statusLabel.setSize(Integer.MAX_VALUE, 20);
        Dimension sd = statusLabel.getPreferredSize();
        int sw = Math.min(sd.width + 12, w - 60);
        statusLabel.setBounds(w - sw - 24, h - 50, sw, 20);

        // Cancel button — bottom-center
        cancelBtn.setBounds((w - 130) / 2, h - 76, 130, 36);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // 1. Frosted glass dark overlay
        g2.setColor(new Color(6, 8, 14, 235));
        g2.fillRect(0, 0, w, h);

        // 2. Ambient pulse glow from center
        int glowAlpha = (int)(45 + 35 * pulseAlpha);
        RadialGradientPaint glow = new RadialGradientPaint(
                w / 2.0f, h / 2.0f - 40, Math.min(w, h) * 0.42f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(56, 189, 248, glowAlpha), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(glow);
        g2.fillRect(0, 0, w, h);

        // 3. Central Animated Crystal / Ring Loader
        int cx = w / 2;
        int cy = h / 2 - 50;

        // Rotating outer dashed ring
        Graphics2D gRing = (Graphics2D) g2.create();
        gRing.translate(cx, cy);
        gRing.rotate(rotateAngle);
        gRing.setColor(new Color(56, 189, 248, 160));
        gRing.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{8, 8}, 0));
        gRing.draw(new Ellipse2D.Float(-40, -40, 80, 80));
        gRing.dispose();

        // Inner glowing core
        int coreAlpha = (int)(180 + 75 * pulseAlpha);
        g2.setColor(new Color(14, 165, 233, coreAlpha));
        g2.fill(new Ellipse2D.Float(cx - 24, cy - 24, 48, 48));

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        String sym = "❄";
        FontMetrics fms = g2.getFontMetrics();
        g2.drawString(sym, cx - fms.stringWidth(sym) / 2, cy + fms.getAscent() / 2 - 2);

        // 4. ICE CLIENT title & sub-status
        int brandAlpha = (int)(220 + 35 * pulseAlpha);
        g2.setColor(new Color(56, 189, 248, brandAlpha));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
        String brand = "ICE CLIENT";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(brand, cx - fm.stringWidth(brand) / 2, cy + 62);

        g2.setColor(new Color(148, 163, 184, 220));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        String sub = "Launching Minecraft Instance...";
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(sub, cx - fm2.stringWidth(sub) / 2, cy + 86);

        // 5. 4-Stage Pipeline Status Pills
        paintPipelineStages(g2, cx, cy + 120);

        // 6. Bottom status bar separation
        g2.setColor(new Color(10, 14, 22, 220));
        g2.fillRect(0, h - 130, w, 130);
        g2.setColor(new Color(56, 189, 248, 50));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(0, h - 130, w, h - 130);

        g2.dispose();
        super.paintComponent(g);
    }

    private void paintPipelineStages(Graphics2D g2, int cx, int startY) {
        String[] stages = {"1. Manifest", "2. Assets", "3. Libraries", "4. Boot JVM"};
        int pillW = 110;
        int gap = 12;
        int totalW = (stages.length * pillW) + ((stages.length - 1) * gap);
        int startX = cx - totalW / 2;

        for (int i = 0; i < stages.length; i++) {
            int px = startX + (i * (pillW + gap));
            int stageNum = i + 1;
            boolean isActive = (currentStage == stageNum);
            boolean isCompleted = (currentStage > stageNum);

            Color bg;
            Color border;
            Color text;

            if (isCompleted) {
                bg = new Color(16, 185, 129, 45);
                border = new Color(16, 185, 129, 180);
                text = new Color(52, 211, 153);
            } else if (isActive) {
                bg = new Color(14, 165, 233, 60);
                border = new Color(56, 189, 248, 220);
                text = new Color(224, 242, 254);
            } else {
                bg = new Color(24, 30, 44, 120);
                border = new Color(45, 54, 76, 120);
                text = new Color(100, 116, 139);
            }

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(px, startY, pillW, 26, 10, 10));

            g2.setColor(border);
            g2.setStroke(new BasicStroke(isActive ? 1.5f : 1.0f));
            g2.draw(new RoundRectangle2D.Float(px, startY, pillW - 1, 25, 10, 10));

            g2.setColor(text);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            String label = isCompleted ? "✓ " + stages[i] : stages[i];
            int tx = px + (pillW - fm.stringWidth(label)) / 2;
            int ty = startY + (26 - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(label, tx, ty);
        }
    }

    // =====================================================================
    //  Public API
    // =====================================================================

    /** Update status text in bottom-right corner and dynamically detect pipeline stage. */
    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> {
            String lower = text.toLowerCase();
            if (lower.contains("manifest") || lower.contains("version") || lower.contains("fabric")) {
                currentStage = 1;
            } else if (lower.contains("asset") || lower.contains("texture") || lower.contains("sound")) {
                currentStage = 2;
            } else if (lower.contains("library") || lower.contains("libraries") || lower.contains("native")) {
                currentStage = 3;
            } else if (lower.contains("start") || lower.contains("launch") || lower.contains("jvm") || lower.contains("game")) {
                currentStage = 4;
            }

            statusLabel.setText("● " + text);
            doLayout();
            repaint();
        });
    }

    /** Update progress bar (0–100) and percentage indicator. */
    public void setProgress(int value) {
        SwingUtilities.invokeLater(() -> {
            int clamped = Math.min(100, Math.max(0, value));
            progressBar.setValue(clamped);
            percentLabel.setText(clamped + "%");
            repaint();
        });
    }

    public void setOnCancel(Runnable r) { this.onCancel = r; }

    /** Show the overlay on top of the given layered pane. */
    public void showOn(JLayeredPane layeredPane) {
        setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        layeredPane.add(this, JLayeredPane.MODAL_LAYER);
        layeredPane.revalidate();
        currentStage = 1;
        progressBar.setValue(0);
        percentLabel.setText("0%");
        pulseTimer.start();
        setVisible(true);
        repaint();
    }

    /** Hide and remove overlay from the layered pane. */
    public void hideFrom(JLayeredPane layeredPane) {
        pulseTimer.stop();
        setVisible(false);
        layeredPane.remove(this);
        layeredPane.revalidate();
        layeredPane.repaint();
    }
}
