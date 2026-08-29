package net.matzified.iceclient.launcher.ui;

import net.matzified.iceclient.launcher.theme.ThemeManager;
import net.matzified.iceclient.launcher.theme.ThemeManager.ClientTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class ThemeSelectorDialog extends JDialog {

    private final ThemeManager themeManager = ThemeManager.getInstance();

    public ThemeSelectorDialog(Window owner) {
        super(owner, "Select Launcher Theme", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setSize(540, 480);
        setLocationRelativeTo(owner);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(15, 17, 23));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));

                ClientTheme t = themeManager.getCurrentTheme();
                g2.setColor(t.getAccentColor());
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header (Title + Close Button)
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        FrozenLabel title = new FrozenLabel("CLIENT THEME SELECTOR", 18);

        JLabel closeBtn = new JLabel("✕");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        closeBtn.setForeground(new Color(156, 163, 175));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(new Color(239, 68, 68));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(new Color(156, 163, 175));
            }
        });

        header.add(title, BorderLayout.WEST);
        header.add(closeBtn, BorderLayout.EAST);
        mainPanel.add(header, BorderLayout.NORTH);

        // Theme Options Cards List
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        for (ClientTheme theme : ClientTheme.values()) {
            listPanel.add(createThemeCard(theme));
            listPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(new Color(15, 17, 23));
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        mainPanel.add(scroll, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createThemeCard(ClientTheme theme) {
        boolean isSelected = (themeManager.getCurrentTheme() == theme);

        JPanel card = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = isSelected ? new Color(26, 32, 46) : (getModel().isRollover() ? new Color(22, 26, 36) : new Color(18, 21, 29));
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));

                if (isSelected) {
                    g2.setColor(theme.getAccentColor());
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                } else {
                    g2.setColor(new Color(36, 42, 56));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                }

                g2.dispose();
                super.paintComponent(g);
            }

            private ButtonModel getModel() {
                return new DefaultButtonModel();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 16, 10, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Gem Icon Thumbnail (44x44)
        ImageIcon icon = themeManager.getThemedIconFor(theme, 44, 44);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setPreferredSize(new Dimension(44, 44));

        // Title & Description
        JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel(theme.getDisplayName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(isSelected ? theme.getAccentColor() : Color.WHITE);

        JLabel descLabel = new JLabel("Official " + theme.getDisplayName() + " Gem & Accent Colors");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLabel.setForeground(new Color(148, 163, 184));

        info.add(nameLabel);
        info.add(descLabel);

        // Right Selection Status Pill
        JPanel rightCol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        rightCol.setOpaque(false);

        if (isSelected) {
            JLabel activeBadge = new JLabel("✓ Active");
            activeBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
            activeBadge.setForeground(theme.getAccentColor());
            rightCol.add(activeBadge);
        } else {
            JLabel selectBtn = new JLabel("Select");
            selectBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            selectBtn.setForeground(new Color(148, 163, 184));
            rightCol.add(selectBtn);
        }

        card.add(iconLabel, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(rightCol, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                themeManager.setTheme(theme);
                dispose();
            }
        });

        return card;
    }
}
