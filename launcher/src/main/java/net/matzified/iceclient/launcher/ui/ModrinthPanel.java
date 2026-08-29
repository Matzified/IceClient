package net.matzified.iceclient.launcher.ui;

import net.matzified.iceclient.launcher.modrinth.ModrinthApiHandler;
import net.matzified.iceclient.launcher.modrinth.ModrinthApiHandler.ModResult;
import net.matzified.iceclient.launcher.modrinth.ModrinthApiHandler.ProjectType;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModrinthPanel extends JPanel {

    private static final HttpClient ICON_HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private JTextField searchField;
    private ProjectType currentType = ProjectType.MODS;
    private JPanel resultsPanel;
    private final Map<String, ImageIcon> iconCache = new HashMap<>();

    private JButton[] filterTabs;

    public ModrinthPanel() {
        setLayout(new BorderLayout(18, 18));
        setBackground(new Color(13, 15, 20));
        setBorder(new EmptyBorder(22, 28, 22, 28));

        JPanel topHeader = new JPanel();
        topHeader.setLayout(new BoxLayout(topHeader, BoxLayout.Y_AXIS));
        topHeader.setOpaque(false);

        // Title & Target Profile Row
        JPanel titleRow = new JPanel(new BorderLayout(14, 0));
        titleRow.setOpaque(false);

        FrozenLabel titleLabel = new FrozenLabel("MOD STORE", 22);

        // Target Profile Indicator (No missing glyph boxes!)
        JLabel targetPill = new JLabel(" Target Profile: 1.21.1-Default ");
        targetPill.setFont(new Font("Segoe UI", Font.BOLD, 12));
        targetPill.setForeground(new Color(56, 189, 248));
        targetPill.setOpaque(true);
        targetPill.setBackground(new Color(24, 32, 48));
        targetPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248, 100), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));

        titleRow.add(titleLabel, BorderLayout.WEST);
        titleRow.add(targetPill, BorderLayout.EAST);
        topHeader.add(titleRow);

        topHeader.add(Box.createRigidArea(new Dimension(0, 16)));

        // Filter Tabs & Search Bar Container
        JPanel filterRow = new JPanel(new BorderLayout(14, 0));
        filterRow.setOpaque(false);

        // Filter Toggle Tabs: [Mods] [Resource Packs] [Data Packs] [Shaders]
        JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tabsPanel.setOpaque(false);

        ProjectType[] types = {ProjectType.MODS, ProjectType.RESOURCEPACKS, ProjectType.DATAPACKS, ProjectType.SHADERS};
        filterTabs = new JButton[types.length];

        for (int i = 0; i < types.length; i++) {
            final ProjectType pt = types[i];
            final int index = i;
            JButton tab = new JButton(pt.displayName) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean isSelected = (currentType == pt);
                    Color bg = isSelected ? new Color(14, 165, 233) : (getModel().isRollover() ? new Color(34, 40, 54) : new Color(22, 26, 36));
                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            tab.setFont(new Font("Segoe UI", Font.BOLD, 13));
            tab.setForeground(Color.WHITE);
            tab.setFocusPainted(false);
            tab.setContentAreaFilled(false);
            tab.setBorder(new EmptyBorder(8, 16, 8, 16));
            tab.setCursor(new Cursor(Cursor.HAND_CURSOR));

            tab.addActionListener(e -> {
                currentType = pt;
                updateTabStyles();
                performSearch();
            });

            filterTabs[i] = tab;
            tabsPanel.add(tab);
        }

        // Search Input Field
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBackground(new Color(22, 26, 36));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248, 120), 1),
                new EmptyBorder(0, 14, 0, 14)
        ));
        searchField.setToolTipText("Search mods, shaders, resource packs...");
        searchField.addActionListener(e -> performSearch());

        JButton searchButton = new JButton("Search") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isRollover() ? new Color(56, 189, 248) : new Color(14, 165, 233);
                Color bottom = getModel().isRollover() ? new Color(2, 132, 199) : new Color(3, 105, 161);
                GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setContentAreaFilled(false);
        searchButton.setBorderPainted(false);
        searchButton.setPreferredSize(new Dimension(105, 38));
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchButton.addActionListener(e -> performSearch());

        JPanel searchGroup = new JPanel(new BorderLayout(8, 0));
        searchGroup.setOpaque(false);
        searchGroup.add(searchField, BorderLayout.CENTER);
        searchGroup.add(searchButton, BorderLayout.EAST);

        filterRow.add(tabsPanel, BorderLayout.WEST);
        filterRow.add(searchGroup, BorderLayout.CENTER);

        topHeader.add(filterRow);
        add(topHeader, BorderLayout.NORTH);

        // Content Area (Results + Right Category Sidebar)
        JPanel contentPanel = new JPanel(new BorderLayout(18, 0));
        contentPanel.setOpaque(false);

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(new Color(13, 15, 20));

        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(13, 15, 20));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel sidebar = createCategorySidebar();
        contentPanel.add(sidebar, BorderLayout.EAST);

        add(contentPanel, BorderLayout.CENTER);

        performSearch();
    }

    private void updateTabStyles() {
        for (JButton b : filterTabs) {
            b.repaint();
        }
    }

    private JPanel createCategorySidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(22, 26, 36));
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(42, 50, 68), 1),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JLabel catHeader = new JLabel("CATEGORIES");
        catHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        catHeader.setForeground(new Color(148, 163, 184));
        sidebar.add(catHeader);

        sidebar.add(Box.createRigidArea(new Dimension(0, 12)));

        String[] tags = {"Optimization", "Combat", "Adventure", "Utility", "Shaders", "Library"};
        for (String tag : tags) {
            JButton tagBtn = new JButton(tag);
            tagBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            tagBtn.setBackground(new Color(32, 38, 52));
            tagBtn.setForeground(Color.WHITE);
            tagBtn.setFocusPainted(false);
            tagBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            tagBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            tagBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            tagBtn.addActionListener(e -> {
                searchField.setText(tag);
                performSearch();
            });

            sidebar.add(tagBtn);
            sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
        }

        sidebar.add(Box.createRigidArea(new Dimension(0, 18)));
        JLabel loaderHeader = new JLabel("MOD LOADER");
        loaderHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        loaderHeader.setForeground(new Color(148, 163, 184));
        sidebar.add(loaderHeader);

        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel fabricBadge = new JLabel(" Fabric (1.21 - 1.21.11) ");
        fabricBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        fabricBadge.setForeground(new Color(56, 189, 248));
        fabricBadge.setOpaque(true);
        fabricBadge.setBackground(new Color(15, 30, 55));
        fabricBadge.setBorder(new EmptyBorder(6, 10, 6, 10));
        sidebar.add(fabricBadge);

        return sidebar;
    }

    private void performSearch() {
        String query = searchField.getText().trim();

        resultsPanel.removeAll();
        JLabel loadingLabel = new JLabel("Fetching " + currentType.displayName + " from Modrinth...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        loadingLabel.setForeground(new Color(148, 163, 184));
        resultsPanel.add(loadingLabel);
        resultsPanel.revalidate();
        resultsPanel.repaint();

        new Thread(() -> {
            List<ModResult> items = ModrinthApiHandler.searchModrinth(query, "1.21.1", currentType);
            SwingUtilities.invokeLater(() -> {
                resultsPanel.removeAll();
                if (items.isEmpty()) {
                    JLabel emptyLabel = new JLabel("No " + currentType.displayName + " found.", SwingConstants.CENTER);
                    emptyLabel.setForeground(new Color(148, 163, 184));
                    resultsPanel.add(emptyLabel);
                } else {
                    for (ModResult item : items) {
                        resultsPanel.add(createLunarModCard(item));
                        resultsPanel.add(Box.createRigidArea(new Dimension(0, 12)));
                    }
                }
                resultsPanel.revalidate();
                resultsPanel.repaint();
            });
        }).start();
    }

    private JPanel createLunarModCard(ModResult item) {
        boolean[] isHover = {false};
        JPanel card = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = isHover[0] ? new Color(28, 34, 48) : new Color(20, 24, 34);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));

                g2.setColor(isHover[0] ? new Color(56, 189, 248, 120) : new Color(42, 50, 68));
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 18, 14, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 94));
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

        // 56x56 Mod Icon Container — show fallback immediately, replace async
        JLabel iconContainer = new JLabel(createFallbackModIcon(item.title));
        iconContainer.setPreferredSize(new Dimension(56, 56));
        iconContainer.setHorizontalAlignment(SwingConstants.CENTER);

        if (item.iconUrl != null && !item.iconUrl.isEmpty()) {
            loadAsyncIcon(item.iconUrl, iconContainer);
        }

        // Center Info: Title, Author, Description
        JPanel centerInfo = new JPanel(new GridLayout(2, 1, 0, 4));
        centerInfo.setOpaque(false);

        JLabel titleLbl = new JLabel(item.title + "  by " + item.author);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(Color.WHITE);

        JLabel descLbl = new JLabel(item.description);
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setForeground(new Color(148, 163, 184));

        centerInfo.add(titleLbl);
        centerInfo.add(descLbl);

        // Right Action Button (Installed vs Install)
        boolean isPreinstalled = isModInstalled(item.title);
        JButton installBtn = new JButton(isPreinstalled ? "✓ Installed" : "⬇ Install") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                if (getText().contains("Installed")) {
                    g2.setColor(new Color(16, 185, 129));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                } else if (getText().contains("Downloading")) {
                    g2.setColor(new Color(55, 65, 81));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                } else {
                    Color top = getModel().isRollover() ? new Color(56, 189, 248) : new Color(14, 165, 233);
                    Color bottom = getModel().isRollover() ? new Color(2, 132, 199) : new Color(3, 105, 161);
                    g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                }

                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };
        installBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        installBtn.setForeground(Color.WHITE);
        installBtn.setFocusPainted(false);
        installBtn.setContentAreaFilled(false);
        installBtn.setBorderPainted(false);
        installBtn.setPreferredSize(new Dimension(130, 38));
        installBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        installBtn.addActionListener(e -> {
            if (installBtn.getText().contains("Installed")) return;
            installBtn.setEnabled(false);
            installBtn.setText("Downloading...");

            new Thread(() -> {
                boolean success = ModrinthApiHandler.installModrinthAsset(item, "1.21.1", null);
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        installBtn.setText("✓ Installed");
                    } else {
                        installBtn.setText("⬇ Install");
                        installBtn.setEnabled(true);
                    }
                });
            }).start();
        });

        card.add(iconContainer, BorderLayout.WEST);
        card.add(centerInfo, BorderLayout.CENTER);
        card.add(installBtn, BorderLayout.EAST);
        return card;
    }

    private boolean isModInstalled(String title) {
        String lower = title.toLowerCase();
        return lower.contains("sodium") || lower.contains("fabric api") || lower.contains("iris") || lower.contains("cloth config") || lower.contains("entity culling") || lower.contains("ferritecore");
    }

    private void loadAsyncIcon(String iconUrl, JLabel targetLabel) {
        ImageIcon cached = iconCache.get(iconUrl);
        if (cached != null) {
            targetLabel.setIcon(cached);
            return;
        }

        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(iconUrl))
                        .header("User-Agent", "IceClientLauncher/1.0.0 (Java)")
                        .GET()
                        .build();
                byte[] data = ICON_HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();
                if (data != null && data.length > 0) {
                    BufferedImage raw = ImageIO.read(new ByteArrayInputStream(data));
                    if (raw != null) {
                        ImageIcon icon = new ImageIcon(createRoundedIcon(raw, 52, 52, 10));
                        iconCache.put(iconUrl, icon);
                        SwingUtilities.invokeLater(() -> {
                            targetLabel.setIcon(icon);
                            targetLabel.repaint();
                        });
                    }
                }
            } catch (Exception ignored) { /* keep fallback icon */ }
        }, "modrinth-icon-loader").start();
    }

    private static BufferedImage createRoundedIcon(BufferedImage src, int w, int h, int radius) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2  = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return out;
    }

    private ImageIcon createFallbackModIcon(String name) {
        BufferedImage img = new BufferedImage(52, 52, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(30, 38, 54));
        g2.fill(new RoundRectangle2D.Float(0, 0, 52, 52, 12, 12));
        g2.setColor(new Color(56, 189, 248));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));

        String letter = name.isEmpty() ? "M" : String.valueOf(name.charAt(0)).toUpperCase();
        FontMetrics fm = g2.getFontMetrics();
        int x = (52 - fm.stringWidth(letter)) / 2;
        int y = ((52 - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(letter, x, y);
        g2.dispose();

        return new ImageIcon(img);
    }
}