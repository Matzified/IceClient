package net.matzified.iceclient.launcher.ui;

import net.matzified.iceclient.launcher.auth.Account;
import net.matzified.iceclient.launcher.auth.AccountManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

public class MicrosoftLoginDialog extends JDialog {

    private final AccountManager accountManager = AccountManager.getInstance();
    private final Runnable onLoginComplete;

    private CardLayout cardLayout;
    private JPanel stepContainer;

    private JTextField emailField;
    private JPasswordField passField;
    private JLabel emailSummaryLabel;
    private String currentEmail = "";

    public MicrosoftLoginDialog(Window owner, Runnable onLoginComplete) {
        super(owner, "Sign into Ice Client", ModalityType.APPLICATION_MODAL);
        this.onLoginComplete = onLoginComplete;

        setUndecorated(true);
        setSize(860, 640);
        setLocationRelativeTo(owner);

        // Main Background Panel with Isometric Minecraft Green Geometry
        JPanel backgroundPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Base Deep Minecraft Green
                g2.setColor(new Color(34, 75, 30));
                g2.fillRect(0, 0, w, h);

                // Draw Isometric 3D Polygon Blocks (Creeper Face Geometry)
                drawIsometricMinecraftBackground(g2, w, h);

                // Subtle dark vignette gradient
                RadialGradientPaint rgp = new RadialGradientPaint(
                        w / 2.0f, h / 2.0f, Math.max(w, h) * 0.75f,
                        new float[]{0.0f, 0.7f, 1.0f},
                        new Color[]{new Color(0, 0, 0, 40), new Color(0, 0, 0, 110), new Color(0, 0, 0, 190)}
                );
                g2.setPaint(rgp);
                g2.fillRect(0, 0, w, h);

                // Window Border
                g2.setColor(new Color(20, 45, 18));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(0, 0, w - 1, h - 1);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        backgroundPanel.setLayout(new BorderLayout());

        // Top Title Bar (Modrinth App Style)
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        titleBar.setPreferredSize(new Dimension(0, 36));
        titleBar.setBorder(new EmptyBorder(6, 14, 0, 14));

        // Window Title & Icon
        JPanel titleLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleLeft.setOpaque(false);

        JLabel logoDot = new JLabel("●");
        logoDot.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoDot.setForeground(new Color(56, 189, 248));

        JLabel titleText = new JLabel("Sign into Ice Client");
        titleText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleText.setForeground(new Color(220, 230, 242));

        titleLeft.add(logoDot);
        titleLeft.add(titleText);

        // Window Controls (—  ▢  ✕)
        JPanel windowControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        windowControls.setOpaque(false);

        JLabel minimizeBtn = createControlGlyph("—");
        minimizeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Minimize logic
            }
        });

        JLabel maxBtn = createControlGlyph("▢");

        JLabel closeBtn = createControlGlyph("✕");
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(new Color(244, 63, 94));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(new Color(200, 210, 225));
            }
        });

        windowControls.add(minimizeBtn);
        windowControls.add(maxBtn);
        windowControls.add(closeBtn);

        titleBar.add(titleLeft, BorderLayout.WEST);
        titleBar.add(windowControls, BorderLayout.EAST);
        backgroundPanel.add(titleBar, BorderLayout.NORTH);

        // Center Floating Sign-In Card (Exact Modrinth/Microsoft App Card)
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel authCard = createMicrosoftAuthCard();
        centerWrapper.add(authCard);
        backgroundPanel.add(centerWrapper, BorderLayout.CENTER);

        // Bottom Minecraft Logo & Links (Exact Match)
        JPanel bottomFooter = new JPanel();
        bottomFooter.setLayout(new BoxLayout(bottomFooter, BoxLayout.Y_AXIS));
        bottomFooter.setOpaque(false);
        bottomFooter.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel mcLogo = new JLabel("MINECRAFT", SwingConstants.CENTER);
        mcLogo.setFont(new Font("Impact", Font.BOLD, 22));
        mcLogo.setForeground(new Color(210, 215, 220));
        mcLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel linksRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        linksRow.setOpaque(false);
        linksRow.add(createFooterLink("Help and feedback"));
        linksRow.add(createFooterLink("Terms of use"));
        linksRow.add(createFooterLink("Privacy and cookies"));
        linksRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        bottomFooter.add(mcLogo);
        bottomFooter.add(Box.createRigidArea(new Dimension(0, 6)));
        bottomFooter.add(linksRow);

        backgroundPanel.add(bottomFooter, BorderLayout.SOUTH);

        add(backgroundPanel);
    }

    private JPanel createMicrosoftAuthCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dark Card Background (#292929)
                g2.setColor(new Color(41, 41, 41));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(380, 420));
        card.setBorder(new EmptyBorder(20, 28, 24, 28));

        // Card Top Header (✕ on left, Microsoft Logo in center)
        JPanel cardTop = new JPanel(new BorderLayout());
        cardTop.setOpaque(false);

        JLabel cardClose = new JLabel("✕");
        cardClose.setFont(new Font("Segoe UI", Font.BOLD, 15));
        cardClose.setForeground(new Color(170, 170, 170));
        cardClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cardClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
        });

        JPanel msHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        msHeader.setOpaque(false);

        JPanel grid2x2 = new JPanel(new GridLayout(2, 2, 2, 2));
        grid2x2.setOpaque(false);
        grid2x2.add(createSquare(new Color(242, 80, 34)));
        grid2x2.add(createSquare(new Color(127, 186, 0)));
        grid2x2.add(createSquare(new Color(0, 164, 239)));
        grid2x2.add(createSquare(new Color(255, 185, 0)));

        JLabel msLabel = new JLabel("Microsoft");
        msLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        msLabel.setForeground(Color.WHITE);

        msHeader.add(grid2x2);
        msHeader.add(msLabel);

        cardTop.add(cardClose, BorderLayout.WEST);
        cardTop.add(msHeader, BorderLayout.CENTER);
        card.add(cardTop, BorderLayout.NORTH);

        // Step Container (CardLayout)
        cardLayout = new CardLayout();
        stepContainer = new JPanel(cardLayout);
        stepContainer.setOpaque(false);

        stepContainer.add(createStep1EmailPanel(), "STEP_1_EMAIL");
        stepContainer.add(createStep2PasswordPanel(), "STEP_2_PASS");

        card.add(stepContainer, BorderLayout.CENTER);
        return card;
    }

    private JPanel createStep1EmailPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        p.add(Box.createRigidArea(new Dimension(0, 16)));

        JLabel title = new JLabel("Sign in", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(title);

        p.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel sub = new JLabel("to continue to Minecraft.", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(new Color(190, 190, 190));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(sub);

        p.add(Box.createRigidArea(new Dimension(0, 24)));

        // Input Box (Email or Phone Number)
        emailField = new JTextField("Matzified");
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emailField.setBackground(new Color(31, 31, 31));
        emailField.setForeground(Color.WHITE);
        emailField.setCaretColor(Color.WHITE);
        emailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(90, 90, 90), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        emailField.setMaximumSize(new Dimension(324, 40));
        emailField.setAlignmentX(Component.CENTER_ALIGNMENT);
        emailField.addActionListener(e -> goToStep2());

        p.add(emailField);

        p.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel forgotUser = createCardLink("Forgot your username?");
        forgotUser.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(forgotUser);

        p.add(Box.createRigidArea(new Dimension(0, 20)));

        // Green "Next" Button (#3B8526)
        JButton nextBtn = createGreenButton("Next", e -> goToStep2());
        nextBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(nextBtn);

        p.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel createAcc = new JLabel("New to Microsoft? Create an account", SwingConstants.CENTER);
        createAcc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        createAcc.setForeground(new Color(210, 210, 210));
        createAcc.setAlignmentX(Component.CENTER_ALIGNMENT);
        createAcc.setCursor(new Cursor(Cursor.HAND_CURSOR));
        p.add(createAcc);

        return p;
    }

    private JPanel createStep2PasswordPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        p.add(Box.createRigidArea(new Dimension(0, 12)));

        // Back arrow + User email pill
        JPanel userHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        userHeader.setOpaque(false);

        JLabel backArrow = new JLabel("←");
        backArrow.setFont(new Font("Segoe UI", Font.BOLD, 15));
        backArrow.setForeground(new Color(200, 200, 200));
        backArrow.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backArrow.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cardLayout.show(stepContainer, "STEP_1_EMAIL");
            }
        });

        emailSummaryLabel = new JLabel("Matzified");
        emailSummaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        emailSummaryLabel.setForeground(new Color(220, 220, 220));

        userHeader.add(backArrow);
        userHeader.add(emailSummaryLabel);
        userHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(userHeader);

        p.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel passTitle = new JLabel("Enter password", SwingConstants.CENTER);
        passTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        passTitle.setForeground(Color.WHITE);
        passTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(passTitle);

        p.add(Box.createRigidArea(new Dimension(0, 20)));

        passField = new JPasswordField("••••••••••••");
        passField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passField.setBackground(new Color(31, 31, 31));
        passField.setForeground(Color.WHITE);
        passField.setCaretColor(Color.WHITE);
        passField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(90, 90, 90), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        passField.setMaximumSize(new Dimension(324, 40));
        passField.setAlignmentX(Component.CENTER_ALIGNMENT);
        passField.addActionListener(e -> finishAuthentication());
        p.add(passField);

        p.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel forgotPass = createCardLink("Forgot password?");
        forgotPass.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(forgotPass);

        p.add(Box.createRigidArea(new Dimension(0, 20)));

        // Green "Sign in" Button
        JButton signInBtn = createGreenButton("Sign in", e -> finishAuthentication());
        signInBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(signInBtn);

        p.add(Box.createRigidArea(new Dimension(0, 16)));

        JLabel otherWays = createCardLink("Other ways to sign in");
        otherWays.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(otherWays);

        return p;
    }

    private void goToStep2() {
        currentEmail = emailField.getText().trim();
        if (currentEmail.isEmpty()) currentEmail = "Matzified";
        emailSummaryLabel.setText(currentEmail);
        cardLayout.show(stepContainer, "STEP_2_PASS");
        passField.requestFocusInWindow();
    }

    private void finishAuthentication() {
        String username = currentEmail;
        if (username.contains("@")) {
            username = username.substring(0, username.indexOf('@'));
        }
        if (username.isEmpty()) username = "Matzified";
        username = Character.toUpperCase(username.charAt(0)) + username.substring(1);

        String uuid = "ms_uuid_" + Math.abs(username.hashCode());
        Account acc = new Account("ms_" + System.currentTimeMillis(), username, uuid, "token_ms_official_valid", currentEmail, true);
        accountManager.addOrUpdateAccount(acc);

        dispose();
        if (onLoginComplete != null) {
            onLoginComplete.run();
        }
    }

    private JButton createGreenButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = getModel().isRollover() ? new Color(70, 148, 52) : new Color(59, 133, 38);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(324, 40));
        btn.setMaximumSize(new Dimension(324, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        return btn;
    }

    private JLabel createControlGlyph(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setForeground(new Color(200, 210, 225));
        l.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return l;
    }

    private JLabel createFooterLink(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(200, 220, 200));
        l.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return l;
    }

    private JLabel createCardLink(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(240, 240, 240));
        l.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return l;
    }

    private JPanel createSquare(Color color) {
        JPanel s = new JPanel();
        s.setPreferredSize(new Dimension(8, 8));
        s.setBackground(color);
        return s;
    }

    private void drawIsometricMinecraftBackground(Graphics2D g2, int w, int h) {
        // Draw Isometric Polyhedral Creeper Geometric Cubes
        int cubeSize = 130;
        Color[] greens = {
                new Color(45, 90, 39),   // Top
                new Color(30, 63, 26),   // Left
                new Color(23, 49, 20),   // Right
                new Color(58, 116, 50),  // Light Top
                new Color(38, 77, 33),   // Light Left
                new Color(28, 58, 25)    // Light Right
        };

        for (int y = -cubeSize; y < h + cubeSize * 2; y += cubeSize * 0.86) {
            for (int x = -cubeSize; x < w + cubeSize * 2; x += cubeSize * 1.5) {
                int ox = ((int)(y / (cubeSize * 0.86)) % 2 == 0) ? 0 : (int)(cubeSize * 0.75);
                drawIsoCube(g2, x + ox, y, cubeSize / 2, greens);
            }
        }
    }

    private void drawIsoCube(Graphics2D g2, int cx, int cy, int s, Color[] colors) {
        // Top Face
        GeneralPath top = new GeneralPath();
        top.moveTo(cx, cy - s);
        top.lineTo(cx + s, cy - s / 2.0);
        top.lineTo(cx, cy);
        top.lineTo(cx - s, cy - s / 2.0);
        top.closePath();
        g2.setColor(colors[0]);
        g2.fill(top);

        // Left Face
        GeneralPath left = new GeneralPath();
        left.moveTo(cx - s, cy - s / 2.0);
        left.lineTo(cx, cy);
        left.lineTo(cx, cy + s);
        left.lineTo(cx - s, cy + s / 2.0);
        left.closePath();
        g2.setColor(colors[1]);
        g2.fill(left);

        // Right Face
        GeneralPath right = new GeneralPath();
        right.moveTo(cx, cy);
        right.lineTo(cx + s, cy - s / 2.0);
        right.lineTo(cx + s, cy + s / 2.0);
        right.lineTo(cx, cy + s);
        right.closePath();
        g2.setColor(colors[2]);
        g2.fill(right);
    }
}
