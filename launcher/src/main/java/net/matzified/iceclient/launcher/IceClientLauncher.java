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
    private ImageIcon scaledLogo32;
    private ImageIcon chromeGemIcon;

    public IceClientLauncher() {
        setUndecorated(true);
        setTitle("Ice Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1140, 720);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);

        loadAssets();
        if (logoIcon != null) {
            setIconImage(logoIcon.getImage());
        }

        AutoUpdater.checkForUpdatesAsync(this);

        // Clean Obsidian Background
        JPanel rootPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(13, 17, 23));
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

    private void loadAssets() {
        try (InputStream is = getClass().getResourceAsStream("/assets/logo.png")) {
            if (is != null) {
                Image img = ImageIO.read(is);
                if (img != null) {
                    logoIcon = new ImageIcon(img);
                    scaledLogo32 = new ImageIcon(img.getScaledInstance(32, 32, Image.SCALE_SMOOTH));
                }
            }
        } catch (Exception ignored) {}

        try (InputStream is = getClass().getResourceAsStream("/assets/ChromeGem.png")) {
            if (is != null) {
                Image img = ImageIO.read(is);
                if (img != null) {
                    chromeGemIcon = new ImageIcon(img.getScaledInstance(140, 140, Image.SCALE_SMOOTH));
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
        JPanel leftBrand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        leftBrand.setOpaque(false);

        if (scaledLogo32 != null) {
            leftBrand.add(new JLabel(scaledLogo32));
        }

        JLabel logoText = new JLabel("ICE CLIENT");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 15));
        logoText.setForeground(Color.WHITE);
        leftBrand.add(logoText);

        bar.add(leftBrand, BorderLayout.WEST);

        // Center Navigation Tabs
        JPanel navCenter = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 10));
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
                        g2.setColor(new Color(56, 189, 248, 30));
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
            tab.setPreferredSize(new Dimension(95, 32));
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
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(22, 28, 18, 28));

        // 1. Full-Bleed Hero Showcase Banner (Lunar Style)
        JPanel heroBanner = new JPanel(new BorderLayout(24, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Subtle linear gradient from dark slate to deep navy
                GradientPaint gp = new GradientPaint(0, 0, new Color(22, 27, 34), getWidth(), 0, new Color(17, 24, 39));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(48, 54, 61));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        heroBanner.setOpaque(false);
        heroBanner.setPreferredSize(new Dimension(getWidth(), 190));
        heroBanner.setBorder(new EmptyBorder(24, 32, 24, 32));

        // Left Hero Text
        JPanel heroLeft = new JPanel();
        heroLeft.setLayout(new BoxLayout(heroLeft, BoxLayout.Y_AXIS));
        heroLeft.setOpaque(false);

        JLabel updateBadge = new JLabel(" ICE CLIENT 1.21.1 COMPETITIVE ");
        updateBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        updateBadge.setForeground(new Color(56, 189, 248));
        updateBadge.setOpaque(true);
        updateBadge.setBackground(new Color(56, 189, 248, 25));
        updateBadge.setBorder(new EmptyBorder(3, 8, 3, 8));
        heroLeft.add(updateBadge);

        heroLeft.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel heroTitle = new JLabel("Max Performance & PvP Advantage");
        heroTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        heroTitle.setForeground(Color.WHITE);
        heroLeft.add(heroTitle);

        heroLeft.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel heroDesc = new JLabel("FastMath acceleration, Small Totem, Low Shield, and In-Game Texture Pack Store.");
        heroDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        heroDesc.setForeground(new Color(148, 163, 184));
        heroLeft.add(heroDesc);

        heroLeft.add(Box.createRigidArea(new Dimension(0, 14)));

        // Action Buttons Row inside Hero
        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionBtns.setOpaque(false);

        JButton storeBtn = new JButton("Explore Mod Store") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(2, 132, 199) : new Color(3, 105, 161);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        storeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        storeBtn.setForeground(Color.WHITE);
        storeBtn.setFocusPainted(false);
        storeBtn.setBorderPainted(false);
        storeBtn.setContentAreaFilled(false);
        storeBtn.setPreferredSize(new Dimension(150, 34));
        storeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        storeBtn.addActionListener(e -> {
            selectedNavIndex = 2;
            cardLayout.show(mainContentCardPanel, "MODSTORE");
        });
        actionBtns.add(storeBtn);

        JButton gitBtn = new JButton("GitHub Repo") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(48, 54, 61) : new Color(33, 38, 45);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(new Color(56, 189, 248, 80));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        gitBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gitBtn.setForeground(Color.WHITE);
        gitBtn.setFocusPainted(false);
        gitBtn.setBorderPainted(false);
        gitBtn.setContentAreaFilled(false);
        gitBtn.setPreferredSize(new Dimension(120, 34));
        gitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gitBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/Matzified/IceClient"));
            } catch (Exception ignored) {}
        });
        actionBtns.add(gitBtn);

        heroLeft.add(actionBtns);
        heroBanner.add(heroLeft, BorderLayout.CENTER);

        // Right Hero Artwork Gem
        if (chromeGemIcon != null) {
            JLabel gem = new JLabel(chromeGemIcon);
            heroBanner.add(gem, BorderLayout.EAST);
        }

        page.add(heroBanner, BorderLayout.NORTH);

        // 2. Quick Server Connect Bar (Lunar Client Feature)
        JPanel serversBar = new JPanel(new GridLayout(1, 4, 14, 0));
        serversBar.setOpaque(false);

        serversBar.add(createServerCard("Hypixel Network", "mc.hypixel.net", "Competitive & BedWars"));
        serversBar.add(createServerCard("Minemen Club", "na.minemen.club", "Ranked 1v1 PvP & Practice"));
        serversBar.add(createServerCard("GommeHD.net", "gommehd.net", "European BedWars"));
        serversBar.add(createServerCard("Singleplayer", "Local Worlds", "Zero-Latency Survival"));

        page.add(serversBar, BorderLayout.CENTER);

        return page;
    }

    private JPanel createServerCard(String name, String ip, String desc) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
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
                Color bg = isHover ? new Color(28, 33, 42) : new Color(22, 27, 34);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(isHover ? new Color(56, 189, 248, 140) : new Color(48, 54, 61));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Top Row: Server Name & Online Status
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(Color.WHITE);
        top.add(nameLbl, BorderLayout.WEST);

        JLabel ping = new JLabel("● Online");
        ping.setFont(new Font("Segoe UI", Font.BOLD, 10));
        ping.setForeground(new Color(52, 211, 153));
        top.add(ping, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Center IP & Description
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel ipLbl = new JLabel(ip);
        ipLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ipLbl.setForeground(new Color(56, 189, 248));
        center.add(ipLbl);

        center.add(Box.createRigidArea(new Dimension(0, 3)));

        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLbl.setForeground(new Color(148, 163, 184));
        center.add(descLbl);

        card.add(center, BorderLayout.CENTER);
        return card;
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