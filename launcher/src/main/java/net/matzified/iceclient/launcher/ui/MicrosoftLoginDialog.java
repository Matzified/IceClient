package net.matzified.iceclient.launcher.ui;

import net.matzified.iceclient.launcher.auth.Account;
import net.matzified.iceclient.launcher.auth.AccountManager;
import net.matzified.iceclient.launcher.auth.MicrosoftAuthHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.util.UUID;

public class MicrosoftLoginDialog extends JDialog {

    private final AccountManager accountManager = AccountManager.getInstance();
    private final Runnable onLoginComplete;

    private JLabel codeLabel;
    private JLabel statusLabel;
    private JButton copyOpenBtn;
    private JProgressBar progressBar;

    private JTextField offlineNameField;

    public MicrosoftLoginDialog(Frame owner, Runnable onLoginComplete) {
        super(owner, "Sign In to Minecraft", true);
        this.onLoginComplete = onLoginComplete;

        setSize(540, 480);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(new Color(11, 14, 20));
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 28, 14, 28));

        JLabel title = new JLabel("SIGN IN TO MINECRAFT");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Authenticate with your official Microsoft Account");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(148, 163, 184));

        JPanel headerText = new JPanel(new GridLayout(2, 1, 0, 3));
        headerText.setOpaque(false);
        headerText.add(title);
        headerText.add(subtitle);
        header.add(headerText, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        // Center Content (Tabs for Microsoft Device Auth & Offline Mode)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Microsoft Live Device Auth
        JPanel msPanel = createMicrosoftPanel();
        tabbedPane.addTab("🟢 Microsoft Live Account", msPanel);

        // Tab 2: Offline Nickname Mode
        JPanel offlinePanel = createOfflinePanel();
        tabbedPane.addTab("⚡ Offline Username", offlinePanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 14));
        footer.setOpaque(false);
        JButton cancelBtn = new JButton("Close");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelBtn.setForeground(new Color(148, 163, 184));
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> dispose());
        footer.add(cancelBtn);

        add(footer, BorderLayout.SOUTH);

        // Automatically start Microsoft Device Auth flow
        startMicrosoftAuth();
    }

    private JPanel createMicrosoftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 28, 20, 28));

        JLabel step1 = new JLabel("1. Enter this code on Microsoft Sign-In:");
        step1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        step1.setForeground(new Color(203, 213, 225));
        panel.add(step1);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Code Display Card
        JPanel codeCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 24, 38));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(56, 189, 248, 100));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        codeCard.setOpaque(false);
        codeCard.setPreferredSize(new Dimension(460, 60));
        codeCard.setMaximumSize(new Dimension(460, 60));
        codeCard.setBorder(new EmptyBorder(10, 20, 10, 20));

        codeLabel = new JLabel("GENERATING CODE...", SwingConstants.CENTER);
        codeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        codeLabel.setForeground(new Color(56, 189, 248));
        codeCard.add(codeLabel, BorderLayout.CENTER);
        panel.add(codeCard);

        panel.add(Box.createRigidArea(new Dimension(0, 16)));

        // Copy & Open Browser Button
        copyOpenBtn = new JButton("🌐 Copy Code & Open Microsoft Login") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(2, 132, 199) : new Color(3, 105, 161);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        copyOpenBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        copyOpenBtn.setForeground(Color.WHITE);
        copyOpenBtn.setFocusPainted(false);
        copyOpenBtn.setBorderPainted(false);
        copyOpenBtn.setContentAreaFilled(false);
        copyOpenBtn.setPreferredSize(new Dimension(460, 40));
        copyOpenBtn.setMaximumSize(new Dimension(460, 40));
        copyOpenBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.add(copyOpenBtn);

        panel.add(Box.createRigidArea(new Dimension(0, 16)));

        // Status & Progress
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(460, 6));
        progressBar.setMaximumSize(new Dimension(460, 6));
        panel.add(progressBar);

        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        statusLabel = new JLabel("Waiting for authorization in browser...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(148, 163, 184));
        panel.add(statusLabel);

        return panel;
    }

    private JPanel createOfflinePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(24, 28, 20, 28));

        JLabel info = new JLabel("Play in offline mode with a custom username:");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        info.setForeground(new Color(203, 213, 225));
        panel.add(info);

        panel.add(Box.createRigidArea(new Dimension(0, 12)));

        offlineNameField = new JTextField("IcePlayer");
        offlineNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        offlineNameField.setForeground(Color.WHITE);
        offlineNameField.setCaretColor(new Color(56, 189, 248));
        offlineNameField.setBackground(new Color(18, 24, 36));
        offlineNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248, 80), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        offlineNameField.setPreferredSize(new Dimension(460, 40));
        offlineNameField.setMaximumSize(new Dimension(460, 40));
        panel.add(offlineNameField);

        panel.add(Box.createRigidArea(new Dimension(0, 16)));

        JButton useOfflineBtn = new JButton("⚡ Use Offline Profile") {
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
        useOfflineBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        useOfflineBtn.setForeground(Color.WHITE);
        useOfflineBtn.setFocusPainted(false);
        useOfflineBtn.setBorderPainted(false);
        useOfflineBtn.setContentAreaFilled(false);
        useOfflineBtn.setPreferredSize(new Dimension(460, 40));
        useOfflineBtn.setMaximumSize(new Dimension(460, 40));
        useOfflineBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        useOfflineBtn.addActionListener(e -> {
            String name = offlineNameField.getText().trim();
            if (name.isEmpty()) name = "IcePlayer";
            String uuid = UUID.randomUUID().toString();
            Account acc = new Account("offline_" + uuid.substring(0, 8), name, uuid, "offline", "", true);
            accountManager.addOrUpdateAccount(acc);
            accountManager.setActiveAccount(acc);
            dispose();
            if (onLoginComplete != null) onLoginComplete.run();
        });
        panel.add(useOfflineBtn);

        return panel;
    }

    private void startMicrosoftAuth() {
        MicrosoftAuthHandler.startDeviceAuth(new MicrosoftAuthHandler.DeviceCodeCallback() {
            @Override
            public void onCodeReceived(String userCode, String verificationUri, String directLink) {
                SwingUtilities.invokeLater(() -> {
                    codeLabel.setText(userCode);
                    copyOpenBtn.addActionListener(e -> {
                        try {
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(userCode), null);
                            Desktop.getDesktop().browse(URI.create(directLink));
                            statusLabel.setText("Code copied! Complete sign-in in your browser.");
                        } catch (Exception ex) {
                            statusLabel.setText("Failed to open browser: " + ex.getMessage());
                        }
                    });
                });
            }

            @Override
            public void onSuccess(String username, String uuid, String accessToken) {
                SwingUtilities.invokeLater(() -> {
                    Account acc = new Account("ms_" + uuid.replace("-", "").substring(0, 12), username, uuid, accessToken, "", true);
                    accountManager.addOrUpdateAccount(acc);
                    accountManager.setActiveAccount(acc);
                    dispose();
                    if (onLoginComplete != null) onLoginComplete.run();
                });
            }

            @Override
            public void onFailure(String error) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("❌ " + error);
                    progressBar.setVisible(false);
                });
            }
        });
    }
}
