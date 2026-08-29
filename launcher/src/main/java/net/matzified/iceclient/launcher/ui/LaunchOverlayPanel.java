package net.matzified.iceclient.launcher.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Full-window frosted-glass pre-launch overlay.
 * Features central animated breathing aura, 4-stage pipeline indicators,
 * live mod downloading badges, and progress bar.
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
    private String activeModName = "Sodium";

    private static final String[] OPTIMIZATION_MODS = {
            "⚡ Sodium", "⚡ Lithium", "⚡ FerriteCore", "⚡ ImmediatelyFast", "⚡ Iris", "⚡ Krypton"
    };

    public LaunchOverlayPanel() {
        setLayout(null);
        setOpaque(false);

        // Status label
        statusLabel = new JLabel("Initializing launch pipeline...");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(new Color(186, 230, 253));
        add(statusLabel);

        // Percentage label
        percentLabel = new JLabel("0%", SwingConstants.RIGHT);
        percentLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        percentLabel.setForeground(new Color(56, 189, 248));
        add(percentLabel);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setOpaque(false);
        progressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth(), h = c.getHeight();

                g2.setColor(new Color(20, 25, 38));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                g2.setColor(new Color(40, 48, 68));
                g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 10, 10));

                int fillW = (int) ((w - 2) * (progressBar.getPercentComplete()));
                if (fillW > 0) {
                    GradientPaint gp = new GradientPaint(
                            0, 0, new Color(2, 132, 199),
                            fillW, 0, new Color(56, 189, 248)
                    );
                    g2.setPaint(gp);
                    g2.fill(new RoundRectangle2D.Float(1, 1, fillW, h - 2, 8, 8));
                }
                g2.dispose();
            }
        });
        add(progressBar);

        // Cancel button
        cancelBtn = new JButton("✕ CANCEL") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover()
                        ? new Color(239, 68, 68, 160)
                        : new Color(30, 36, 52, 200);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(new Color(239, 68, 68, 120));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cancelBtn.setForeground(new Color(254, 202, 202));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setContentAreaFilled(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> {
            if (onCancel != null) onCancel.run();
        });
        add(cancelBtn);

        pulseTimer = new Timer(25, e -> {
            if (pulseUp) {
                pulseAlpha += 0.025f;
                if (pulseAlpha >= 1f) { pulseAlpha = 1f; pulseUp = false; }
            } else {
                pulseAlpha -= 0.025f;
                if (pulseAlpha <= 0f) { pulseAlpha = 0f; pulseUp = true; }
            }
            rotateAngle = (rotateAngle + 2.5f) % 360f;
            repaint();
        });
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int w = getWidth(), h = getHeight();
        int barW = Math.min(540, w - 80);
        int barH = 10;
        int barX = (w - barW) / 2;
        int barY = h - 65;

        progressBar.setBounds(barX, barY, barW, barH);
        percentLabel.setBounds(barX + barW - 60, barY - 22, 60, 18);
        statusLabel.setBounds(barX, barY - 22, barW - 70, 18);
        cancelBtn.setBounds(w - 120, h - 35, 95, 24);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int cx = w / 2, cy = h / 2 - 50;

        // Dark frosted backdrop
        g2.setColor(new Color(7, 10, 16, 240));
        g2.fillRect(0, 0, w, h);

        // Outer rotating glowing ring
        int ringR = 85;
        g2.translate(cx, cy);
        g2.rotate(Math.toRadians(rotateAngle));
        for (int i = 0; i < 8; i++) {
            float a = (float) (i * Math.PI / 4.0);
            int dotX = (int) (Math.cos(a) * ringR);
            int dotY = (int) (Math.sin(a) * ringR);
            int dotAlpha = (int) (40 + (i * 22));
            g2.setColor(new Color(56, 189, 248, dotAlpha));
            g2.fill(new Ellipse2D.Float(dotX - 4, dotY - 4, 8, 8));
        }
        g2.rotate(-Math.toRadians(rotateAngle));
        g2.translate(-cx, -cy);

        // Center Icon Circle
        int coreR = 56;
        g2.setColor(new Color(18, 26, 44));
        g2.fill(new Ellipse2D.Float(cx - coreR, cy - coreR, coreR * 2, coreR * 2));
        g2.setColor(new Color(56, 189, 248, 160));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new Ellipse2D.Float(cx - coreR, cy - coreR, coreR * 2, coreR * 2));

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 36));
        FontMetrics fmc = g2.getFontMetrics();
        String emblem = "🧊";
        g2.drawString(emblem, cx - fmc.stringWidth(emblem) / 2, cy + 13);

        // Titles
        g2.setColor(new Color(56, 189, 248));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
        String brand = "ICE CLIENT";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(brand, cx - fm.stringWidth(brand) / 2, cy + 86);

        // 4-Stage Pipeline Status Pills
        paintPipelineStages(g2, cx, cy + 115);

        // Live Optimization Suite Badges
        paintModBadges(g2, cx, cy + 155);

        g2.dispose();
        super.paintComponent(g);
    }

    private void paintPipelineStages(Graphics2D g2, int cx, int startY) {
        String[] stages = {"1. Manifest", "2. Assets", "3. Libraries", "4. Boot JVM"};
        int pillW = 100;
        int gap = 10;
        int totalW = (stages.length * pillW) + ((stages.length - 1) * gap);
        int startX = cx - totalW / 2;

        for (int i = 0; i < stages.length; i++) {
            int px = startX + (i * (pillW + gap));
            int stageNum = i + 1;
            boolean isActive = (currentStage == stageNum);
            boolean isCompleted = (currentStage > stageNum);

            Color bg = isCompleted ? new Color(16, 185, 129, 45) : (isActive ? new Color(14, 165, 233, 60) : new Color(24, 30, 44, 120));
            Color border = isCompleted ? new Color(16, 185, 129, 180) : (isActive ? new Color(56, 189, 248, 220) : new Color(45, 54, 76, 120));
            Color text = isCompleted ? new Color(52, 211, 153) : (isActive ? new Color(224, 242, 254) : new Color(100, 116, 139));

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(px, startY, pillW, 24, 8, 8));
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Float(px, startY, pillW - 1, 23, 8, 8));

            g2.setColor(text);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            String label = isCompleted ? "✓ " + stages[i] : stages[i];
            int tx = px + (pillW - fm.stringWidth(label)) / 2;
            int ty = startY + (24 - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(label, tx, ty);
        }
    }

    private void paintModBadges(Graphics2D g2, int cx, int startY) {
        int pillW = 90;
        int gap = 8;
        int totalW = (OPTIMIZATION_MODS.length * pillW) + ((OPTIMIZATION_MODS.length - 1) * gap);
        int startX = cx - totalW / 2;

        for (int i = 0; i < OPTIMIZATION_MODS.length; i++) {
            int px = startX + (i * (pillW + gap));
            String mod = OPTIMIZATION_MODS[i];
            boolean isThisMod = activeModName.toLowerCase().contains(mod.replace("⚡ ", "").toLowerCase());

            Color bg = isThisMod ? new Color(56, 189, 248, 60) : new Color(18, 24, 36, 160);
            Color border = isThisMod ? new Color(56, 189, 248, 220) : new Color(40, 48, 68, 120);
            Color text = isThisMod ? new Color(224, 242, 254) : new Color(148, 163, 184);

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(px, startY, pillW, 22, 6, 6));
            g2.setColor(border);
            g2.draw(new RoundRectangle2D.Float(px, startY, pillW - 1, 21, 6, 6));

            g2.setColor(text);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int tx = px + (pillW - fm.stringWidth(mod)) / 2;
            int ty = startY + (22 - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(mod, tx, ty);
        }
    }

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

            for (String m : OPTIMIZATION_MODS) {
                String clean = m.replace("⚡ ", "").toLowerCase();
                if (lower.contains(clean)) {
                    activeModName = clean;
                    break;
                }
            }

            statusLabel.setText("● " + text);
            doLayout();
            repaint();
        });
    }

    public void setProgress(int value) {
        SwingUtilities.invokeLater(() -> {
            int clamped = Math.min(100, Math.max(0, value));
            progressBar.setValue(clamped);
            percentLabel.setText(clamped + "%");
            repaint();
        });
    }

    public void setOnCancel(Runnable r) { this.onCancel = r; }

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

    public void hideFrom(JLayeredPane layeredPane) {
        pulseTimer.stop();
        setVisible(false);
        layeredPane.remove(this);
        layeredPane.revalidate();
        layeredPane.repaint();
    }
}
