package net.matzified.iceclient.launcher.ui;

import net.matzified.iceclient.launcher.auth.Account;
import net.matzified.iceclient.launcher.auth.AccountManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A floating popup anchored below the player badge in the title bar.
 * Lists all saved accounts and lets the user one-click switch or add a new one.
 */
public class AccountSwitcherPopup extends JWindow {

    private final AccountManager accountManager = AccountManager.getInstance();
    private final Runnable onAccountChanged;
    private final JPanel contentPanel;

    private static final Color BG           = new Color(20, 24, 34);
    private static final Color BORDER_COLOR = new Color(56, 189, 248, 110);
    private static final Color ACTIVE_BG    = new Color(14, 165, 233, 35);
    private static final Color HOVER_BG     = new Color(255, 255, 255, 12);
    private static final Color TEXT_ACTIVE  = new Color(56, 189, 248);
    private static final Color TEXT_DIM     = new Color(100, 116, 139);

    /** Lazily-registered global click-outside listener. */
    private final AWTEventListener dismissListener = event -> {
        if (event instanceof MouseEvent me && me.getID() == MouseEvent.MOUSE_PRESSED && isVisible()) {
            if (!getBounds().contains(me.getLocationOnScreen())) {
                dismiss();
            }
        }
    };

    public AccountSwitcherPopup(Window owner, Runnable onAccountChanged) {
        super(owner);
        this.onAccountChanged = onAccountChanged;

        setLayout(new BorderLayout());

        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(10, 12, 10, 12));

        add(contentPanel, BorderLayout.CENTER);
    }

    // =========================================================================
    //  Show / Hide
    // =========================================================================

    public void toggle(Component anchor) {
        if (isVisible()) {
            dismiss();
        } else {
            showBelow(anchor);
        }
    }

    public void showBelow(Component anchor) {
        rebuildContent();
        setMinimumSize(new Dimension(240, 10));
        pack();

        Point loc = anchor.getLocationOnScreen();
        int x = loc.x + anchor.getWidth() - getWidth();
        int y = loc.y + anchor.getHeight() + 5;
        setLocation(x, y);

        Toolkit.getDefaultToolkit().addAWTEventListener(dismissListener, AWTEvent.MOUSE_EVENT_MASK);
        setVisible(true);
        toFront();
    }

    public void dismiss() {
        setVisible(false);
        Toolkit.getDefaultToolkit().removeAWTEventListener(dismissListener);
    }

    // =========================================================================
    //  Content Builder
    // =========================================================================

    private void rebuildContent() {
        contentPanel.removeAll();

        // Section header
        JLabel header = new JLabel("ACCOUNTS");
        header.setFont(new Font("Segoe UI", Font.BOLD, 10));
        header.setForeground(TEXT_DIM);
        header.setAlignmentX(LEFT_ALIGNMENT);
        contentPanel.add(header);
        contentPanel.add(gap(6));

        List<Account> accounts = accountManager.getAccounts();
        Account active = accountManager.getActiveAccount();

        for (Account acc : accounts) {
            boolean isActive = active != null && active.getId().equals(acc.getId());
            contentPanel.add(createAccountRow(acc, isActive));
            contentPanel.add(gap(3));
        }

        if (accounts.isEmpty()) {
            JLabel empty = new JLabel("No accounts signed in yet");
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            empty.setForeground(TEXT_DIM);
            empty.setAlignmentX(LEFT_ALIGNMENT);
            contentPanel.add(empty);
            contentPanel.add(gap(4));
        }

        // Separator
        contentPanel.add(gap(4));
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(40, 48, 64));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        contentPanel.add(sep);
        contentPanel.add(gap(8));

        // Add account button
        JLabel addBtn = new JLabel("＋  Add Microsoft Account");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setForeground(TEXT_ACTIVE);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.setAlignmentX(LEFT_ALIGNMENT);
        addBtn.setBorder(new EmptyBorder(2, 2, 2, 2));
        addBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dismiss();
                Window owner = (Window) getOwner();
                MicrosoftLoginWebDialog dlg = new MicrosoftLoginWebDialog(owner, () -> {
                    rebuildContent();
                    if (onAccountChanged != null) onAccountChanged.run();
                });
                dlg.setVisible(true);
            }
            @Override
            public void mouseEntered(MouseEvent e) { addBtn.setForeground(Color.WHITE); }
            @Override
            public void mouseExited(MouseEvent e)  { addBtn.setForeground(TEXT_ACTIVE); }
        });
        contentPanel.add(addBtn);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // =========================================================================
    //  Account Row
    // =========================================================================

    private JPanel createAccountRow(Account acc, boolean isActive) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            private boolean hovered = false;

            {
                if (!isActive) {
                    addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                        @Override
                        public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            accountManager.setActiveAccount(acc);
                            dismiss();
                            if (onAccountChanged != null) onAccountChanged.run();
                        }
                    });
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isActive) {
                    g2.setColor(ACTIVE_BG);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                } else if (hovered) {
                    g2.setColor(HOVER_BG);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 6, 5, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        row.setPreferredSize(new Dimension(232, 46));
        row.setAlignmentX(LEFT_ALIGNMENT);
        if (!isActive) row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Avatar
        row.add(new JLabel(createAvatarIcon(acc.getUsername(), 30)), BorderLayout.WEST);

        // Name + status
        JPanel info = new JPanel(new GridLayout(2, 1, 0, 1));
        info.setOpaque(false);

        JLabel name = new JLabel(acc.getUsername());
        name.setFont(new Font("Segoe UI", Font.BOLD, 13));
        name.setForeground(isActive ? TEXT_ACTIVE : Color.WHITE);

        JLabel status = new JLabel(isActive ? "● Active" : "Click to switch");
        status.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        status.setForeground(isActive ? new Color(16, 185, 129) : TEXT_DIM);

        info.add(name);
        info.add(status);
        row.add(info, BorderLayout.CENTER);

        // Remove account button (X) on hover — small, top-right
        if (!isActive) {
            JLabel removeBtn = new JLabel("✕");
            removeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            removeBtn.setForeground(TEXT_DIM);
            removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            removeBtn.setToolTipText("Remove account");
            removeBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    e.consume();
                    accountManager.removeAccount(acc.getId());
                    rebuildContent();
                    pack();
                    if (onAccountChanged != null) onAccountChanged.run();
                }
                @Override
                public void mouseEntered(MouseEvent e) { removeBtn.setForeground(new Color(248, 113, 113)); }
                @Override
                public void mouseExited(MouseEvent e)  { removeBtn.setForeground(TEXT_DIM); }
            });
            row.add(removeBtn, BorderLayout.EAST);
        }

        return row;
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private static ImageIcon createAvatarIcon(String username, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(24, 32, 50));
        g2.fill(new RoundRectangle2D.Float(0, 0, size, size, 7, 7));
        g2.setColor(new Color(56, 189, 248));
        g2.setFont(new Font("Segoe UI", Font.BOLD, (int)(size * 0.5)));
        String letter = (username == null || username.isEmpty()) ? "?" : String.valueOf(username.charAt(0)).toUpperCase();
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(letter, (size - fm.stringWidth(letter)) / 2, (size + fm.getAscent() - fm.getDescent()) / 2);
        g2.dispose();
        return new ImageIcon(img);
    }

    private static Component gap(int h) { return Box.createRigidArea(new Dimension(0, h)); }
}
