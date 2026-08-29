package net.matzified.iceclient.launcher;

import net.matzified.iceclient.launcher.auth.Account;
import net.matzified.iceclient.launcher.auth.AccountManager;
import net.matzified.iceclient.launcher.launch.MinecraftLaunchEngine;
import net.matzified.iceclient.launcher.model.Profile;
import net.matzified.iceclient.launcher.profile.ProfileManager;
import net.matzified.iceclient.launcher.ui.*;
import net.matzified.iceclient.launcher.utils.AutoUpdater;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;
import java.net.URI;

public class IceClientLauncher extends JFrame {

    private final ProfileManager profileManager = ProfileManager.getInstance();
    private final AccountManager accountManager = AccountManager.getInstance();

    private Point dragOffset;
    private JPanel mainContentCardPanel;
    private CardLayout cardLayout;

    private JButton launchGameBtn;
    private JButton versionSelectBtn;

    private ProfileManagerPanel profileManagerPanel;
    private ModsManagerPanel modsManagerPanel;
    private ModrinthPanel modrinthPanel;
    private LoginPanel loginPanel;

    private LaunchOverlayPanel launchOverlay;
    private AccountSwitcherPopup accountSwitcherPopup;

    private volatile Process runningProcess = null;
    private int selectedNavIndex = 0;
    private final JButton[] dockTabs = new JButton[5];
    private ImageIcon logoIcon;
    private ImageIcon scaledLogo28;
    private ImageIcon scaledLogo80;

    public IceClientLauncher() {
        setUndecorated(true);
        setTitle("Ice Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1140, 720);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);

        loadLogo();
        if (logoIcon != null) {
            setIconImage(logoIcon.getImage());
        }

        AutoUpdater.checkForUpdatesAsync(this);

        // Lunar-inspired clean obsidian theme
        JPanel rootPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(14, 17, 23));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(new Color(48, 54, 61));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        rootPanel.setOpaque(false);
        setContentPane(rootPanel);

        // 1. Top Navbar
        JPanel topNavbar = createTopNavbar();
        rootPanel.add(topNavbar, BorderLayout.NORTH);

        // 2. Central Content Area
        cardLayout = new CardLayout();
        mainContentCardPanel = new JPanel(cardLayout);
        mainContentCardPanel.setOpaque(false);

        JPanel launchpadPage = createLunarHomePage();
        profileManagerPanel = new ProfileManagerPanel(this::onProfileStateChanged, this::openCreateProfileDialog);
        modrinthPanel = new ModrinthPanel();
        modsManagerPanel = new ModsManagerPanel();
        loginPanel = new LoginPanel(this::onAccountStateChanged);

        mainContentCardPanel.add(launchpadPage, "HOME");
        mainContentCardPanel.add(profileManagerPanel, "PROFILES");
        mainContentCardPanel.add(modrinthPanel, "MODSTORE");
        mainContentCardPanel.add(modsManagerPanel, "MODS");
        mainContentCardPanel.add(loginPanel, "ACCOUNTS");

        rootPanel.add(mainContentCardPanel, BorderLayout.CENTER);

        // 3. Bottom Launch Bar
        JPanel bottomLaunchBar = createBottomLaunchBar();
        rootPanel.add(bottomLaunchBar, BorderLayout.SOUTH);

        // Launch Overlay Layer
        launchOverlay = new LaunchOverlayPanel();
        accountSwitcherPopup = new AccountSwitcherPopup(this, this::onAccountStateChanged);
        accountManager.addListener(this::onAccountStateChanged);

        try {
            setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
        } catch (Exception ignored) {}

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                try {
                    setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                } catch (Exception ignored) {}
                if (launchOverlay.isVisible()) {
                    launchOverlay.setBounds(0, 0, getLayeredPane().getWidth(), getLayeredPane().getHeight());
                }
            }
        });

        // Prompt first-time login if no saved account exists
        SwingUtilities.invokeLater(() -> {
            Account active = accountManager.getActiveAccount();
            if (active == null || "IcePlayer".equalsIgnoreCase(active.getUsername()) || active.getUsername().contains("default")) {
                new MicrosoftLoginDialog(this, this::onAccountStateChanged).setVisible(true);
            }
        });
    }

    private void loadLogo() {
        try (InputStream is = getClass().getResourceAsStream("/assets/logo.png")) {
            if (is != null) {
                Image img = ImageIO.read(is);
                if (img != null) {
                    logoIcon = new ImageIcon(img);
                    scaledLogo28 = new ImageIcon(img.getScaledInstance(28, 28, Image.SCALE_SMOOTH));
                    scaledLogo80 = new ImageIcon(img.getScaledInstance(80, 80, Image.SCALE_SMOOTH));
                }
            }
        } catch (Exception ignored) {}
    }

    private JPanel createTopNavbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(22, 27, 34));
        bar.setPreferredSize(new Dimension(getWidth(), 52));
        bar.setBorder(new EmptyBorder(0, 18, 0, 16));

        // Drag Listener
        bar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { dragOffset = e.getPoint(); }
        });
        bar.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point curr = getLocation();
                setLocation(curr.x + e.getX() - dragOffset.x, curr.y + e.getY() - dragOffset.y);
            }
        });

        // Left Branding
        JPanel leftBrand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        leftBrand.setOpaque(false);

        if (scaledLogo28 != null) {
            JLabel logoImg = new JLabel(scaledLogo28);
            leftBrand.add(logoImg);
        }

        JLabel logoText = new JLabel("ICE CLIENT");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 15));
        logoText.setForeground(Color.WHITE);
        leftBrand.add(logoText);

        bar.add(leftBrand, BorderLayout.WEST);

        // Center Navigation Tabs
        JPanel navCenter = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 10));
        navCenter.setOpaque(false);

        String[] tabNames = {"HOME", "PROFILES", "MOD STORE", "MODS", "ACCOUNTS"};
        String[] cardNames = {"HOME", "PROFILES", "MODSTORE", "MODS", "ACCOUNTS"};

        for (int i = 0; i < tabNames.length; i++) {
            final int idx = i;
            final String cName = cardNames[i];
            JButton tab = new JButton(tabNames[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean isSel = (selectedNavIndex == idx);
                    if (isSel) {
                        g2.setColor(new Color(56, 189, 248, 25));
                        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                        g2.setColor(new Color(56, 189, 248));
                        g2.fillRect(6, getHeight() - 2, getWidth() - 12, 2);
                    } else if (getModel().isRollover()) {
                        g2.setColor(new Color(255, 255, 255, 10));
                        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            tab.setFont(new Font("Segoe UI", Font.BOLD, 12));
            tab.setForeground(selectedNavIndex == idx ? new Color(56, 189, 248) : new Color(148, 163, 184));
            tab.setFocusPainted(false);
            tab.setBorderPainted(false);
            tab.setContentAreaFilled(false);
            tab.setPreferredSize(new Dimension(100, 32));
            tab.setCursor(new Cursor(Cursor.HAND_CURSOR));

            tab.addActionListener(e -> {
                selectedNavIndex = idx;
                for (int j = 0; j < dockTabs.length; j++) {
                    if (dockTabs[j] != null) {
                        dockTabs[j].setForeground(j == idx ? new Color(56, 189, 248) : new Color(148, 163, 184));
                        dockTabs[j].repaint();
                    }
                }
                cardLayout.show(mainContentCardPanel, cName);
            });

            dockTabs[i] = tab;
            navCenter.add(tab);
        }

        bar.add(navCenter, BorderLayout.CENTER);

        // Right Actions (Account Chip + Window Controls)
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rightActions.setOpaque(false);

        Account activeAcc = accountManager.getActiveAccount();
        String playerName = activeAcc != null ? activeAcc.getUsername() : "IcePlayer";

        JButton accountBtn = new JButton("● " + playerName) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(33, 38, 45) : new Color(22, 27, 34);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(new Color(48, 54, 61));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        accountBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        accountBtn.setForeground(new Color(56, 189, 248));
        accountBtn.setFocusPainted(false);
        accountBtn.setBorderPainted(false);
        accountBtn.setContentAreaFilled(false);
        accountBtn.setPreferredSize(new Dimension(130, 30));
        accountBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        accountBtn.addActionListener(e -> new MicrosoftLoginDialog(this, this::onAccountStateChanged).setVisible(true));
        rightActions.add(accountBtn);

        JButton minBtn = new JButton("—");
        minBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        minBtn.setForeground(new Color(148, 163, 184));
        minBtn.setFocusPainted(false);
        minBtn.setBorderPainted(false);
        minBtn.setContentAreaFilled(false);
        minBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        minBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        rightActions.add(minBtn);

        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        closeBtn.setForeground(new Color(239, 68, 68));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> System.exit(0));
        rightActions.add(closeBtn);

        bar.add(rightActions, BorderLayout.EAST);
        return bar;
    }

    private JPanel createLunarHomePage() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel showcaseCard = new JPanel(new BorderLayout(24, 18)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(22, 27, 34));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(new Color(48, 54, 61));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        showcaseCard.setOpaque(false);
        showcaseCard.setPreferredSize(new Dimension(840, 360));
        showcaseCard.setBorder(new EmptyBorder(32, 36, 32, 36));

        // Center Content Box
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);

        if (scaledLogo80 != null) {
            JLabel logo = new JLabel(scaledLogo80);
            logo.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerContent.add(logo);
            centerContent.add(Box.createRigidArea(new Dimension(0, 14)));
        }

        JLabel title = new JLabel("ICE CLIENT");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerContent.add(title);

        centerContent.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel sub = new JLabel("1000 FPS Competitive Ecosystem • Fabric 1.21.1");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(new Color(56, 189, 248));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerContent.add(sub);

        centerContent.add(Box.createRigidArea(new Dimension(0, 20)));

        // Quick Feature Pills Row
        JPanel pillsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        pillsRow.setOpaque(false);
        pillsRow.add(createPill("⚡ 1000 FPS Turbo Math"));
        pillsRow.add(createPill("🛡️ Low Shield & Small Totem"));
        pillsRow.add(createPill("🎨 In-Game Texture Packs"));
        pillsRow.add(createPill("🌊 VulkanMod Support"));
        centerContent.add(pillsRow);

        centerContent.add(Box.createRigidArea(new Dimension(0, 24)));

        // Community Buttons Row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);

        JButton gitBtn = createLinkButton("GitHub Repository", "https://github.com/Matzified/IceClient");
        JButton modStoreBtn = createLinkButton("Browse Mod Store", null);
        modStoreBtn.addActionListener(e -> {
            selectedNavIndex = 2;
            cardLayout.show(mainContentCardPanel, "MODSTORE");
        });

        btnRow.add(gitBtn);
        btnRow.add(modStoreBtn);
        centerContent.add(btnRow);

        showcaseCard.add(centerContent, BorderLayout.CENTER);
        page.add(showcaseCard);

        return page;
    }

    private JLabel createPill(String text) {
        JLabel pill = new JLabel(" " + text + " ");
        pill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pill.setForeground(new Color(203, 213, 225));
        pill.setOpaque(true);
        pill.setBackground(new Color(33, 38, 45));
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(48, 54, 61), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        return pill;
    }

    private JButton createLinkButton(String title, String url) {
        JButton btn = new JButton(title) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(48, 54, 61) : new Color(33, 38, 45);
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
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setPreferredSize(new Dimension(160, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (url != null) {
            btn.addActionListener(e -> {
                try {
                    Desktop.getDesktop().browse(URI.create(url));
                } catch (Exception ignored) {}
            });
        }
        return btn;
    }

    private JPanel createBottomLaunchBar() {
        JPanel launchBar = new JPanel(new BorderLayout(16, 0));
        launchBar.setBackground(new Color(22, 27, 34));
        launchBar.setPreferredSize(new Dimension(getWidth(), 72));
        launchBar.setBorder(new EmptyBorder(12, 24, 12, 24));

        // Left Version Selector Pill
        Profile p = profileManager.getActiveProfile();
        String pName = p != null ? p.getName() : "Vanilla (1.21.1)";
        String pVer = p != null ? p.getMcVersion() : "1.21.1";

        versionSelectBtn = new JButton("🧊 " + pName + " (" + pVer + ") ▾") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(33, 38, 45) : new Color(14, 17, 23);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(new Color(48, 54, 61));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        versionSelectBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        versionSelectBtn.setForeground(Color.WHITE);
        versionSelectBtn.setFocusPainted(false);
        versionSelectBtn.setBorderPainted(false);
        versionSelectBtn.setContentAreaFilled(false);
        versionSelectBtn.setPreferredSize(new Dimension(260, 46));
        versionSelectBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        versionSelectBtn.addActionListener(e -> {
            selectedNavIndex = 1;
            for (int j = 0; j < dockTabs.length; j++) {
                if (dockTabs[j] != null) {
                    dockTabs[j].setForeground(j == 1 ? new Color(56, 189, 248) : new Color(148, 163, 184));
                    dockTabs[j].repaint();
                }
            }
            cardLayout.show(mainContentCardPanel, "PROFILES");
        });
        launchBar.add(versionSelectBtn, BorderLayout.WEST);

        // Center Telemetry Info
        JPanel centerInfo = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 12));
        centerInfo.setOpaque(false);

        JLabel ramInfo = new JLabel("RAM: 4.0 GB • Fabric 0.16.0");
        ramInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ramInfo.setForeground(new Color(148, 163, 184));
        centerInfo.add(ramInfo);

        launchBar.add(centerInfo, BorderLayout.CENTER);

        // Right Launch Button
        launchGameBtn = new JButton("LAUNCH MINECRAFT " + pVer) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(16, 185, 129) : new Color(5, 150, 105);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        launchGameBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        launchGameBtn.setForeground(Color.WHITE);
        launchGameBtn.setFocusPainted(false);
        launchGameBtn.setBorderPainted(false);
        launchGameBtn.setContentAreaFilled(false);
        launchGameBtn.setPreferredSize(new Dimension(240, 46));
        launchGameBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        launchGameBtn.addActionListener(e -> onLaunchClicked());

        launchBar.add(launchGameBtn, BorderLayout.EAST);
        return launchBar;
    }

    private void onLaunchClicked() {
        Profile activeProfile = profileManager.getActiveProfile();
        if (activeProfile == null) return;

        Account activeAccount = accountManager.getActiveAccount();
        launchOverlay.showOn(getLayeredPane());

        new Thread(() -> {
            try {
                MinecraftLaunchEngine engine = new MinecraftLaunchEngine();
                engine.setStatusCallback(status -> launchOverlay.setStatus(status));
                engine.setProgressCallback(progress -> launchOverlay.setProgress(progress));
                launchOverlay.setOnCancel(engine::cancel);

                Process process = engine.launch(activeProfile, activeAccount);
                runningProcess = process;
                SwingUtilities.invokeLater(() -> launchOverlay.hideFrom(getLayeredPane()));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    launchOverlay.hideFrom(getLayeredPane());
                    JOptionPane.showMessageDialog(IceClientLauncher.this, ex.getMessage(), "Launch Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void onProfileStateChanged() {
        Profile p = profileManager.getActiveProfile();
        if (p != null && versionSelectBtn != null) {
            versionSelectBtn.setText("🧊 " + p.getName() + " (" + p.getMcVersion() + ") ▾");
            launchGameBtn.setText("LAUNCH MINECRAFT " + p.getMcVersion());
        }
        profileManagerPanel.refreshProfiles();
        modsManagerPanel.refreshMods();
        repaint();
    }

    private void onAccountStateChanged() {
        repaint();
    }

    private void openCreateProfileDialog() {
        new CreateProfileDialog(this, this::onProfileStateChanged, tabIdx -> {
            selectedNavIndex = tabIdx;
            cardLayout.show(mainContentCardPanel, "PROFILES");
        }).setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new IceClientLauncher().setVisible(true);
        });
    }
}