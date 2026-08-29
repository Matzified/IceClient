package net.matzified.iceclient.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import net.matzified.iceclient.launcher.auth.Account;
import net.matzified.iceclient.launcher.auth.AccountManager;
import net.matzified.iceclient.launcher.launch.MinecraftLaunchEngine;
import net.matzified.iceclient.launcher.model.Profile;
import net.matzified.iceclient.launcher.profile.ProfileManager;
import net.matzified.iceclient.launcher.ui.*;
import net.matzified.iceclient.launcher.ui.VectorIcon.IconType;
import net.matzified.iceclient.launcher.utils.AutoUpdater;

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
import javax.imageio.ImageIO;

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

    private LaunchOverlayPanel  launchOverlay;
    private AccountSwitcherPopup accountSwitcherPopup;

    /** Tracks the currently running Minecraft process (null when not running). */
    private volatile Process runningProcess = null;

    /** Button visual states */
    private enum LaunchState { READY, STARTING, RUNNING }
    private LaunchState launchState = LaunchState.READY;

    private ImageIcon logoIcon;
    private int selectedNavIndex = 0;
    private final JPanel[]     navButtons   = new JPanel[5];
    private final VectorIcon[] vectorIcons  = new VectorIcon[5];

    public IceClientLauncher() {
        setUndecorated(true);
        setTitle("Ice Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);

        loadLogo();
        if (taskbarIcon != null) {
            setIconImage(taskbarIcon.getImage());
        } else if (logoIcon != null) {
            setIconImage(logoIcon.getImage());
        }

        AutoUpdater.checkForUpdatesAsync(this);

        getContentPane().setBackground(new Color(13, 14, 18));
        setLayout(new BorderLayout());

        // Custom Top Window Bar
        JPanel windowTitleBar = createWindowTitleBar();
        add(windowTitleBar, BorderLayout.NORTH);

        // Left Vertical Sidebar with Modern Vector Icons
        JPanel leftSidebar = createLeftSidebarWithVectorIcons();
        add(leftSidebar, BorderLayout.WEST);

        // Center Content Area
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

        // ---- Launch overlay (covers window via layered pane) ----
        launchOverlay = new LaunchOverlayPanel();

        // ---- Account switcher popup ----
        accountSwitcherPopup = new AccountSwitcherPopup(this, this::onAccountStateChanged);

        // ---- Register account change listener ----
        accountManager.addListener(this::onAccountStateChanged);

        // ---- Resize overlay with window ----
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (launchOverlay.isVisible()) {
                    launchOverlay.setBounds(0, 0, getLayeredPane().getWidth(), getLayeredPane().getHeight());
                }
            }
        });
    }

    private ImageIcon taskbarIcon;

    private void loadLogo() {
        try (InputStream is = getClass().getResourceAsStream("/assets/logo_taskbar.png")) {
            if (is != null) {
                Image img = ImageIO.read(is);
                if (img != null) {
                    taskbarIcon = new ImageIcon(img);
                }
            }
        } catch (Exception ignored) {}

        try (InputStream is = getClass().getResourceAsStream("/assets/logo.png")) {
            if (is != null) {
                Image img = ImageIO.read(is);
                if (img != null) {
                    Image scaled = img.getScaledInstance(36, 36, Image.SCALE_SMOOTH);
                    logoIcon = new ImageIcon(scaled);
                }
            }
        } catch (Exception ignored) {}
    }

    private JPanel createWindowTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(13, 15, 20));
        bar.setPreferredSize(new Dimension(0, 44));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(28, 32, 42)));

        bar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });
        bar.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset != null) {
                    Point curr = getLocation();
                    setLocation(curr.x + e.getX() - dragOffset.x, curr.y + e.getY() - dragOffset.y);
                }
            }
        });

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        brand.setOpaque(false);

        if (logoIcon != null) {
            brand.add(new JLabel(logoIcon));
        }

        FrozenLabel titleLabel = new FrozenLabel("ICE CLIENT", 16);
        brand.add(titleLabel);

        JLabel versionBadge = new JLabel(" v1.0.0 (1.21-1.21.11 Fabric)");
        versionBadge.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        versionBadge.setForeground(new Color(148, 163, 184));
        brand.add(versionBadge);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        rightControls.setOpaque(false);

        // Lunar-Style Player Status Pill Button (Opens Account Manager)
        JPanel playerBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(24, 28, 38));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));

                g2.setColor(new Color(56, 189, 248, 140));
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));

                // Green Online Dot
                g2.setColor(new Color(16, 185, 129));
                g2.fillOval(10, getHeight() / 2 - 4, 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        playerBadge.setOpaque(false);
        playerBadge.setBorder(new EmptyBorder(4, 22, 4, 14));
        playerBadge.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playerBadge.setToolTipText("Open Account Manager");

        Account activeAcc = accountManager.getActiveAccount();
        playerNameLbl = new JLabel(activeAcc != null ? activeAcc.getUsername() : "Matzified");
        playerNameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        playerNameLbl.setForeground(Color.WHITE);
        playerBadge.add(playerNameLbl);

        playerBadge.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                accountSwitcherPopup.toggle(playerBadge);
            }
        });

        // Lunar-Style Minimalist Vector Window Controls
        JButton minBtn = createVectorWinControl(WinControlKind.MINIMIZE, e -> setState(Frame.ICONIFIED));
        JButton maxBtn = createVectorWinControl(WinControlKind.MAXIMIZE, e -> setExtendedState(getExtendedState() == MAXIMIZED_BOTH ? NORMAL : MAXIMIZED_BOTH));
        JButton closeBtn = createVectorWinControl(WinControlKind.CLOSE, e -> System.exit(0));

        rightControls.add(playerBadge);
        rightControls.add(minBtn);
        rightControls.add(maxBtn);
        rightControls.add(closeBtn);

        bar.add(brand, BorderLayout.WEST);
        bar.add(rightControls, BorderLayout.EAST);
        return bar;
    }

    private enum WinControlKind { MINIMIZE, MAXIMIZE, CLOSE }

    private JButton createVectorWinControl(WinControlKind kind, java.awt.event.ActionListener action) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isHover = getModel().isRollover();
                if (isHover) {
                    g2.setColor(kind == WinControlKind.CLOSE ? new Color(225, 29, 72) : new Color(38, 44, 58));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                }

                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                if (kind == WinControlKind.MINIMIZE) {
                    g2.drawLine(cx - 5, cy, cx + 5, cy);
                } else if (kind == WinControlKind.MAXIMIZE) {
                    g2.drawRoundRect(cx - 5, cy - 5, 10, 10, 2, 2);
                } else if (kind == WinControlKind.CLOSE) {
                    g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                    g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
                }

                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(32, 28));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        return btn;
    }

    private JPanel createLaunchpadPage() {
        JPanel page = new JPanel(new BorderLayout(0, 20));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(20, 26, 24, 26));

        // Container for Top Sections (Launch Hero + 4 Quick Stats Widgets)
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // ==========================================
        // 1. High-End Glassmorphic Launch Hero Box
        // ==========================================
        JPanel launchBox = new JPanel(new BorderLayout(20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Deep Midnight Gradient with ambient cyan radial glow
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(14, 165, 233, 45),
                        getWidth(), getHeight(), new Color(10, 14, 23, 245)
                );
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));

                // Glowing Cyan Outer Border
                g2.setColor(new Color(56, 189, 248, 110));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        launchBox.setOpaque(false);
        launchBox.setPreferredSize(new Dimension(1000, 220));
        launchBox.setBorder(new EmptyBorder(18, 28, 18, 28));

        Account activeAcc = accountManager.getActiveAccount();
        String name = activeAcc != null ? activeAcc.getUsername() : "Matzified";

        welcomeLbl = new JLabel("Welcome Back, " + name, SwingConstants.CENTER);
        welcomeLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcomeLbl.setForeground(Color.WHITE);

        // Premium Cyan/Ice Gradient Launch Game Button — 3 states: READY / STARTING / RUNNING
        launchGameBtn = new JButton("▶  LAUNCH GAME") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                if (launchState == LaunchState.STARTING) {
                    // Charcoal disabled
                    g2.setColor(new Color(38, 44, 58));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
                    g2.setColor(new Color(75, 85, 99));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 14, 14));

                } else if (launchState == LaunchState.RUNNING) {
                    // Crimson Red STOP gradient
                    Color top    = getModel().isRollover() ? new Color(239, 68, 68)  : new Color(220, 38, 38);
                    Color bottom = getModel().isRollover() ? new Color(185, 28, 28)  : new Color(153, 27, 27);
                    g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
                    g2.setColor(new Color(254, 202, 202, 160));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 14, 14));

                } else {
                    // Blue LAUNCH gradient (READY state)
                    Color top    = getModel().isRollover() ? new Color(56, 189, 248) : new Color(14, 165, 233);
                    Color bottom = getModel().isRollover() ? new Color(2, 132, 199)  : new Color(3, 105, 161);
                    g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
                    g2.setColor(getModel().isRollover() ? new Color(224, 242, 254, 220) : new Color(186, 230, 253, 140));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 14, 14));
                }

                // Explicitly render button text
                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                String text = getText();
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);

                g2.dispose();
            }
        };
        launchGameBtn.setFont(new Font("Segoe UI", Font.BOLD, 17));
        launchGameBtn.setForeground(Color.WHITE);
        launchGameBtn.setFocusPainted(false);
        launchGameBtn.setContentAreaFilled(false);
        launchGameBtn.setBorderPainted(false);
        launchGameBtn.setPreferredSize(new Dimension(340, 52));
        launchGameBtn.setMinimumSize(new Dimension(340, 52));
        launchGameBtn.setMaximumSize(new Dimension(340, 52));
        launchGameBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        launchGameBtn.addActionListener(e -> {
            if (launchState == LaunchState.RUNNING) {
                stopMinecraft();
            } else if (launchState == LaunchState.READY) {
                launchMinecraft();
            }
        });

        activeProfileSubLabel = new JLabel("⚡ Fabric 1.21.1 · FPS Boosted (Optimized) · 4 GB RAM", SwingConstants.CENTER);
        activeProfileSubLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        activeProfileSubLabel.setForeground(new Color(186, 230, 253));

        updateActiveProfileStatus();

        JPanel centerBox = new JPanel();
        centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
        centerBox.setOpaque(false);

        welcomeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        launchGameBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        activeProfileSubLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerBox.add(Box.createVerticalGlue());
        centerBox.add(welcomeLbl);
        centerBox.add(Box.createRigidArea(new Dimension(0, 14)));
        centerBox.add(launchGameBtn);
        centerBox.add(Box.createRigidArea(new Dimension(0, 12)));
        centerBox.add(activeProfileSubLabel);
        centerBox.add(Box.createVerticalGlue());

        if (logoIcon != null) {
            Image bigImg = logoIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            JLabel leftArtwork = new JLabel(new ImageIcon(bigImg));
            JLabel rightArtwork = new JLabel(new ImageIcon(bigImg));
            leftArtwork.setPreferredSize(new Dimension(110, 110));
            rightArtwork.setPreferredSize(new Dimension(110, 110));
            leftArtwork.setHorizontalAlignment(SwingConstants.CENTER);
            rightArtwork.setHorizontalAlignment(SwingConstants.CENTER);
            launchBox.add(leftArtwork, BorderLayout.WEST);
            launchBox.add(rightArtwork, BorderLayout.EAST);
        }

        launchBox.add(centerBox, BorderLayout.CENTER);
        topContainer.add(launchBox);
        topContainer.add(Box.createRigidArea(new Dimension(0, 16)));

        // ==========================================
        // 2. Interactive 4-Widget Quick Stats Grid
        // ==========================================
        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        statsGrid.setOpaque(false);
        statsGrid.setPreferredSize(new Dimension(1000, 68));

        statsGrid.add(createStatCard("⚡ Engine", "Fabric 1.21.1", new Color(56, 189, 248)));
        statsGrid.add(createStatCard("🚀 FPS Boost", "Sodium + Iris Active", new Color(52, 211, 153)));
        statsGrid.add(createStatCard("💾 Memory", "4 GB Allocated", new Color(192, 132, 252)));
        statsGrid.add(createStatCard("🛡️ Protection", "Isolated Instance", new Color(251, 191, 36)));

        topContainer.add(statsGrid);
        page.add(topContainer, BorderLayout.NORTH);

        // ==========================================
        // 3. Quick Join Server Cards Section
        // ==========================================
        JPanel serversSection = new JPanel(new BorderLayout(0, 12));
        serversSection.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel jumpInHeader = new JLabel("🎮 Quick Join Servers");
        jumpInHeader.setFont(new Font("Segoe UI", Font.BOLD, 17));
        jumpInHeader.setForeground(Color.WHITE);

        JLabel subHeader = new JLabel("1-Click Launch & Direct Connect");
        subHeader.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subHeader.setForeground(new Color(148, 163, 184));

        headerRow.add(jumpInHeader, BorderLayout.WEST);
        headerRow.add(subHeader, BorderLayout.EAST);
        serversSection.add(headerRow, BorderLayout.NORTH);

        JPanel serversGrid = new JPanel(new GridLayout(3, 1, 0, 10));
        serversGrid.setOpaque(false);

        serversGrid.add(createServerCard("Minemen Club [EU] (1.21.1)", "Minemen.Club — Duels / FFA / Practice Teams", "3,860 online", "18ms"));
        serversGrid.add(createServerCard("PvP Club Network (1.21.x)", "MCPVP.CLUB — Competitive Ranked Practice", "8,009 online", "24ms"));
        serversGrid.add(createServerCard("Fox SMP Community (1.21.11)", "discord.gg/IceClient — Survival SMP Community", "124 online", "12ms"));

        serversSection.add(serversGrid, BorderLayout.CENTER);
        page.add(serversSection, BorderLayout.CENTER);

        return page;
    }

    private JPanel createStatCard(String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(20, 24, 34, 220));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));

                g2.setColor(new Color(45, 52, 68));
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(accent);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valLbl.setForeground(Color.WHITE);

        text.add(titleLbl);
        text.add(valLbl);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JPanel createServerCard(String name, String desc, String onlineCount, String ping) {
        boolean[] isHover = {false};
        JPanel card = new JPanel(new BorderLayout(15, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = isHover[0] ? new Color(28, 34, 48) : new Color(20, 24, 34);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));

                g2.setColor(isHover[0] ? new Color(56, 189, 248, 140) : new Color(45, 52, 68));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 18, 12, 18));
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

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 3));
        info.setOpaque(false);

        JLabel title = new JLabel(name);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel(desc + "  •  🟢 " + onlineCount + "  •  📶 " + ping);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(148, 163, 184));

        info.add(title);
        info.add(sub);

        JButton playBtn = new JButton("▶ Play") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color top = getModel().isRollover() ? new Color(56, 189, 248) : new Color(14, 165, 233);
                Color bottom = getModel().isRollover() ? new Color(2, 132, 199) : new Color(3, 105, 161);
                g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
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
        playBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        playBtn.setForeground(Color.WHITE);
        playBtn.setFocusPainted(false);
        playBtn.setContentAreaFilled(false);
        playBtn.setBorderPainted(false);
        playBtn.setPreferredSize(new Dimension(105, 38));
        playBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        playBtn.addActionListener(e -> launchMinecraft());

        card.add(info, BorderLayout.CENTER);
        card.add(playBtn, BorderLayout.EAST);
        return card;
    }

    private JPanel createLeftSidebarWithVectorIcons() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(18, 20, 26));
        sidebar.setPreferredSize(new Dimension(68, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(31, 35, 45)));

        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));

        IconType[] types = {IconType.HOME, IconType.PROFILES, IconType.STORE, IconType.MODS, IconType.ACCOUNTS};
        String[] tooltips = {"Launchpad", "Profiles & Instances", "Mod Store", "Installed Mods", "Account Manager"};
        String[] cards = {"LAUNCHPAD", "PROFILES", "MODSTORE", "MODS", "ACCOUNTS"};

        for (int i = 0; i < types.length; i++) {
            final int index = i;
            JPanel navItem = new JPanel(new BorderLayout());
            navItem.setOpaque(false);
            navItem.setMaximumSize(new Dimension(68, 54));
            navItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
            navItem.setToolTipText(tooltips[i]);

            VectorIcon vIcon = new VectorIcon(types[i]);
            vIcon.setIconColor(i == 0 ? new Color(56, 189, 248) : new Color(148, 163, 184));
            vectorIcons[i] = vIcon;

            JPanel iconWrapper = new JPanel(new GridBagLayout());
            iconWrapper.setOpaque(false);
            iconWrapper.add(vIcon);

            JPanel activeIndicator = new JPanel();
            activeIndicator.setPreferredSize(new Dimension(4, 0));
            activeIndicator.setBackground(i == 0 ? new Color(56, 189, 248) : new Color(18, 20, 26));

            navItem.add(activeIndicator, BorderLayout.WEST);
            navItem.add(iconWrapper, BorderLayout.CENTER);

            navItem.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectNavTab(index, cards[index]);
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (selectedNavIndex != index) {
                        navItem.setBackground(new Color(26, 29, 36));
                        navItem.setOpaque(true);
                        vIcon.setIconColor(new Color(240, 249, 255));
                        navItem.repaint();
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (selectedNavIndex != index) {
                        navItem.setOpaque(false);
                        vIcon.setIconColor(new Color(148, 163, 184));
                        navItem.repaint();
                    }
                }
            });

            navButtons[i] = navItem;
            sidebar.add(navItem);
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        sidebar.add(Box.createVerticalGlue());

        // Discord Community Button on Sidebar Footer
        JPanel discordItem = new JPanel(new GridBagLayout());
        discordItem.setOpaque(false);
        discordItem.setMaximumSize(new Dimension(68, 54));
        discordItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
        discordItem.setToolTipText("Join our Discord Community (discord.gg/IceClient)");

        JLabel discordLbl = new JLabel("💬");
        discordLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        discordItem.add(discordLbl);

        discordItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://discord.gg/IceClient"));
                } catch (Exception ignored) {}
            }
        });

        sidebar.add(discordItem);
        sidebar.add(Box.createRigidArea(new Dimension(0, 14)));

        return sidebar;
    }

    private void selectNavTab(int index, String cardName) {
        selectedNavIndex = index;
        for (int i = 0; i < navButtons.length; i++) {
            JPanel item = navButtons[i];
            JPanel indicator = (JPanel) item.getComponent(0);
            VectorIcon vIcon = vectorIcons[i];

            if (i == index) {
                item.setOpaque(true);
                item.setBackground(new Color(26, 29, 36));
                indicator.setBackground(new Color(56, 189, 248));
                if (vIcon != null) vIcon.setIconColor(new Color(56, 189, 248));
            } else {
                item.setOpaque(false);
                indicator.setBackground(new Color(18, 20, 26));
                if (vIcon != null) vIcon.setIconColor(new Color(148, 163, 184));
            }
            item.repaint();
        }

        if (index == 3) {
            modsManagerPanel.refreshModsList();
        } else if (index == 4) {
            loginPanel.refreshAccountsUI();
        }

        cardLayout.show(mainContentCardPanel, cardName);
    }

    private void openCreateProfileDialog() {
        CreateProfileDialog dlg = new CreateProfileDialog(this, this::onProfileStateChanged, tabIndex -> selectNavTab(tabIndex, "MODSTORE"));
        dlg.setVisible(true);
    }

    private void onAccountStateChanged() {
        Account active = accountManager.getActiveAccount();
        if (active != null) {
            if (playerNameLbl != null) playerNameLbl.setText(active.getUsername());
            if (welcomeLbl   != null) welcomeLbl.setText("Welcome Back, " + active.getUsername());
        }
    }

    private void onProfileStateChanged() {
        updateActiveProfileStatus();
        if (modsManagerPanel != null) {
            modsManagerPanel.refreshModsList();
        }
        if (profileManagerPanel != null) {
            profileManagerPanel.refreshProfileList();
        }
    }

    private void updateActiveProfileStatus() {
        Profile active = profileManager.getActiveProfile();
        if (active != null && activeProfileSubLabel != null) {
            activeProfileSubLabel.setText(active.getName() + " (" + active.getMcVersion() + ") - " + active.getRamGb() + " GB RAM");
        }
    }

    /** Transitions the launch button to the given state. Must be called on the EDT. */
    private void setLaunchState(LaunchState state) {
        launchState = state;
        switch (state) {
            case READY    -> { launchGameBtn.setText("▶  LAUNCH GAME"); launchGameBtn.setEnabled(true); }
            case STARTING -> { launchGameBtn.setText("⟳  STARTING..."); launchGameBtn.setEnabled(false); }
            case RUNNING  -> { launchGameBtn.setText("■  STOP GAME");   launchGameBtn.setEnabled(true); }
        }
        launchGameBtn.repaint();
    }

    private void stopMinecraft() {
        Process proc = runningProcess;
        if (proc != null && proc.isAlive()) {
            proc.destroy();
        }
        // State reset happens in the waitFor() block after proc exits
    }

    private void launchMinecraft() {
        Profile active    = profileManager.getActiveProfile();
        Account activeAcc = accountManager.getActiveAccount();

        if (active == null) {
            JOptionPane.showMessageDialog(this,
                "Please select an active profile first!",
                "Launch Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ---- Transition: READY → STARTING ----
        setLaunchState(LaunchState.STARTING);
        launchOverlay.setProgress(0);
        launchOverlay.setStatus("Initializing launch engine...");
        launchOverlay.showOn(getLayeredPane());

        MinecraftLaunchEngine engine = new MinecraftLaunchEngine();
        engine.setStatusCallback(launchOverlay::setStatus);
        engine.setProgressCallback(launchOverlay::setProgress);

        launchOverlay.setOnCancel(() -> {
            engine.cancel();
            SwingUtilities.invokeLater(() -> {
                launchOverlay.hideFrom(getLayeredPane());
                setLaunchState(LaunchState.READY);
            });
        });

        new Thread(() -> {
            try {
                Process proc = engine.launch(active, activeAcc);
                if (proc == null || engine.isCancelled()) {
                    SwingUtilities.invokeLater(() -> {
                        launchOverlay.hideFrom(getLayeredPane());
                        setLaunchState(LaunchState.READY);
                    });
                    return;
                }

                runningProcess = proc;

                // ---- Transition: STARTING → RUNNING ----
                SwingUtilities.invokeLater(() -> {
                    launchOverlay.hideFrom(getLayeredPane());
                    setLaunchState(LaunchState.RUNNING);
                });

                // Block until Minecraft exits
                proc.waitFor();
                runningProcess = null;

                // ---- Transition: RUNNING → READY ----
                SwingUtilities.invokeLater(() -> setLaunchState(LaunchState.READY));

            } catch (Exception ex) {
                runningProcess = null;
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    launchOverlay.hideFrom(getLayeredPane());
                    setLaunchState(LaunchState.READY);
                    JOptionPane.showMessageDialog(IceClientLauncher.this,
                        "Failed to launch Minecraft:\n" + ex.getMessage(),
                        "Launch Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "ice-launch-thread").start();
    }

    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored2) {}
        }

        SwingUtilities.invokeLater(() -> {
            FeatherSplashWindow splash = new FeatherSplashWindow();
            splash.setVisible(true);

            new Thread(() -> {
                try {
                    splash.updateProgress(15, "Loading Settings...");
                    Thread.sleep(350);

                    splash.updateProgress(45, "Initializing Fabric 1.21.1 Profile Manager...");
                    Thread.sleep(350);

                    splash.updateProgress(75, "Connecting to Modrinth Engine...");
                    Thread.sleep(350);

                    splash.updateProgress(90, "Preparing Account Credentials...");
                    Thread.sleep(300);

                    splash.updateProgress(100, "Ready!");
                    Thread.sleep(200);

                    SwingUtilities.invokeLater(() -> {
                        splash.dispose();
                        IceClientLauncher launcher = new IceClientLauncher();
                        launcher.setVisible(true);
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        splash.dispose();
                        IceClientLauncher launcher = new IceClientLauncher();
                        launcher.setVisible(true);
                    });
                }
            }).start();
        });
    }
}