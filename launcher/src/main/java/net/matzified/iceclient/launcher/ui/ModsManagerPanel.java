package net.matzified.iceclient.launcher.ui;

import net.matzified.iceclient.launcher.model.Profile;
import net.matzified.iceclient.launcher.profile.ProfileManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class ModsManagerPanel extends JPanel {

    private final ProfileManager profileManager = ProfileManager.getInstance();
    private JPanel modsListPanel;
    private JLabel activeProfileLabel;
    private JTextField searchFilterField;
    private String currentSearchFilter = "";

    public ModsManagerPanel() {
        setLayout(new BorderLayout(18, 18));
        setBackground(new Color(13, 15, 20));
        setBorder(new EmptyBorder(22, 28, 22, 28));

        // Header Section
        JPanel topSection = new JPanel(new BorderLayout(14, 12));
        topSection.setOpaque(false);

        JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleGroup.setOpaque(false);
        FrozenLabel titleLabel = new FrozenLabel("INSTALLED MODS & PACKS", 22);
        titleGroup.add(titleLabel);

        activeProfileLabel = new JLabel("Active Profile: Ice Client 1.21.1 (Optimized)");
        activeProfileLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        activeProfileLabel.setForeground(new Color(56, 189, 248));

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        headerText.add(titleGroup);
        headerText.add(Box.createRigidArea(new Dimension(0, 4)));
        headerText.add(activeProfileLabel);

        // Action Toolbar: [Search] [+ Add Custom Mod] [Enable All] [Disable All] [Open Folder]
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setOpaque(false);

        searchFilterField = new JTextField(12);
        searchFilterField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchFilterField.setBackground(new Color(20, 24, 34));
        searchFilterField.setForeground(Color.WHITE);
        searchFilterField.setCaretColor(new Color(56, 189, 248));
        searchFilterField.setPreferredSize(new Dimension(160, 36));
        searchFilterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(42, 50, 68), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        searchFilterField.setToolTipText("Filter installed mods...");
        searchFilterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSearch(); }
            private void updateSearch() {
                currentSearchFilter = searchFilterField.getText().trim().toLowerCase();
                refreshModsList();
            }
        });

        JButton addModBtn = createPrimaryButton("+ Add Mod File", e -> addCustomMod());
        JButton enableAllBtn = createSecondaryButton("Enable All", e -> setAllModsState(true));
        JButton disableAllBtn = createSecondaryButton("Disable All", e -> setAllModsState(false));
        JButton openFolderBtn = createSecondaryButton("📂 Mods Folder", e -> openModsFolder());

        toolbar.add(searchFilterField);
        toolbar.add(addModBtn);
        toolbar.add(enableAllBtn);
        toolbar.add(disableAllBtn);
        toolbar.add(openFolderBtn);

        topSection.add(headerText, BorderLayout.WEST);
        topSection.add(toolbar, BorderLayout.EAST);
        add(topSection, BorderLayout.NORTH);

        // Center Content Area: Mod Cards List
        modsListPanel = new JPanel();
        modsListPanel.setLayout(new BoxLayout(modsListPanel, BoxLayout.Y_AXIS));
        modsListPanel.setBackground(new Color(13, 15, 20));

        JScrollPane scrollPane = new JScrollPane(modsListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(13, 15, 20));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        refreshModsList();
    }

    public void refreshModsList() {
        modsListPanel.removeAll();
        Profile activeProfile = profileManager.getActiveProfile();
        if (activeProfile != null) {
            activeProfileLabel.setText("Active Profile: " + activeProfile.getName() + " (" + activeProfile.getMcVersion() + ")");
            File modsDir = activeProfile.getModsDir();
            if (modsDir.exists() && modsDir.isDirectory()) {
                File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".jar") || name.endsWith(".disabled") || name.endsWith(".zip") || name.endsWith(".mrpack"));
                if (files != null && files.length > 0) {
                    int count = 0;
                    for (File f : files) {
                        if (currentSearchFilter.isEmpty() || f.getName().toLowerCase().contains(currentSearchFilter)) {
                            modsListPanel.add(createLunarModCard(f));
                            modsListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                            count++;
                        }
                    }
                    if (count == 0) {
                        showNoMatchState();
                    }
                } else {
                    showEmptyState();
                }
            } else {
                showEmptyState();
            }
        } else {
            showEmptyState();
        }
        modsListPanel.revalidate();
        modsListPanel.repaint();
    }

    private void showEmptyState() {
        JLabel emptyLbl = new JLabel("No mods installed in this profile yet. Click '+ Add Mod File' or browse the Mod Store!", SwingConstants.CENTER);
        emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        emptyLbl.setForeground(new Color(148, 163, 184));
        emptyLbl.setBorder(new EmptyBorder(40, 0, 0, 0));
        modsListPanel.add(emptyLbl);
    }

    private void showNoMatchState() {
        JLabel emptyLbl = new JLabel("No mods found matching '" + currentSearchFilter + "'", SwingConstants.CENTER);
        emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        emptyLbl.setForeground(new Color(148, 163, 184));
        emptyLbl.setBorder(new EmptyBorder(40, 0, 0, 0));
        modsListPanel.add(emptyLbl);
    }

    private void setAllModsState(boolean enable) {
        Profile activeProfile = profileManager.getActiveProfile();
        if (activeProfile == null) return;
        File modsDir = activeProfile.getModsDir();
        if (!modsDir.exists() || !modsDir.isDirectory()) return;

        File[] files = modsDir.listFiles();
        if (files == null) return;

        for (File f : files) {
            String name = f.getName();
            if (enable && name.endsWith(".disabled")) {
                f.renameTo(new File(modsDir, name.replace(".disabled", "")));
            } else if (!enable && !name.endsWith(".disabled") && (name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".mrpack"))) {
                f.renameTo(new File(modsDir, name + ".disabled"));
            }
        }
        refreshModsList();
    }

    private JPanel createLunarModCard(File modFile) {
        boolean[] isHover = {false};
        boolean isEnabled = !modFile.getName().endsWith(".disabled");
        String filename = modFile.getName().replace(".disabled", "");
        String cleanTitle = formatModTitle(filename);
        long sizeKb = modFile.length() / 1024;
        String sizeStr = sizeKb > 1024 ? String.format("%.1f MB", sizeKb / 1024.0) : sizeKb + " KB";

        JPanel card = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = isHover[0] ? new Color(28, 34, 48) : new Color(20, 24, 34);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));

                g2.setColor(isHover[0] ? new Color(56, 189, 248, 140) : new Color(42, 50, 68));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 18, 12, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHover[0] = true;
                card.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isHover[0] = false;
                card.repaint();
            }
        });

        // 48x48 Mod Icon Badge
        JLabel iconContainer = new JLabel(createModIconBadge(cleanTitle, isEnabled));
        iconContainer.setPreferredSize(new Dimension(48, 48));

        // Center Info: Clean Title + Filename Subtitle
        JPanel centerInfo = new JPanel(new GridLayout(2, 1, 0, 2));
        centerInfo.setOpaque(false);

        JLabel titleLbl = new JLabel(cleanTitle);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(isEnabled ? Color.WHITE : new Color(148, 163, 184));

        JLabel subLbl = new JLabel(filename + "  •  " + sizeStr);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(100, 116, 139));

        centerInfo.add(titleLbl);
        centerInfo.add(subLbl);

        // Right Controls: Toggle Switch (ON / OFF) & Delete Button
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        rightControls.setOpaque(false);

        JButton toggleBtn = new JButton(isEnabled ? "ACTIVE" : "DISABLED") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color bg = isEnabled ? new Color(16, 185, 129) : new Color(55, 65, 81);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };
        toggleBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        toggleBtn.setForeground(Color.WHITE);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setPreferredSize(new Dimension(84, 32));
        toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        toggleBtn.addActionListener(e -> {
            toggleModState(modFile);
            refreshModsList();
        });

        JButton deleteBtn = new JButton("Delete") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color bg = getModel().isRollover() ? new Color(225, 29, 72) : new Color(159, 18, 57);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };
        deleteBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setContentAreaFilled(false);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setPreferredSize(new Dimension(75, 32));
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Delete mod '" + filename + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                modFile.delete();
                refreshModsList();
            }
        });

        rightControls.add(toggleBtn);
        rightControls.add(deleteBtn);

        card.add(iconContainer, BorderLayout.WEST);
        card.add(centerInfo, BorderLayout.CENTER);
        card.add(rightControls, BorderLayout.EAST);
        return card;
    }

    private String formatModTitle(String filename) {
        String name = filename.replace(".jar", "").replace(".zip", "").replace(".mrpack", "");
        if (name.contains("-")) name = name.substring(0, name.indexOf('-'));
        if (name.contains("_")) name = name.substring(0, name.indexOf('_'));

        if (name.equalsIgnoreCase("sodium")) return "Sodium (Rendering Engine)";
        if (name.equalsIgnoreCase("iris")) return "Iris Shaders";
        if (name.equalsIgnoreCase("fabric")) return "Fabric API";
        if (name.equalsIgnoreCase("cloth")) return "Cloth Config API";
        if (name.equalsIgnoreCase("entity")) return "Entity Culling";
        if (name.equalsIgnoreCase("ferritecore")) return "FerriteCore Memory Saver";
        if (name.equalsIgnoreCase("modmenu")) return "Mod Menu UI";

        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private ImageIcon createModIconBadge(String title, boolean isEnabled) {
        BufferedImage img = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = isEnabled ? new Color(30, 38, 54) : new Color(24, 28, 38);
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, 48, 48, 12, 12));

        Color fg = isEnabled ? new Color(56, 189, 248) : new Color(100, 116, 139);
        g2.setColor(fg);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));

        String letter = title.isEmpty() ? "M" : String.valueOf(title.charAt(0)).toUpperCase();
        FontMetrics fm = g2.getFontMetrics();
        int x = (48 - fm.stringWidth(letter)) / 2;
        int y = ((48 - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(letter, x, y);
        g2.dispose();

        return new ImageIcon(img);
    }

    private void toggleModState(File modFile) {
        try {
            if (modFile.getName().endsWith(".disabled")) {
                String newName = modFile.getName().replace(".disabled", "");
                modFile.renameTo(new File(modFile.getParentFile(), newName));
            } else {
                String newName = modFile.getName() + ".disabled";
                modFile.renameTo(new File(modFile.getParentFile(), newName));
            }
        } catch (Exception ignored) {}
    }

    private void addCustomMod() {
        Profile active = profileManager.getActiveProfile();
        if (active == null) return;

        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        FileDialog dialog = new FileDialog(parentFrame, "Select Custom Mod (.jar)", FileDialog.LOAD);
        dialog.setFile("*.jar;*.zip;*.mrpack");
        dialog.setVisible(true);

        String file = dialog.getFile();
        String dir = dialog.getDirectory();

        if (file != null && dir != null) {
            File selectedFile = new File(dir, file);
            File dest = new File(active.getModsDir(), selectedFile.getName());
            try {
                java.nio.file.Files.copy(selectedFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                refreshModsList();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to copy mod file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openModsFolder() {
        Profile active = profileManager.getActiveProfile();
        if (active == null) return;
        try {
            Desktop.getDesktop().open(active.getModsDir());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to open folder: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton createPrimaryButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color top = getModel().isRollover() ? new Color(56, 189, 248) : new Color(14, 165, 233);
                Color bottom = getModel().isRollover() ? new Color(2, 132, 199) : new Color(3, 105, 161);
                GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(145, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        return btn;
    }

    private JButton createSecondaryButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color bg = getModel().isRollover() ? new Color(34, 40, 54) : new Color(22, 26, 36);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(new Color(45, 52, 68));
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
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(125, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        return btn;
    }
}
