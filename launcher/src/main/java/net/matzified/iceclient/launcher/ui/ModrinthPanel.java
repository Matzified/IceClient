package net.matzified.iceclient.launcher.ui;

import net.matzified.iceclient.launcher.model.Profile;
import net.matzified.iceclient.launcher.modrinth.ModrinthApiHandler;
import net.matzified.iceclient.launcher.modrinth.ModrinthApiHandler.ModResult;
import net.matzified.iceclient.launcher.modrinth.ModrinthApiHandler.ProjectType;
import net.matzified.iceclient.launcher.profile.ProfileManager;

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
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private JTextField searchField;
    private ProjectType currentType = ProjectType.MODS;
    private JPanel resultsPanel;
    private final Map<String, ImageIcon> iconCache = new HashMap<>();

    public ModrinthPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(11, 14, 20));
        setBorder(new EmptyBorder(20, 24, 20, 24));

        // 1. Top Header Bar (Branding, Target Profile Pill, Search Bar & Tabs)
        JPanel topHeader = new JPanel();
        topHeader.setLayout(new BoxLayout(topHeader, BoxLayout.Y_AXIS));
        topHeader.setOpaque(false);

        // Title Row
        JPanel titleRow = new JPanel(new BorderLayout(14, 0));
        titleRow.setOpaque(false);

        JPanel brandWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandWrapper.setOpaque(false);

        FrozenLabel titleLabel = new FrozenLabel("MOD STORE", 22);
        brandWrapper.add(titleLabel);

        JLabel subLabel = new JLabel("Powered by Modrinth • High-Performance Verified");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(new Color(100, 116, 139));
        brandWrapper.add(subLabel);

        titleRow.add(brandWrapper, BorderLayout.WEST);

        // Target Profile Badge
        Profile activeProfile = ProfileManager.getInstance().getActiveProfile();
        String profileName = activeProfile != null ? activeProfile.getName() : "Vanilla (1.21.1)";
        String mcVer = activeProfile != null ? activeProfile.getMcVersion() : "1.21.1";

        JLabel targetPill = new JLabel(" 🎯 Active: " + profileName + " (" + mcVer + ") ");
        targetPill.setFont(new Font("Segoe UI", Font.BOLD, 12));
        targetPill.setForeground(new Color(56, 189, 248));
        targetPill.setOpaque(true);
        targetPill.setBackground(new Color(18, 24, 38));
        targetPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 189, 248, 80), 1),
                new EmptyBorder(5, 12, 5, 12)
        ));

        titleRow.add(targetPill, BorderLayout.EAST);
        topHeader.add(titleRow);

        topHeader.add(Box.createRigidArea(new Dimension(0, 14)));

        // Navigation Tabs & Search Box
        JPanel navRow = new JPanel(new BorderLayout(14, 0));
        navRow.setOpaque(false);

        JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabsPanel.setOpaque(false);

        ProjectType[] types = {ProjectType.MODS, ProjectType.RESOURCEPACKS, ProjectType.DATAPACKS, ProjectType.SHADERS};

        for (ProjectType pt : types) {
            JButton tab = new JButton(pt.displayName) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean isSelected = (currentType == pt);
                    Color bg = isSelected ? new Color(2, 132, 199) : (getModel().isRollover() ? new Color(28, 34, 48) : new Color(18, 22, 32));
                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                    if (isSelected) {
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
            tab.setPreferredSize(new Dimension(130, 36));
            tab.setCursor(new Cursor(Cursor.HAND_CURSOR));
            tab.addActionListener(e -> {
                currentType = pt;
                repaint();
                performSearch(searchField.getText());
            });
            tabsPanel.add(tab);
        }
        navRow.add(tabsPanel, BorderLayout.WEST);

        // Search Field Box
        JPanel searchBox = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 24, 36));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(new Color(56, 189, 248, 80));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        searchBox.setOpaque(false);
        searchBox.setPreferredSize(new Dimension(300, 36));
        searchBox.setBorder(new EmptyBorder(0, 12, 0, 12));

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setForeground(new Color(148, 163, 184));
        searchBox.add(searchIcon, BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(new Color(56, 189, 248));
        searchField.setOpaque(false);
        searchField.setBorder(null);
        searchField.addActionListener(e -> performSearch(searchField.getText()));
        searchBox.add(searchField, BorderLayout.CENTER);

        navRow.add(searchBox, BorderLayout.EAST);
        topHeader.add(navRow);

        add(topHeader, BorderLayout.NORTH);

        // 2. Results Grid Panel
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new GridLayout(0, 2, 14, 14));
        resultsPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);

        add(scrollPane, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> performSearch(""));
    }

    private void performSearch(String query) {
        resultsPanel.removeAll();
        resultsPanel.setLayout(new BorderLayout());

        JLabel loadingLabel = new JLabel("Fetching compatible assets from Modrinth...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loadingLabel.setForeground(new Color(148, 163, 184));
        resultsPanel.add(loadingLabel, BorderLayout.CENTER);
        resultsPanel.revalidate();
        resultsPanel.repaint();

        new SwingWorker<List<ModResult>, Void>() {
            @Override
            protected List<ModResult> doInBackground() {
                Profile p = ProfileManager.getInstance().getActiveProfile();
                String ver = (p != null) ? p.getMcVersion() : "1.21.1";
                return ModrinthApiHandler.searchModrinth(query, ver, currentType);
            }

            @Override
            protected void done() {
                try {
                    List<ModResult> list = get();
                    resultsPanel.removeAll();
                    resultsPanel.setLayout(new GridLayout(0, 2, 14, 14));

                    if (list.isEmpty()) {
                        JLabel emptyLabel = new JLabel("No matching assets found for this category.", SwingConstants.CENTER);
                        emptyLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                        emptyLabel.setForeground(new Color(148, 163, 184));
                        resultsPanel.setLayout(new BorderLayout());
                        resultsPanel.add(emptyLabel, BorderLayout.CENTER);
                    } else {
                        for (ModResult item : list) {
                            resultsPanel.add(createModCard(item));
                        }
                    }
                    resultsPanel.revalidate();
                    resultsPanel.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private JPanel createModCard(ModResult item) {
        JPanel card = new JPanel(new BorderLayout(14, 0)) {
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
                g2.setColor(isHover ? new Color(24, 30, 44) : new Color(16, 20, 30));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(isHover ? new Color(56, 189, 248, 120) : new Color(255, 255, 255, 15));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.setPreferredSize(new Dimension(340, 96));

        // Left Icon
        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(52, 52));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadIconAsync(item.iconUrl, iconLabel);
        card.add(iconLabel, BorderLayout.WEST);

        // Center Details
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel titleLbl = new JLabel(item.title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(Color.WHITE);
        center.add(titleLbl);

        center.add(Box.createRigidArea(new Dimension(0, 3)));

        String desc = item.description.length() > 62 ? item.description.substring(0, 59) + "..." : item.description;
        JLabel descLbl = new JLabel(desc.isEmpty() ? "Verified high-performance mod." : desc);
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLbl.setForeground(new Color(148, 163, 184));
        center.add(descLbl);

        center.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel metaLbl = new JLabel("by " + item.author + " • ⬇ " + item.downloads);
        metaLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        metaLbl.setForeground(new Color(56, 189, 248));
        center.add(metaLbl);

        card.add(center, BorderLayout.CENTER);

        // Right Install Button
        JButton installBtn = new JButton(item.isInstalled ? "✓ Installed" : "📥 Install") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = item.isInstalled ? new Color(16, 185, 129) : (getModel().isRollover() ? new Color(2, 132, 199) : new Color(3, 105, 161));
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        installBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        installBtn.setForeground(Color.WHITE);
        installBtn.setFocusPainted(false);
        installBtn.setBorderPainted(false);
        installBtn.setContentAreaFilled(false);
        installBtn.setPreferredSize(new Dimension(94, 32));
        installBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        installBtn.addActionListener(e -> {
            if (item.isInstalled) return;
            installBtn.setText("⏳ ...");
            installBtn.setEnabled(false);

            Profile p = ProfileManager.getInstance().getActiveProfile();
            String ver = p != null ? p.getMcVersion() : "1.21.1";

            ModrinthApiHandler.installModAsync(item, ver, success -> SwingUtilities.invokeLater(() -> {
                if (success) {
                    installBtn.setText("✓ Installed");
                    installBtn.repaint();
                } else {
                    installBtn.setText("❌ Failed");
                    installBtn.setEnabled(true);
                }
            }));
        });

        JPanel rightWrapper = new JPanel(new GridBagLayout());
        rightWrapper.setOpaque(false);
        rightWrapper.add(installBtn);
        card.add(rightWrapper, BorderLayout.EAST);

        return card;
    }

    private void loadIconAsync(String urlStr, JLabel label) {
        if (urlStr == null || urlStr.isEmpty()) {
            label.setIcon(createDefaultThumbnail());
            return;
        }

        if (iconCache.containsKey(urlStr)) {
            label.setIcon(iconCache.get(urlStr));
            return;
        }

        label.setIcon(createDefaultThumbnail());

        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(urlStr))
                        .timeout(Duration.ofSeconds(6))
                        .header("User-Agent", "IceClientLauncher/1.0.0")
                        .GET()
                        .build();

                HttpResponse<byte[]> res = ICON_HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (res.statusCode() == 200) {
                    BufferedImage raw = ImageIO.read(new ByteArrayInputStream(res.body()));
                    if (raw != null) {
                        Image scaled = raw.getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                        ImageIcon icon = new ImageIcon(scaled);
                        iconCache.put(urlStr, icon);
                        SwingUtilities.invokeLater(() -> label.setIcon(icon));
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private ImageIcon createDefaultThumbnail() {
        BufferedImage img = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(24, 32, 48));
        g2.fillRoundRect(0, 0, 48, 48, 10, 10);
        g2.setColor(new Color(56, 189, 248));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("📦", (48 - fm.stringWidth("📦")) / 2, 30);
        g2.dispose();
        return new ImageIcon(img);
    }
}