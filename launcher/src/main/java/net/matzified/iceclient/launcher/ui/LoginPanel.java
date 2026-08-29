package net.matzified.iceclient.launcher.ui;

import net.matzified.iceclient.launcher.auth.Account;
import net.matzified.iceclient.launcher.auth.AccountManager;
import net.matzified.iceclient.launcher.auth.MicrosoftDeviceCode;
import net.matzified.iceclient.launcher.auth.MinecraftAuthenticator;
import net.matzified.iceclient.launcher.auth.MinecraftSession;
import net.matzified.iceclient.launcher.auth.XSTSErrorException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;

public class LoginPanel extends JPanel {

    private final AccountManager accountManager = AccountManager.getInstance();
    private final MinecraftAuthenticator authenticator = new MinecraftAuthenticator();
    private final Runnable onAccountUpdated;

    private JPanel activeAccountCard;
    private JPanel accountsListPanel;
    private JPanel authOverlayPanel;
    private JLabel authCodeLabel;
    private JLabel authStatusLabel;
    private JButton copyAndOpenBtn;

    private Thread currentAuthThread;
    private String activeUserCode = "";
    private String activeVerificationUri = "https://www.microsoft.com/link";

    public LoginPanel(Runnable onAccountUpdated) {
        this.onAccountUpdated = onAccountUpdated;

        setLayout(new BorderLayout(18, 18));
        setBackground(new Color(13, 15, 20));
        setBorder(new EmptyBorder(22, 28, 22, 28));

        // Top Header
        JPanel topHeader = new JPanel();
        topHeader.setLayout(new BoxLayout(topHeader, BoxLayout.Y_AXIS));
        topHeader.setOpaque(false);

        FrozenLabel titleLabel = new FrozenLabel("ACCOUNT MANAGER", 22);
        JLabel subLabel = new JLabel("Sign in with your Microsoft / Xbox account to play online on multiplayer servers.");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(new Color(148, 163, 184));

        topHeader.add(titleLabel);
        topHeader.add(Box.createRigidArea(new Dimension(0, 6)));
        topHeader.add(subLabel);
        topHeader.add(Box.createRigidArea(new Dimension(0, 16)));

        add(topHeader, BorderLayout.NORTH);

        // Center Content Area
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);

        // 1. Active Account Banner Card
        activeAccountCard = createActiveAccountCard();
        centerContent.add(activeAccountCard);
        centerContent.add(Box.createRigidArea(new Dimension(0, 16)));

        // 2. Microsoft OAuth2 Device Sign-In Card
        JPanel signInActionCard = createSignInActionCard();
        centerContent.add(signInActionCard);
        centerContent.add(Box.createRigidArea(new Dimension(0, 16)));

        // 3. Saved Accounts Section
        JLabel savedTitle = new JLabel("SAVED ACCOUNTS");
        savedTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        savedTitle.setForeground(new Color(148, 163, 184));
        centerContent.add(savedTitle);
        centerContent.add(Box.createRigidArea(new Dimension(0, 8)));

        accountsListPanel = new JPanel();
        accountsListPanel.setLayout(new BoxLayout(accountsListPanel, BoxLayout.Y_AXIS));
        accountsListPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(accountsListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(13, 15, 20));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        centerContent.add(scrollPane);

        add(centerContent, BorderLayout.CENTER);

        refreshAccountsUI();
    }

    public void refreshAccountsUI() {
        activeAccountCard.removeAll();
        activeAccountCard.add(buildActiveAccountInner());
        activeAccountCard.revalidate();
        activeAccountCard.repaint();

        accountsListPanel.removeAll();
        List<Account> accounts = accountManager.getAccounts();
        if (accounts.isEmpty()) {
            JLabel emptyLbl = new JLabel("No accounts added yet. Click 'Sign In with Microsoft' above!", SwingConstants.CENTER);
            emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            emptyLbl.setForeground(new Color(148, 163, 184));
            accountsListPanel.add(emptyLbl);
        } else {
            for (Account acc : accounts) {
                accountsListPanel.add(createAccountRow(acc));
                accountsListPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }
        accountsListPanel.revalidate();
        accountsListPanel.repaint();
    }

    private JPanel createActiveAccountCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(14, 165, 233, 40),
                        getWidth(), getHeight(), new Color(2, 132, 199, 15)
                );
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));

                g2.setColor(new Color(56, 189, 248, 140));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 20, 16, 20));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        return card;
    }

    private JPanel buildActiveAccountInner() {
        JPanel inner = new JPanel(new BorderLayout(16, 0));
        inner.setOpaque(false);

        Account active = accountManager.getActiveAccount();
        String username = active != null ? active.getUsername() : "Matzified";
        String uuid = active != null ? active.getUuid() : "00000000-0000-0000-0000-000000000000";

        // Avatar Badge
        JLabel avatar = new JLabel(createPlayerAvatar(username));
        avatar.setPreferredSize(new Dimension(56, 56));

        // Details
        JPanel details = new JPanel(new GridLayout(2, 1, 0, 3));
        details.setOpaque(false);

        JLabel nameLbl = new JLabel(username);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        nameLbl.setForeground(Color.WHITE);

        JLabel statusBadge = new JLabel("🟢 Active Account  •  Microsoft / Xbox Live  •  UUID: " + uuid);
        statusBadge.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBadge.setForeground(new Color(186, 230, 253));

        details.add(nameLbl);
        details.add(statusBadge);

        inner.add(avatar, BorderLayout.WEST);
        inner.add(details, BorderLayout.CENTER);
        return inner;
    }

    private JPanel createSignInActionCard() {
        JPanel card = new JPanel(new BorderLayout(16, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(24, 28, 38));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));

                g2.setColor(new Color(45, 52, 68));
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 20, 16, 20));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        // Top Row: Title + Login Button
        JPanel topRow = new JPanel(new BorderLayout(14, 0));
        topRow.setOpaque(false);

        JPanel logoGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoGroup.setOpaque(false);

        JPanel grid2x2 = new JPanel(new GridLayout(2, 2, 2, 2));
        grid2x2.setOpaque(false);
        grid2x2.add(createSquare(new Color(242, 80, 34)));
        grid2x2.add(createSquare(new Color(127, 186, 0)));
        grid2x2.add(createSquare(new Color(0, 164, 239)));
        grid2x2.add(createSquare(new Color(255, 185, 0)));

        JLabel title = new JLabel("Microsoft & Xbox Live Authentication");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);

        logoGroup.add(grid2x2);
        logoGroup.add(title);

        JButton startOAuthBtn = createPrimaryButton("Sign In with Microsoft", e -> startWebViewLogin());

        JButton deviceCodeFallback = new JButton("Use device code instead");
        deviceCodeFallback.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        deviceCodeFallback.setForeground(new Color(100, 116, 139));
        deviceCodeFallback.setFocusPainted(false);
        deviceCodeFallback.setContentAreaFilled(false);
        deviceCodeFallback.setBorderPainted(false);
        deviceCodeFallback.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deviceCodeFallback.addActionListener(e -> startMicrosoftOAuthFlow());

        JPanel btnGroup = new JPanel();
        btnGroup.setLayout(new BoxLayout(btnGroup, BoxLayout.Y_AXIS));
        btnGroup.setOpaque(false);
        startOAuthBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        deviceCodeFallback.setAlignmentX(Component.RIGHT_ALIGNMENT);
        btnGroup.add(startOAuthBtn);
        btnGroup.add(Box.createRigidArea(new Dimension(0, 3)));
        btnGroup.add(deviceCodeFallback);

        topRow.add(logoGroup, BorderLayout.WEST);
        topRow.add(btnGroup,  BorderLayout.EAST);

        // Auth Overlay Panel (Hidden by default, shows code when authenticating)
        authOverlayPanel = new JPanel(new BorderLayout(12, 0));
        authOverlayPanel.setOpaque(false);
        authOverlayPanel.setVisible(false);

        JPanel codeWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        codeWrapper.setOpaque(false);

        authCodeLabel = new JLabel("CODE: --------");
        authCodeLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        authCodeLabel.setForeground(new Color(56, 189, 248));
        authCodeLabel.setOpaque(true);
        authCodeLabel.setBackground(new Color(15, 23, 42));
        authCodeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248, 120), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));

        copyAndOpenBtn = createSecondaryButton("Copy Code & Open Link", e -> copyAndOpenBrowser());
        codeWrapper.add(authCodeLabel);
        codeWrapper.add(copyAndOpenBtn);

        authStatusLabel = new JLabel("Waiting for you to sign in at microsoft.com/link...");
        authStatusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        authStatusLabel.setForeground(new Color(56, 189, 248));

        authOverlayPanel.add(codeWrapper, BorderLayout.WEST);
        authOverlayPanel.add(authStatusLabel, BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);
        card.add(authOverlayPanel, BorderLayout.CENTER);

        return card;
    }

    /**
     * Opens the embedded JavaFX WebView Microsoft login dialog.
     * This is the primary / preferred login method.
     */
    private void startWebViewLogin() {
        Window window = SwingUtilities.getWindowAncestor(this);
        MicrosoftLoginWebDialog dlg = new MicrosoftLoginWebDialog(window, () -> {
            SwingUtilities.invokeLater(() -> {
                refreshAccountsUI();
                if (onAccountUpdated != null) onAccountUpdated.run();
            });
        });
        dlg.setVisible(true);
    }

    /**
     * Fallback: Microsoft Device Code flow (no embedded browser needed).
     * User is shown a code to enter at microsoft.com/link.
     */
    private void startMicrosoftOAuthFlow() {
        if (currentAuthThread != null && currentAuthThread.isAlive()) {
            currentAuthThread.interrupt();
        }

        authOverlayPanel.setVisible(true);
        authCodeLabel.setText("CONNECTING...");
        authStatusLabel.setText("Connecting to Microsoft identity service...");
        revalidate();
        repaint();

        currentAuthThread = new Thread(() -> {
            try {
                // Step 1: Request Microsoft Device Code
                MicrosoftDeviceCode deviceCode = authenticator.requestDeviceCode();
                activeUserCode = deviceCode.userCode();
                activeVerificationUri = deviceCode.verificationUri();

                SwingUtilities.invokeLater(() -> {
                    authCodeLabel.setText("CODE: " + deviceCode.userCode());
                    authStatusLabel.setText("Enter code at " + deviceCode.verificationUri() + " (Code copied to clipboard)");
                    copyToClipboard(deviceCode.userCode());
                });

                // Auto-open browser
                try {
                    Desktop.getDesktop().browse(new URI(deviceCode.verificationUri()));
                } catch (Exception ignored) {}

                // Step 1.5: Poll Microsoft OAuth Token
                MinecraftAuthenticator.MicrosoftTokens tokens = authenticator.pollMicrosoftToken(
                        deviceCode,
                        msg -> SwingUtilities.invokeLater(() -> authStatusLabel.setText(msg))
                );

                SwingUtilities.invokeLater(() -> authStatusLabel.setText("Authenticating Xbox Live & Minecraft Services..."));

                // Steps 2 -> 3 -> 4 -> 5: Complete Pipeline
                MinecraftSession session = authenticator.authenticateWithMicrosoftToken(tokens.accessToken(), tokens.refreshToken());

                // Save to AccountManager
                accountManager.addSession(session);

                SwingUtilities.invokeLater(() -> {
                    authOverlayPanel.setVisible(false);
                    refreshAccountsUI();
                    if (onAccountUpdated != null) {
                        onAccountUpdated.run();
                    }
                    JOptionPane.showMessageDialog(this,
                            "Successfully signed in as " + session.username() + "!",
                            "Minecraft Login Successful", JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (XSTSErrorException xstsErr) {
                xstsErr.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    authStatusLabel.setText("Xbox Error: " + xstsErr.getMessage());
                    JOptionPane.showMessageDialog(this, xstsErr.getMessage(), "Xbox Live Error", JOptionPane.ERROR_MESSAGE);
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    authStatusLabel.setText("Authentication failed: " + e.getMessage());
                });
            }
        });
        currentAuthThread.start();
    }

    private void copyAndOpenBrowser() {
        if (!activeUserCode.isEmpty()) {
            copyToClipboard(activeUserCode);
        }
        try {
            Desktop.getDesktop().browse(new URI(activeVerificationUri));
            authStatusLabel.setText("Code copied! Enter it in the browser window.");
        } catch (Exception ignored) {}
    }

    private void copyToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (Exception ignored) {}
    }

    private JPanel createAccountRow(Account acc) {
        boolean isActive = (accountManager.getActiveAccount() != null && accountManager.getActiveAccount().getId().equals(acc.getId()));

        JPanel row = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = isActive ? new Color(24, 38, 54) : new Color(20, 24, 34);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));

                g2.setColor(isActive ? new Color(56, 189, 248, 140) : new Color(42, 50, 68));
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10, 16, 10, 16));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel avatar = new JLabel(createPlayerAvatar(acc.getUsername()));
        avatar.setPreferredSize(new Dimension(38, 38));

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
        info.setOpaque(false);

        JLabel nameLbl = new JLabel(acc.getUsername() + (isActive ? " (Active)" : ""));
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLbl.setForeground(isActive ? new Color(56, 189, 248) : Color.WHITE);

        JLabel idLbl = new JLabel("UUID: " + acc.getUuid());
        idLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        idLbl.setForeground(new Color(148, 163, 184));

        info.add(nameLbl);
        info.add(idLbl);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        if (!isActive) {
            JButton selectBtn = createSecondaryButton("Set Active", e -> {
                accountManager.setActiveAccount(acc);
                refreshAccountsUI();
                if (onAccountUpdated != null) {
                    onAccountUpdated.run();
                }
            });
            actions.add(selectBtn);
        }

        JButton removeBtn = new JButton("✕") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(225, 29, 72) : new Color(38, 44, 58);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        removeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        removeBtn.setForeground(Color.WHITE);
        removeBtn.setFocusPainted(false);
        removeBtn.setContentAreaFilled(false);
        removeBtn.setBorderPainted(false);
        removeBtn.setPreferredSize(new Dimension(32, 32));
        removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        removeBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Remove account '" + acc.getUsername() + "'?", "Remove Account", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                accountManager.removeAccount(acc);
                refreshAccountsUI();
                if (onAccountUpdated != null) {
                    onAccountUpdated.run();
                }
            }
        });

        actions.add(removeBtn);

        row.add(avatar, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private ImageIcon createPlayerAvatar(String username) {
        BufferedImage img = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(30, 41, 59));
        g2.fill(new RoundRectangle2D.Float(0, 0, 48, 48, 12, 12));

        g2.setColor(new Color(56, 189, 248));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));

        String letter = username.isEmpty() ? "M" : String.valueOf(username.charAt(0)).toUpperCase();
        FontMetrics fm = g2.getFontMetrics();
        int x = (48 - fm.stringWidth(letter)) / 2;
        int y = ((48 - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(letter, x, y);
        g2.dispose();

        return new ImageIcon(img);
    }

    private JButton createPrimaryButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isRollover() ? new Color(0, 120, 215) : new Color(0, 103, 184);
                Color bottom = getModel().isRollover() ? new Color(0, 103, 184) : new Color(0, 80, 150);
                GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(200, 38));
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
                Color bg = getModel().isRollover() ? new Color(38, 48, 68) : new Color(26, 32, 46);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(new Color(56, 189, 248, 100));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(170, 32));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        return btn;
    }

    private JPanel createSquare(Color color) {
        JPanel s = new JPanel();
        s.setPreferredSize(new Dimension(8, 8));
        s.setBackground(color);
        return s;
    }
}