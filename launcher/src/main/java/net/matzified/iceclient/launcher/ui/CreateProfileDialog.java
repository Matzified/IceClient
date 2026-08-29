package net.matzified.iceclient.launcher.ui;

import net.matzified.iceclient.launcher.model.Profile;
import net.matzified.iceclient.launcher.profile.ProfileManager;
import net.matzified.iceclient.launcher.utils.ModpackImporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;
import java.util.UUID;

public class CreateProfileDialog extends JDialog {

    private final ProfileManager profileManager = ProfileManager.getInstance();
    private final Runnable onProfileCreated;
    private final ConsumerTabSwitch tabSwitchCallback;

    public interface ConsumerTabSwitch {
        void switchToTab(int tabIndex);
    }

    public CreateProfileDialog(Frame owner, Runnable onProfileCreated, ConsumerTabSwitch tabSwitchCallback) {
        super(owner, "Create New Profile", true);
        this.onProfileCreated = onProfileCreated;
        this.tabSwitchCallback = tabSwitchCallback;

        setSize(780, 520);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(new Color(18, 20, 26));

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);

        JLabel titleLbl = new JLabel("Create New Profile");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLbl.setForeground(Color.WHITE);

        JLabel subLbl = new JLabel("Select whether you want to create a new profile or import an existing profile into Ice Client.");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLbl.setForeground(new Color(156, 163, 175));

        header.add(titleLbl);
        header.add(subLbl);
        mainPanel.add(header, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(2, 2, 16, 16));
        gridPanel.setOpaque(false);

        gridPanel.add(createTile("Wizard", "Create your own profile from scratch in a few easy clicks!", e -> openWizardDialog()));
        gridPanel.add(createTile("Install Modpack", "Browse community modpacks and install one in just a few clicks!", e -> {
            dispose();
            if (tabSwitchCallback != null) tabSwitchCallback.switchToTab(1);
        }));
        gridPanel.add(createTile("Import from Filesystem", "Select or Drag & Drop a .mrpack or .zip file into Ice Client!", e -> importFromFilesystem()));
        gridPanel.add(createTile("From other Launchers", "Import your existing .minecraft or Fabric launcher profiles!", e -> importFromMinecraft()));

        mainPanel.add(gridPanel, BorderLayout.CENTER);

        setupDragAndDrop(gridPanel);

        add(mainPanel);
    }

    private JPanel createTile(String title, String description, java.awt.event.ActionListener onClick) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(26, 29, 36));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(42, 47, 58), 1),
                new EmptyBorder(22, 20, 22, 20)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLbl = new JLabel("<html><center>" + description + "</center></html>", SwingConstants.CENTER);
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setForeground(new Color(156, 163, 175));
        descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(titleLbl);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(descLbl);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                onClick.actionPerformed(null);
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(36, 41, 52));
                card.setBorder(BorderFactory.createLineBorder(new Color(56, 189, 248), 1));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(26, 29, 36));
                card.setBorder(BorderFactory.createLineBorder(new Color(42, 47, 58), 1));
            }
        });

        return card;
    }

    private void openWizardDialog() {
        JDialog wizard = new JDialog(this, "Profile Wizard", true);
        wizard.setSize(480, 480);
        wizard.setLocationRelativeTo(this);
        wizard.getContentPane().setBackground(new Color(18, 20, 26));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(20, 25, 20, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel nameLbl = new JLabel("PROFILE NAME:");
        nameLbl.setForeground(new Color(156, 163, 175));
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(nameLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        JTextField nameTxt = new JTextField("My Ice Profile");
        nameTxt.setBackground(new Color(26, 29, 36));
        nameTxt.setForeground(Color.WHITE);
        nameTxt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        form.add(nameTxt, gbc);

        // Version
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel verLbl = new JLabel("VERSION:");
        verLbl.setForeground(new Color(156, 163, 175));
        verLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(verLbl, gbc);

        gbc.gridx = 1;
        JComboBox<String> verCombo = new JComboBox<>(ProfileManager.SUPPORTED_VERSIONS);
        verCombo.setBackground(new Color(26, 29, 36));
        verCombo.setForeground(Color.WHITE);
        verCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        form.add(verCombo, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel descTitleLbl = new JLabel("DESCRIPTION:");
        descTitleLbl.setForeground(new Color(156, 163, 175));
        descTitleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(descTitleLbl, gbc);

        gbc.gridx = 1;
        JTextField descTxt = new JTextField("Custom Fabric 1.21 Profile");
        descTxt.setBackground(new Color(26, 29, 36));
        descTxt.setForeground(Color.WHITE);
        descTxt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        form.add(descTxt, gbc);

        // Banner Color
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel bannerTitleLbl = new JLabel("BANNER THEME:");
        bannerTitleLbl.setForeground(new Color(156, 163, 175));
        bannerTitleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(bannerTitleLbl, gbc);

        gbc.gridx = 1;
        JComboBox<String> colorCombo = new JComboBox<>(new String[]{"Icy Blue (#0284C7)", "Cyan Glow (#38BDF8)", "Emerald (#10B981)", "Purple (#8B5CF6)", "Crimson (#EF4444)"});
        colorCombo.setBackground(new Color(26, 29, 36));
        colorCombo.setForeground(Color.WHITE);
        form.add(colorCombo, gbc);

        // Create Button
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JButton createBtn = new JButton("CREATE PROFILE");
        createBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        createBtn.setBackground(new Color(2, 132, 199));
        createBtn.setForeground(Color.WHITE);
        createBtn.setPreferredSize(new Dimension(200, 42));

        createBtn.addActionListener(e -> {
            String name = nameTxt.getText().trim();
            if (name.isEmpty()) name = "New Profile";

            String id = "profile_" + UUID.randomUUID().toString().substring(0, 8);
            File profilesDir = new File(profileManager.getRootDir(), "profiles");
            String folderName = name.replaceAll("[^a-zA-Z0-9.-]", "_") + "-" + UUID.randomUUID().toString().substring(0, 4);
            File gameDir = new File(profilesDir, folderName);

            String selectedColor = "#0284C7";
            if (colorCombo.getSelectedIndex() == 1) selectedColor = "#38BDF8";
            else if (colorCombo.getSelectedIndex() == 2) selectedColor = "#10B981";
            else if (colorCombo.getSelectedIndex() == 3) selectedColor = "#8B5CF6";
            else if (colorCombo.getSelectedIndex() == 4) selectedColor = "#EF4444";

            Profile p = new Profile(
                    id,
                    name,
                    descTxt.getText().trim(),
                    (String) verCombo.getSelectedItem(),
                    "0.16.0",
                    4,
                    ProfileManager.DEFAULT_JVM_ARGS,
                    gameDir.getAbsolutePath(),
                    "ICE",
                    selectedColor
            );

            profileManager.addProfile(p);
            wizard.dispose();
            dispose();
            if (onProfileCreated != null) onProfileCreated.run();
        });

        form.add(createBtn, gbc);
        wizard.add(form);
        wizard.setVisible(true);
    }

    private void importFromFilesystem() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        FileDialog fd = new FileDialog(owner, "Select .mrpack or .zip Modpack File", FileDialog.LOAD);
        fd.setFile("*.mrpack;*.zip");
        fd.setVisible(true);

        if (fd.getFile() != null) {
            File packFile = new File(fd.getDirectory(), fd.getFile());
            processModpackFile(packFile);
        }
    }

    private void processModpackFile(File packFile) {
        String profileName = packFile.getName().replaceAll("\\.(mrpack|zip)$", "");
        Profile p = ModpackImporter.importModpackFile(packFile, profileName);
        if (p != null) {
            profileManager.addProfile(p);
            JOptionPane.showMessageDialog(this, "Modpack '" + p.getName() + "' imported successfully into dedicated isolated folder!", "Modpack Imported", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            if (onProfileCreated != null) onProfileCreated.run();
            if (tabSwitchCallback != null) tabSwitchCallback.switchToTab(0);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to import modpack. Please check file format.", "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importFromMinecraft() {
        File dotMinecraft = new File(System.getenv("APPDATA"), ".minecraft");
        if (dotMinecraft.exists()) {
            String id = "profile_imported_" + UUID.randomUUID().toString().substring(0, 6);
            Profile p = new Profile(
                    id,
                    "Imported .minecraft",
                    "Imported profile pointing to vanilla .minecraft directory",
                    "1.21.1",
                    "0.16.0",
                    4,
                    ProfileManager.DEFAULT_JVM_ARGS,
                    dotMinecraft.getAbsolutePath(),
                    "VANILLA",
                    "#8B5CF6"
            );
            profileManager.addProfile(p);
            JOptionPane.showMessageDialog(this, "Imported .minecraft profile!", "Import Complete", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            if (onProfileCreated != null) onProfileCreated.run();
        } else {
            JOptionPane.showMessageDialog(this, "Default .minecraft directory not found on system.", "Not Found", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void setupDragAndDrop(JPanel panel) {
        new DropTarget(panel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (droppedFiles != null && !droppedFiles.isEmpty()) {
                        File packFile = droppedFiles.get(0);
                        if (packFile.getName().endsWith(".mrpack") || packFile.getName().endsWith(".zip")) {
                            processModpackFile(packFile);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
