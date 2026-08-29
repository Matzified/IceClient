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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ModsManagerPanel extends JPanel {

    private final ProfileManager profileManager = ProfileManager.getInstance();
    private JPanel modsGridPanel;
    private JTextField searchField;
    private JLabel countLabel;

    public ModsManagerPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(9, 12, 18));
        setBorder(new EmptyBorder(22, 28, 20, 28));

        // 1. Header Row
        JPanel topHeader = new JPanel(new BorderLayout(14, 0));
        topHeader.setOpaque(false);

        JPanel titleWrapper = new JPanel();
        titleWrapper.setLayout(new BoxLayout(titleWrapper, BoxLayout.Y_AXIS));
        titleWrapper.setOpaque(false);

        JLabel title = new JLabel("INSTALLED MODS & ADDONS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(56, 189, 248));
        titleWrapper.add(title);

        Profile active = profileManager.getActiveProfile();
        String pName = active != null ? active.getName() : "Vanilla (1.21.1)";
        countLabel = new JLabel("Instance: " + pName + " • 0 mods loaded");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        countLabel.setForeground(new Color(148, 163, 184));
        titleWrapper.add(countLabel);

        topHeader.add(titleWrapper, BorderLayout.WEST);

        // Action Buttons & Search
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        // Search Bar
        JPanel searchBox = new JPanel(new BorderLayout(6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 22, 34));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(new Color(56, 189, 248, 60));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        searchBox.setOpaque(false);
        searchBox.setPreferredSize(new Dimension(200, 36));
        searchBox.setBorder(new EmptyBorder(0, 10, 0, 10));

        JLabel searchIcon = new JLabel("🔍");
        searchBox.add(searchIcon, BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(new Color(56, 189, 248));
        searchField.setOpaque(false);
        searchField.setBorder(null);
        searchField.addActionListener(e -> refreshMods());
        searchBox.add(searchField, BorderLayout.CENTER);

        rightActions.add(searchBox);

        // Add Mod File Button
        JButton addModBtn = new JButton("＋ Add Mod File") {
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
        addModBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addModBtn.setForeground(Color.WHITE);
        addModBtn.setFocusPainted(false);
        addModBtn.setBorderPainted(false);
        addModBtn.setContentAreaFilled(false);
        addModBtn.setPreferredSize(new Dimension(140, 36));
        addModBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addModBtn.addActionListener(e -> onAddModClicked());
        rightActions.add(addModBtn);

        // Open Folder Button
        JButton folderBtn = new JButton("📂 Open Folder") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(34, 42, 60) : new Color(22, 28, 40);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        folderBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        folderBtn.setForeground(new Color(203, 213, 225));
        folderBtn.setFocusPainted(false);
        folderBtn.setBorderPainted(false);
        folderBtn.setContentAreaFilled(false);
        folderBtn.setPreferredSize(new Dimension(130, 36));
        folderBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        folderBtn.addActionListener(e -> onOpenFolderClicked());
        rightActions.add(folderBtn);

        topHeader.add(rightActions, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // 2. Mods Grid
        modsGridPanel = new JPanel();
        modsGridPanel.setLayout(new GridLayout(0, 2, 14, 14));
        modsGridPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(modsGridPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        refreshMods();
    }

    public void refreshMods() {
        modsGridPanel.removeAll();
        Profile p = profileManager.getActiveProfile();
        if (p == null) return;

        File modsDir = p.getModsDir();
        if (!modsDir.exists()) modsDir.mkdirs();

        File[] files = modsDir.listFiles(f -> f.getName().endsWith(".jar") || f.getName().endsWith(".jar.disabled"));
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        int count = 0;
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if (!query.isEmpty() && !name.toLowerCase().contains(query)) continue;
                modsGridPanel.add(createModCard(f));
                count++;
            }
        }

        countLabel.setText("Instance: " + p.getName() + " • " + count + " mods installed");
        modsGridPanel.revalidate();
        modsGridPanel.repaint();
    }

    private JPanel createModCard(File file) {
        boolean isEnabled = !file.getName().endsWith(".disabled");
        String displayName = file.getName().replaceAll("\\.jar(\\.disabled)?$", "");
        long sizeKb = file.length() / 1024;

        JPanel card = new JPanel(new BorderLayout(12, 0)) {
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
                Color bg = isHover ? new Color(22, 28, 42) : new Color(14, 18, 28);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                Color border = isEnabled ? new Color(56, 189, 248, 100) : new Color(255, 255, 255, 15);
                g2.setColor(border);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.setPreferredSize(new Dimension(360, 68));

        // Left Icon Pill
        JLabel icon = new JLabel(displayName.toLowerCase().contains("ice") ? "🧊" : "🧩");
        icon.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        card.add(icon, BorderLayout.WEST);

        // Center Details
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        String trimmedName = displayName.length() > 32 ? displayName.substring(0, 29) + "..." : displayName;
        JLabel nameLbl = new JLabel(trimmedName);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(isEnabled ? Color.WHITE : new Color(148, 163, 184));
        center.add(nameLbl);

        center.add(Box.createRigidArea(new Dimension(0, 3)));

        JLabel sizeLbl = new JLabel("Size: " + sizeKb + " KB • " + (isEnabled ? "Enabled" : "Disabled"));
        sizeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sizeLbl.setForeground(isEnabled ? new Color(56, 189, 248) : new Color(100, 116, 139));
        center.add(sizeLbl);

        card.add(center, BorderLayout.CENTER);

        // Right Actions (Toggle & Delete)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        right.setOpaque(false);

        // Toggle Switch
        JButton toggleBtn = new JButton(isEnabled ? "ON" : "OFF") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isEnabled ? new Color(2, 132, 199) : new Color(40, 48, 64);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        toggleBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        toggleBtn.setForeground(Color.WHITE);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setPreferredSize(new Dimension(50, 26));
        toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleBtn.addActionListener(e -> {
            File target = isEnabled ? new File(file.getParentFile(), file.getName() + ".disabled")
                                    : new File(file.getParentFile(), file.getName().replace(".disabled", ""));
            file.renameTo(target);
            refreshMods();
        });
        right.add(toggleBtn);

        // Delete Button
        JButton delBtn = new JButton("🗑") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(220, 38, 38) : new Color(30, 36, 48);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        delBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        delBtn.setForeground(Color.WHITE);
        delBtn.setFocusPainted(false);
        delBtn.setBorderPainted(false);
        delBtn.setContentAreaFilled(false);
        delBtn.setPreferredSize(new Dimension(32, 26));
        delBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        delBtn.addActionListener(e -> {
            file.delete();
            refreshMods();
        });
        right.add(delBtn);

        card.add(right, BorderLayout.EAST);

        return card;
    }

    private void onAddModClicked() {
        Profile p = profileManager.getActiveProfile();
        if (p == null) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Minecraft Mod JAR");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JAR Mods (*.jar)", "jar"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            File dest = new File(p.getModsDir(), selected.getName());
            try {
                Files.copy(selected.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                refreshMods();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to copy mod: " + ex.getMessage());
            }
        }
    }

    private void onOpenFolderClicked() {
        Profile p = profileManager.getActiveProfile();
        if (p == null) return;
        try {
            Desktop.getDesktop().open(p.getModsDir());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open folder: " + ex.getMessage());
        }
    }
}
