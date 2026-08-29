package net.matzified.iceclient.launcher.ui;

import net.matzified.iceclient.launcher.model.Profile;
import net.matzified.iceclient.launcher.profile.ProfileManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.List;

public class ProfileManagerPanel extends JPanel {

    private final ProfileManager profileManager = ProfileManager.getInstance();
    private final Runnable onActiveProfileChanged;
    private final Runnable onCreateProfileRequested;

    private JPanel profileCardsPanel;
    private JTextField searchField;

    public ProfileManagerPanel(Runnable onActiveProfileChanged, Runnable onCreateProfileRequested) {
        this.onActiveProfileChanged = onActiveProfileChanged;
        this.onCreateProfileRequested = onCreateProfileRequested;

        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(9, 12, 18));
        setBorder(new EmptyBorder(22, 28, 20, 28));

        // 1. Top Header Bar
        JPanel topHeader = new JPanel(new BorderLayout(14, 0));
        topHeader.setOpaque(false);

        JPanel titleWrapper = new JPanel();
        titleWrapper.setLayout(new BoxLayout(titleWrapper, BoxLayout.Y_AXIS));
        titleWrapper.setOpaque(false);

        JLabel title = new JLabel("PROFILES & INSTANCES");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(56, 189, 248));
        titleWrapper.add(title);

        JLabel subtitle = new JLabel("Manage isolated Minecraft instances with custom versions, mods, and 1000 FPS JVM presets.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(148, 163, 184));
        titleWrapper.add(subtitle);

        topHeader.add(titleWrapper, BorderLayout.WEST);

        // Right Actions (Search & + Create Profile Button)
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        JButton newProfileBtn = new JButton("＋ New Profile") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(16, 185, 129) : new Color(5, 150, 105);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        newProfileBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        newProfileBtn.setForeground(Color.WHITE);
        newProfileBtn.setFocusPainted(false);
        newProfileBtn.setBorderPainted(false);
        newProfileBtn.setContentAreaFilled(false);
        newProfileBtn.setPreferredSize(new Dimension(140, 36));
        newProfileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newProfileBtn.addActionListener(e -> {
            if (onCreateProfileRequested != null) onCreateProfileRequested.run();
        });
        rightActions.add(newProfileBtn);

        topHeader.add(rightActions, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // 2. Profile Cards Scrollable View
        profileCardsPanel = new JPanel();
        profileCardsPanel.setLayout(new GridLayout(0, 2, 16, 16));
        profileCardsPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(profileCardsPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        refreshProfiles();
    }

    public void refreshProfiles() {
        profileCardsPanel.removeAll();
        List<Profile> profiles = profileManager.getProfiles();
        Profile active = profileManager.getActiveProfile();

        for (Profile p : profiles) {
            boolean isActive = active != null && p.getId().equals(active.getId());
            profileCardsPanel.add(createProfileCard(p, isActive));
        }

        profileCardsPanel.revalidate();
        profileCardsPanel.repaint();
    }

    private JPanel createProfileCard(Profile p, boolean isActive) {
        JPanel card = new JPanel(new BorderLayout(12, 12)) {
            private boolean isHover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { isHover = true; repaint(); }
                    @Override
                    public void mouseExited(MouseEvent e) { isHover = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isActive ? new Color(18, 26, 42) : (isHover ? new Color(20, 25, 38) : new Color(14, 18, 28));
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                
                Color border = isActive ? new Color(56, 189, 248, 160) : (isHover ? new Color(56, 189, 248, 80) : new Color(255, 255, 255, 15));
                g2.setColor(border);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setPreferredSize(new Dimension(380, 140));

        // Top Row: Name + Version Badge
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel nameLbl = new JLabel("🧊 " + p.getName());
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameLbl.setForeground(Color.WHITE);
        topRow.add(nameLbl, BorderLayout.WEST);

        JLabel verBadge = new JLabel(" " + p.getMcVersion() + " Fabric ");
        verBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        verBadge.setForeground(new Color(56, 189, 248));
        verBadge.setOpaque(true);
        verBadge.setBackground(new Color(24, 34, 52));
        verBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248, 100), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));
        topRow.add(verBadge, BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);

        // Center Details: Description & RAM info
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        String desc = p.getDescription();
        if (desc == null || desc.isEmpty()) desc = "Isolated Minecraft game instance.";
        if (desc.length() > 65) desc = desc.substring(0, 62) + "...";
        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setForeground(new Color(148, 163, 184));
        center.add(descLbl);

        center.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel ramLbl = new JLabel("⚡ Memory: " + p.getRamGb() + " GB RAM Allocated • 1000 FPS Turbo Engine");
        ramLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        ramLbl.setForeground(new Color(100, 116, 139));
        center.add(ramLbl);

        card.add(center, BorderLayout.CENTER);

        // Bottom Actions Toolbar
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);

        // Left Action Pills: Open Folder
        JButton folderBtn = new JButton("📂 Folder") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(34, 42, 60) : new Color(24, 30, 44);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        folderBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        folderBtn.setForeground(new Color(203, 213, 225));
        folderBtn.setFocusPainted(false);
        folderBtn.setBorderPainted(false);
        folderBtn.setContentAreaFilled(false);
        folderBtn.setPreferredSize(new Dimension(84, 28));
        folderBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        folderBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(p.getGameDir()));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not open folder: " + ex.getMessage());
            }
        });
        bottomRow.add(folderBtn, BorderLayout.WEST);

        // Right Action: Select / Active Pill
        JButton activeBtn = new JButton(isActive ? "✓ ACTIVE" : "▶ Select") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isActive ? new Color(16, 185, 129) : (getModel().isRollover() ? new Color(2, 132, 199) : new Color(3, 105, 161));
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        activeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        activeBtn.setForeground(Color.WHITE);
        activeBtn.setFocusPainted(false);
        activeBtn.setBorderPainted(false);
        activeBtn.setContentAreaFilled(false);
        activeBtn.setPreferredSize(new Dimension(100, 28));
        activeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        activeBtn.addActionListener(e -> {
            profileManager.setActiveProfile(p);
            refreshProfiles();
            if (onActiveProfileChanged != null) onActiveProfileChanged.run();
        });
        bottomRow.add(activeBtn, BorderLayout.EAST);

        card.add(bottomRow, BorderLayout.SOUTH);

        return card;
    }
}
