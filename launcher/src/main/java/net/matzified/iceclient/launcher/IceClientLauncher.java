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
    private JLabel activeProfileSubLabel;
    private JLabel playerNameLbl;
    private JLabel welcomeLbl;

    private ProfileManagerPanel profileManagerPanel;
    private ModsManagerPanel modsManagerPanel;
    private ModrinthPanel modrinthPanel;
    private LoginPanel loginPanel;

    private LaunchOverlayPanel launchOverlay;
    private AccountSwitcherPopup accountSwitcherPopup;

    private volatile Process runningProcess = null;

    private enum LaunchState { READY, STARTING, RUNNING }
    private LaunchState launchState = LaunchState.READY;

    private ImageIcon logoIcon;
    private int selectedNavIndex = 0;
    private final JButton[] dockTabs = new JButton[5];

    public IceClientLauncher() {
        setUndecorated(true);
        setTitle("Ice Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 780);
        setMinimumSize(new Dimension(1040, 700));
        setLocationRelativeTo(null);

        loadLogo();
        if (logoIcon != null) {
            setIconImage(logoIcon.getImage());
        }

        AutoUpdater.checkForUpdatesAsync(this);

        getContentPane().setBackground(new Color(11, 14, 20));
        setLayout(new BorderLayout());

        // 1. Top Custom Window Bar
        JPanel windowTitleBar = createWindowTitleBar();
        add(windowTitleBar, BorderLayout.NORTH);

        // 2. Center Content Area (Cards)
        cardLayout = new CardLayout();
        mainContentCardPanel = new JPanel(cardLayout);
        mainContentCardPanel.setOpaque(false);

        JPanel launchpadPage = createLaunchpadPage();
        profileManagerPanel = new ProfileManagerPanel(this::onProfileStateChanged, this::openCreateProfileDialog);
        modrinthPanel = new ModrinthPanel();
        modsManagerPanel = new ModsManagerPanel();
        loginPanel = new LoginPanel(this::onAccountStateChanged);

        mainContentCardPanel.add(launchpadPage, "LAUNCHPAD");
        mainContentCardPanel.add(profileManagerPanel, "PROFILES");
        mainContentCardPanel.add(modrinthPanel, "MODSTORE");
        mainContentCardPanel.add(modsManagerPanel, "MODS");
        mainContentCardPanel.add(loginPanel, "ACCOUNTS");

        add(mainContentCardPanel, BorderLayout.CENTER);

        // 3. Centered Bottom Navigation Dock
        JPanel bottomBar = createCenteredBottomDock();
        add(bottomBar, BorderLayout.SOUTH);

        // Launch Overlay Layer
        launchOverlay = new LaunchOverlayPanel();
        accountSwitcherPopup = new AccountSwitcherPopup(this, this::onAccountStateChanged);
        accountManager.addListener(this::onAccountStateChanged);

        try {
            setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
        } catch (Exception ignored) {}

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                try {
                    setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
                } catch (Exception ignored) {}
                if (launchOverlay.isVisible()) {
                    launchOverlay.setBounds(0, 0, getLayeredPane().getWidth(), getLayeredPane().getHeight());
                }
            }
        });
    }

    private void loadLogo() {
        try (InputStream is = getClass().getResourceAsStream("/assets/logo.png")) {
            if (is != null) {
                Image img = ImageIO.read(is);
                if (img != null) {
                    logoIcon = new ImageIcon(img);
                }
            }
        } catch (Exception ignored) {}
    }

    private JPanel createWindowTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(9, 12, 18));
        bar.setPreferredSize(new Dimension(getWidth(), 52));
        bar.setBorder(new EmptyBorder(0, 20, 0, 16));

        // Window drag listener
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

        JLabel logoText = new JLabel("🧊 ICE CLIENT");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logoText.setForeground(new Color(56, 189, 248));
        leftBrand.add(logoText);

        JLabel tagPill = new JLabel(" 1000 FPS COMPETITIVE ");
        tagPill.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tagPill.setForeground(new Color(148, 163, 184));
        tagPill.setOpaque(true);
        tagPill.setBackground(new Color(20, 28, 44));
        tagPill.setBorder(new EmptyBorder(3, 8, 3, 8));
        leftBrand.add(tagPill);

        bar.add(leftBrand, BorderLayout.WEST);

        // Right Account & Window Control Buttons
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        rightActions.setOpaque(false);

        // Active Account Pill
        Account activeAcc = accountManager.getActiveAccount();
        String playerName = activeAcc != null ? activeAcc.getUsername() : "IcePlayer";

        JButton accountBtn = new JButton("👤 " + playerName) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(28, 36, 52) : new Color(18, 24, 38);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(new Color(56, 189, 248, 100));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        accountBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        accountBtn.setForeground(Color.WHITE);
        accountBtn.setFocusPainted(false);
        accountBtn.setBorderPainted(false);
        accountBtn.setContentAreaFilled(false);
        accountBtn.setPreferredSize(new Dimension(140, 32));
        accountBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        accountBtn.addActionListener(e -> new MicrosoftLoginDialog(this, this::onAccountStateChanged).setVisible(true));
        rightActions.add(accountBtn);

        // Minimize Button
        JButton minBtn = new JButton("—");
        minBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        minBtn.setForeground(new Color(148, 163, 184));
        minBtn.setFocusPainted(false);
        minBtn.setBorderPainted(false);
        minBtn.setContentAreaFilled(false);
        minBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        minBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        rightActions.add(minBtn);

        // Close Button
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
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

    private JPanel createCenteredBottomDock() {
        JPanel dockContainer = new JPanel(new BorderLayout());
        dockContainer.setBackground(new Color(9, 12, 18));
        dockContainer.setBorder(new EmptyBorder(10, 20, 14, 20));

        // Floating Glass Dock Box
        JPanel dockPill = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 20, 30));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(new Color(56, 189, 248, 60));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        dockPill.setOpaque(false);

        String[] tabNames = {"🎮 PLAY", "📦 PROFILES", "🛒 MOD STORE", "🧩 MODS", "👤 ACCOUNTS"};
        String[] cardNames = {"LAUNCHPAD", "PROFILES", "MODSTORE", "MODS", "ACCOUNTS"};

        for (int i = 0; i < tabNames.length; i++) {
            final int idx = i;
            final String cName = cardNames[i];
            JButton tab = new JButton(tabNames[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean isSel = (selectedNavIndex == idx);
                    Color bg = isSel ? new Color(2, 132, 199) : (getModel().isRollover() ? new Color(28, 36, 52) : new Color(0, 0, 0, 0));
                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                    if (isSel) {
                        g2.setColor(new Color(56, 189, 248));
                        g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            tab.setFont(new Font("Segoe UI", Font.BOLD, 13));
            tab.setForeground(Color.WHITE);
            tab.setFocusPainted(false);
            tab.setBorderPainted(false);
            tab.setContentAreaFilled(false);
            tab.setPreferredSize(new Dimension(140, 36));
            tab.setCursor(new Cursor(Cursor.HAND_CURSOR));

            tab.addActionListener(e -> {
                selectedNavIndex = idx;
                cardLayout.show(mainContentCardPanel, cName);
                dockPill.repaint();
            });

            dockTabs[i] = tab;
            dockPill.add(tab);
        }

        dockContainer.add(dockPill, BorderLayout.CENTER);
        return dockContainer;
    }

    private JPanel createLaunchpadPage() {
        JPanel page = new JPanel(new BorderLayout(18, 18));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(20, 28, 14, 28));

        // 1. Hero Showcase Banner
        JPanel heroBanner = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(14, 22, 38), getWidth(), getHeight(), new Color(9, 14, 24));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(new Color(56, 189, 248, 100));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        heroBanner.setOpaque(false);
        heroBanner.setPreferredSize(new Dimension(getWidth(), 140));
        heroBanner.setBorder(new EmptyBorder(22, 28, 22, 28));

        JPanel heroText = new JPanel();
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        heroText.setOpaque(false);

        JLabel heroTitle = new JLabel("ICE CLIENT • 1000 FPS COMPETITIVE ECOSYSTEM");
        heroTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heroTitle.setForeground(new Color(56, 189, 248));
        heroText.add(heroTitle);

        heroText.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel heroSub = new JLabel("Low-latency mathematical fast-math acceleration, VulkanMod support & 38+ fair-play PvP modules.");
        heroSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        heroSub.setForeground(new Color(148, 163, 184));
        heroText.add(heroSub);

        heroBanner.add(heroText, BorderLayout.CENTER);
        page.add(heroBanner, BorderLayout.NORTH);

        // 2. Feature & Media Cards Grid
        JPanel mediaGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        mediaGrid.setOpaque(false);

        mediaGrid.add(createFeatureCard("⚡ 1000 FPS Turbo Engine", "Precomputed FastMath trig tables, GC idle cleaner, and Java 21 ZGC memory pre-touch."));
        mediaGrid.add(createFeatureCard("🛡️ Modern 1.21+ PvP Suite", "Mace stomp multiplier, Wind Charge tracker, Shield disable countdown & reach tracking."));
        mediaGrid.add(createFeatureCard("🌊 Vulkan API Ready", "Native zero-overhead VulkanMod integration without OpenGL framebuffer collision."));

        page.add(mediaGrid, BorderLayout.CENTER);

        // 3. Big Glowing Launch Footer Bar
        JPanel launchBar = new JPanel(new BorderLayout(20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 20, 30));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(new Color(56, 189, 248, 80));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.dispose();
            }
        };
        launchBar.setOpaque(false);
        launchBar.setPreferredSize(new Dimension(getWidth(), 76));
        launchBar.setBorder(new EmptyBorder(12, 22, 12, 22));

        // Active Profile Info
        JPanel profInfo = new JPanel(new GridLayout(2, 1, 0, 2));
        profInfo.setOpaque(false);

        Profile p = profileManager.getActiveProfile();
        String pName = p != null ? p.getName() : "Vanilla (1.21.1)";
        String pVer = p != null ? p.getMcVersion() : "1.21.1";

        JLabel profTitle = new JLabel("🎯 Profile: " + pName);
        profTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        profTitle.setForeground(Color.WHITE);

        activeProfileSubLabel = new JLabel("Fabric 1.21.1 • In-Engine FastMath & Ice HUD Active");
        activeProfileSubLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        activeProfileSubLabel.setForeground(new Color(56, 189, 248));

        profInfo.add(profTitle);
        profInfo.add(activeProfileSubLabel);
        launchBar.add(profInfo, BorderLayout.WEST);

        // Launch Button
        launchGameBtn = new JButton("🚀 LAUNCH MINECRAFT") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(2, 132, 199) : new Color(3, 105, 161);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        launchGameBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        launchGameBtn.setForeground(Color.WHITE);
        launchGameBtn.setFocusPainted(false);
        launchGameBtn.setBorderPainted(false);
        launchGameBtn.setContentAreaFilled(false);
        launchGameBtn.setPreferredSize(new Dimension(240, 50));
        launchGameBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        launchGameBtn.addActionListener(e -> onLaunchClicked());

        launchBar.add(launchGameBtn, BorderLayout.EAST);
        page.add(launchBar, BorderLayout.SOUTH);

        return page;
    }

    private JPanel createFeatureCard(String title, String desc) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 20, 30));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(new Color(255, 255, 255, 15));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(new Color(56, 189, 248));
        card.add(titleLbl, BorderLayout.NORTH);

        JLabel descLbl = new JLabel("<html>" + desc + "</html>");
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setForeground(new Color(148, 163, 184));
        card.add(descLbl, BorderLayout.CENTER);

        return card;
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