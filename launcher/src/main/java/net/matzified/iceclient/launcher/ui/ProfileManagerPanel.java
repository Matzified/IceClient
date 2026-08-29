package net.matzified.iceclient.launcher.ui;

import net.matzified.iceclient.launcher.model.Profile;
import net.matzified.iceclient.launcher.profile.ProfileManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;

public class ProfileManagerPanel extends JPanel {

    private final ProfileManager profileManager = ProfileManager.getInstance();

    private JList<Profile> profileList;
    private DefaultListModel<Profile> listModel;

    private JTextField nameField;
    private JTextField descField;
    private JComboBox<String> versionCombo;
    private JSlider ramSlider;
    private JLabel ramLabel;
    private JComboBox<String> jvmPresetCombo;
    private JTextField jvmArgsField;
    private JLabel gameDirLabel;
    private JLabel modDirLabel;

    private Profile editingProfile;
    private final Runnable onActiveProfileChanged;
    private final Runnable onCreateProfileRequested;

    public ProfileManagerPanel(Runnable onActiveProfileChanged, Runnable onCreateProfileRequested) {
        this.onActiveProfileChanged = onActiveProfileChanged;
        this.onCreateProfileRequested = onCreateProfileRequested;

        setLayout(new BorderLayout(20, 18));
        setBackground(new Color(13, 15, 20));
        setBorder(new EmptyBorder(22, 28, 22, 28));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout(0, 4));
        headerPanel.setOpaque(false);

        FrozenLabel titleLabel = new FrozenLabel("PROFILES & INSTANCES", 22);
        JLabel subLabel = new JLabel("Each profile operates in an isolated instance sandbox for mods, resource packs, and shaders.");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Left Panel: Saved Profiles List & Actions
        JPanel leftPanel = new JPanel(new BorderLayout(0, 12));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(360, 0));

        listModel = new DefaultListModel<>();
        profileList = new JList<>(listModel);
        profileList.setBackground(new Color(18, 22, 30));
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profileList.setFixedCellHeight(64);
        profileList.setCellRenderer(new ProfileCellRenderer());

        refreshProfileList();

        JScrollPane scrollPane = new JScrollPane(profileList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(36, 42, 56), 1));
        scrollPane.getViewport().setBackground(new Color(18, 22, 30));

        // Action Toolbar below the list
        JPanel listButtons = new JPanel(new GridLayout(1, 4, 6, 0));
        listButtons.setOpaque(false);

        JButton newBtn = createStyledButton("+ New", new Color(16, 185, 129));
        JButton activateBtn = createStyledButton("⚡ Active", new Color(14, 165, 233));
        JButton openFolderBtn = createStyledButton("📂 Folder", new Color(55, 65, 81));
        JButton deleteBtn = createStyledButton("🗑️ Delete", new Color(220, 38, 38));

        listButtons.add(newBtn);
        listButtons.add(activateBtn);
        listButtons.add(openFolderBtn);
        listButtons.add(deleteBtn);

        JLabel listHeader = new JLabel("SAVED INSTANCES (.iceclient)", SwingConstants.LEFT);
        listHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        listHeader.setForeground(new Color(148, 163, 184));

        leftPanel.add(listHeader, BorderLayout.NORTH);
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        leftPanel.add(listButtons, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);

        // Right Panel: Modern Editor Card
        JPanel editorPanel = createEditorPanel();
        add(editorPanel, BorderLayout.CENTER);

        // Listeners
        profileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Profile sel = profileList.getSelectedValue();
                if (sel != null) {
                    loadProfileToEditor(sel);
                }
            }
        });

        newBtn.addActionListener(e -> {
            if (this.onCreateProfileRequested != null) {
                this.onCreateProfileRequested.run();
            }
        });

        activateBtn.addActionListener(e -> {
            Profile sel = profileList.getSelectedValue();
            if (sel != null) {
                profileManager.setActiveProfile(sel);
                profileList.repaint();
                if (this.onActiveProfileChanged != null) {
                    this.onActiveProfileChanged.run();
                }
            }
        });

        openFolderBtn.addActionListener(e -> {
            Profile sel = profileList.getSelectedValue();
            if (sel != null && sel.getGameDir() != null) {
                try {
                    File dir = new File(sel.getGameDir());
                    if (!dir.exists()) dir.mkdirs();
                    Desktop.getDesktop().open(dir);
                } catch (Exception ignored) {}
            }
        });

        deleteBtn.addActionListener(e -> {
            Profile sel = profileList.getSelectedValue();
            if (sel != null) {
                if (profileManager.getProfiles().size() <= 1) {
                    JOptionPane.showMessageDialog(this, "You must keep at least one profile!", "Cannot Delete", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete profile '" + sel.getName() + "'?", "Delete Profile", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    profileManager.removeProfile(sel);
                    refreshProfileList();
                    if (this.onActiveProfileChanged != null) {
                        this.onActiveProfileChanged.run();
                    }
                }
            }
        });

        if (!listModel.isEmpty()) {
            profileList.setSelectedIndex(0);
        }
    }

    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(20, 24, 34));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 48, 64), 1),
                new EmptyBorder(20, 24, 20, 24)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 8, 7, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.25;
        panel.add(createFormLabel("PROFILE NAME:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.75;
        nameField = new JTextField();
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nameField.setBackground(new Color(13, 16, 23));
        nameField.setForeground(Color.WHITE);
        nameField.setCaretColor(new Color(56, 189, 248));
        panel.add(nameField, gbc);

        // Row 1: Description
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.25;
        panel.add(createFormLabel("DESCRIPTION:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.75;
        descField = new JTextField();
        descField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descField.setBackground(new Color(13, 16, 23));
        descField.setForeground(Color.WHITE);
        panel.add(descField, gbc);

        // Row 2: Version
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.25;
        panel.add(createFormLabel("MINECRAFT VERSION:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.75;
        versionCombo = new JComboBox<>(ProfileManager.SUPPORTED_VERSIONS);
        versionCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        versionCombo.setBackground(new Color(13, 16, 23));
        versionCombo.setForeground(Color.WHITE);
        panel.add(versionCombo, gbc);

        // Row 3: Allocated RAM Slider
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.25;
        panel.add(createFormLabel("ALLOCATED MEMORY:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.75;
        JPanel ramPanel = new JPanel(new BorderLayout(10, 0));
        ramPanel.setOpaque(false);

        ramSlider = new JSlider(2, 16, 4);
        ramSlider.setMajorTickSpacing(2);
        ramSlider.setMinorTickSpacing(1);
        ramSlider.setPaintTicks(true);
        ramSlider.setPaintLabels(true);
        ramSlider.setOpaque(false);
        ramSlider.setForeground(new Color(148, 163, 184));

        ramLabel = new JLabel("4.0 GB", SwingConstants.CENTER);
        ramLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        ramLabel.setForeground(new Color(56, 189, 248));
        ramLabel.setOpaque(true);
        ramLabel.setBackground(new Color(13, 16, 23));
        ramLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248, 100), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));

        ramSlider.addChangeListener(e -> {
            int val = ramSlider.getValue();
            ramLabel.setText(val + ".0 GB");
            if (val <= 4) {
                ramLabel.setForeground(new Color(52, 211, 153));
            } else if (val <= 8) {
                ramLabel.setForeground(new Color(56, 189, 248));
            } else if (val <= 12) {
                ramLabel.setForeground(new Color(251, 191, 36));
            } else {
                ramLabel.setForeground(new Color(248, 113, 113));
            }
        });

        ramPanel.add(ramSlider, BorderLayout.CENTER);
        ramPanel.add(ramLabel, BorderLayout.EAST);
        panel.add(ramPanel, gbc);

        // Row 4: JVM Optimization Presets
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.25;
        panel.add(createFormLabel("JVM OPTIMIZATION:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.75;
        String[] presets = {
            "⚡ 1000 FPS Ultra (Aikar G1GC Mega-Throughput)",
            "🚀 1000 FPS Zero-Lag (Java 21 Generational ZGC)",
            "Default / Custom"
        };
        jvmPresetCombo = new JComboBox<>(presets);
        jvmPresetCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        jvmPresetCombo.setBackground(new Color(13, 16, 23));
        jvmPresetCombo.setForeground(Color.WHITE);
        jvmPresetCombo.addActionListener(e -> {
            int idx = jvmPresetCombo.getSelectedIndex();
            if (idx == 0) {
                jvmArgsField.setText("-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=30 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=35 -XX:G1MaxNewSizePercent=45 -XX:G1ReservePercent=15 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Diceclient.ultrafps=true");
            } else if (idx == 1) {
                jvmArgsField.setText("-XX:+UseZGC -XX:+ZGenerational -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch -XX:+UseNUMA -XX:+OptimizeStringConcat -XX:+UseStringDeduplication -Dsun.java2d.opengl=true -Dsun.java2d.d3d=true -Diceclient.ultrafps=true");
            }
        });
        panel.add(jvmPresetCombo, gbc);

        // Row 5: JVM Flags Raw String
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.25;
        panel.add(createFormLabel("CUSTOM JVM ARGS:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.75;
        jvmArgsField = new JTextField();
        jvmArgsField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        jvmArgsField.setBackground(new Color(13, 16, 23));
        jvmArgsField.setForeground(Color.WHITE);
        panel.add(jvmArgsField, gbc);

        // Row 6: Directory Display Box
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.weightx = 1.0;
        JPanel folderBox = new JPanel(new GridLayout(2, 1, 4, 4));
        folderBox.setBackground(new Color(13, 16, 23));
        folderBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(36, 42, 56)),
                new EmptyBorder(8, 12, 8, 12)
        ));

        gameDirLabel = new JLabel("📁 Instance Game Directory: -");
        gameDirLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gameDirLabel.setForeground(new Color(148, 163, 184));

        modDirLabel = new JLabel("📦 Instance Mods Directory: -");
        modDirLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        modDirLabel.setForeground(new Color(148, 163, 184));

        folderBox.add(gameDirLabel);
        folderBox.add(modDirLabel);
        panel.add(folderBox, gbc);

        // Row 7: Save Button
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; gbc.weightx = 1.0;
        JButton saveBtn = createStyledButton("✓  SAVE PROFILE CHANGES", new Color(14, 165, 233));
        saveBtn.setPreferredSize(new Dimension(200, 44));
        saveBtn.addActionListener(e -> saveCurrentEditor());
        panel.add(saveBtn, gbc);

        return panel;
    }

    private JLabel createFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(new Color(148, 163, 184));
        return lbl;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color fill = getModel().isRollover() ? bg.brighter() : bg;
                g2.setColor(fill);
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
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void refreshProfileList() {
        listModel.clear();
        for (Profile p : profileManager.getProfiles()) {
            listModel.addElement(p);
        }
    }

    private void loadProfileToEditor(Profile p) {
        this.editingProfile = p;
        nameField.setText(p.getName());
        descField.setText(p.getDescription() != null ? p.getDescription() : "");
        versionCombo.setSelectedItem(p.getMcVersion());
        ramSlider.setValue(p.getRamGb());
        ramLabel.setText(p.getRamGb() + ".0 GB");
        jvmArgsField.setText(p.getJvmArgs() != null ? p.getJvmArgs() : ProfileManager.DEFAULT_JVM_ARGS);
        gameDirLabel.setText("📁 Instance Game Directory: " + (p.getGameDir() != null ? p.getGameDir() : "-"));
        modDirLabel.setText("📦 Instance Mods Directory: " + (p.getModDir() != null ? p.getModDir() : "-"));
    }

    private void saveCurrentEditor() {
        if (editingProfile == null) return;

        editingProfile.setName(nameField.getText().trim());
        editingProfile.setDescription(descField.getText().trim());
        editingProfile.setMcVersion((String) versionCombo.getSelectedItem());
        editingProfile.setRamGb(ramSlider.getValue());
        editingProfile.setJvmArgs(jvmArgsField.getText().trim());

        profileManager.saveProfiles();
        profileList.repaint();

        if (this.onActiveProfileChanged != null) {
            this.onActiveProfileChanged.run();
        }

        JOptionPane.showMessageDialog(this, "Profile '" + editingProfile.getName() + "' saved successfully!", "Profile Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Custom ListCellRenderer that renders sleek modern profile cards with active crown badges. */
    private class ProfileCellRenderer extends JPanel implements ListCellRenderer<Profile> {

        private final JLabel iconLbl = new JLabel("❄");
        private final JLabel nameLbl = new JLabel();
        private final JLabel infoLbl = new JLabel();
        private final JLabel activeBadge = new JLabel("👑 ACTIVE");

        public ProfileCellRenderer() {
            setLayout(new BorderLayout(12, 0));
            setOpaque(true);
            setBorder(new EmptyBorder(8, 12, 8, 12));

            iconLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
            iconLbl.setForeground(new Color(56, 189, 248));
            iconLbl.setPreferredSize(new Dimension(32, 32));
            iconLbl.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel centerText = new JPanel(new GridLayout(2, 1, 0, 2));
            centerText.setOpaque(false);

            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            nameLbl.setForeground(Color.WHITE);

            infoLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            infoLbl.setForeground(new Color(148, 163, 184));

            centerText.add(nameLbl);
            centerText.add(infoLbl);

            activeBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            activeBadge.setForeground(new Color(52, 211, 153));
            activeBadge.setOpaque(true);
            activeBadge.setBackground(new Color(16, 185, 129, 35));
            activeBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(16, 185, 129, 120), 1),
                    new EmptyBorder(2, 6, 2, 6)
            ));

            add(iconLbl, BorderLayout.WEST);
            add(centerText, BorderLayout.CENTER);
            add(activeBadge, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Profile> list, Profile value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                nameLbl.setText(value.getName());
                infoLbl.setText(value.getMcVersion() + " Fabric  •  " + value.getRamGb() + " GB RAM");

                Profile active = profileManager.getActiveProfile();
                boolean isActive = active != null && active.getId().equals(value.getId());
                activeBadge.setVisible(isActive);

                if (isSelected) {
                    setBackground(new Color(30, 41, 59));
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(56, 189, 248, 180), 1),
                            new EmptyBorder(7, 11, 7, 11)
                    ));
                } else {
                    setBackground(new Color(18, 22, 30));
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(31, 38, 52), 1),
                            new EmptyBorder(7, 11, 7, 11)
                    ));
                }
            }
            return this;
        }
    }
}
