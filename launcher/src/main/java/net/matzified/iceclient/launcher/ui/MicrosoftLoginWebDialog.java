package net.matzified.iceclient.launcher.ui;

import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import net.matzified.iceclient.launcher.auth.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Official Microsoft sign-in dialog using an embedded JavaFX WebView.
 *
 * Flow:
 *  1. Opens a local HTTP server on a random port (loopback redirect URI).
 *  2. Builds the Microsoft OAuth2 / PKCE authorization URL.
 *  3. Loads the URL in a JavaFX WebView — user sees the REAL Microsoft login page.
 *  4. After the user signs in, Microsoft redirects to http://127.0.0.1:{port}/?code=...
 *  5. The local server captures the authorization code.
 *  6. Runs the full PKCE → Xbox Live → XSTS → Minecraft Services → Profile pipeline.
 *  7. Saves the session via AccountManager and calls onLoginComplete.
 */
public class MicrosoftLoginWebDialog extends JDialog {

    private final AccountManager accountManager = AccountManager.getInstance();
    private final Runnable onLoginComplete;

    private JFXPanel fxPanel;
    private JLabel   statusLabel;
    private JButton  cancelBtn;

    private HttpServer localServer;
    private CompletableFuture<String> codeFuture;

    // Prevent JavaFX Platform from shutting down on dialog close
    static { Platform.setImplicitExit(false); }

    public MicrosoftLoginWebDialog(Window owner, Runnable onLoginComplete) {
        super(owner, "Sign in with Microsoft — Ice Client", ModalityType.APPLICATION_MODAL);
        this.onLoginComplete = onLoginComplete;

        setSize(490, 660);
        setMinimumSize(new Dimension(420, 520));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // -------- Top Bar --------
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 7));
        topBar.setBackground(new Color(13, 15, 20));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(30, 35, 50)));

        JPanel msLogo = buildMicrosoftLogo(16);
        JLabel msLabel = new JLabel("Sign in with your Microsoft / Xbox account");
        msLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msLabel.setForeground(new Color(200, 210, 225));

        topBar.add(msLogo);
        topBar.add(msLabel);
        add(topBar, BorderLayout.NORTH);

        // -------- JavaFX WebView (center) --------
        // Creating JFXPanel on the EDT initializes the JavaFX toolkit
        fxPanel = new JFXPanel();
        fxPanel.setBackground(new Color(24, 28, 38));
        add(fxPanel, BorderLayout.CENTER);

        // -------- Bottom Status Bar --------
        JPanel bottomBar = new JPanel(new BorderLayout(8, 0));
        bottomBar.setBackground(new Color(13, 15, 20));
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 35, 50)),
                new EmptyBorder(7, 14, 7, 14)
        ));

        statusLabel = new JLabel("Loading Microsoft sign-in page...");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(new Color(148, 163, 184));

        cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cancelBtn.setBackground(new Color(40, 46, 60));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> closeAndCleanup());

        bottomBar.add(statusLabel, BorderLayout.CENTER);
        bottomBar.add(cancelBtn,   BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);

        // Cleanup when window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { closeAndCleanup(); }
        });

        // Start the OAuth flow in a background thread
        startOAuthFlow();
    }

    // =========================================================================
    //  OAuth Flow
    // =========================================================================

    private void startOAuthFlow() {
        codeFuture = new CompletableFuture<>();

        new Thread(() -> {
            try {
                // ---- 1. Start local loopback HTTP server on a random port ----
                localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                int    port        = localServer.getAddress().getPort();
                String redirectUri = "http://127.0.0.1:" + port;

                // ---- 2. Build PKCE auth URL ----
                PKCE pkce = new PKCE();

                Map<String, String> params = new LinkedHashMap<>();
                params.put("client_id",             MicrosoftPKCEAuthenticator.CLIENT_ID);
                params.put("response_type",          "code");
                params.put("redirect_uri",           redirectUri);
                params.put("scope",                  MicrosoftPKCEAuthenticator.SCOPE);
                params.put("code_challenge",         pkce.getCodeChallenge());
                params.put("code_challenge_method",  "S256");
                params.put("prompt",                 "select_account");

                StringBuilder authUrl = new StringBuilder(
                        "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?");
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (authUrl.charAt(authUrl.length() - 1) != '?') authUrl.append('&');
                    authUrl.append(java.net.URLEncoder.encode(entry.getKey(), "UTF-8"))
                           .append('=')
                           .append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"));
                }

                // ---- 3. Register redirect handler on local server ----
                AtomicBoolean captured = new AtomicBoolean(false);
                final CompletableFuture<String> codeRef = codeFuture;

                localServer.createContext("/", exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    Map<String, String> qp = parseQuery(query);

                    // Serve a success page regardless
                    String html = "<!DOCTYPE html><html><head><title>Ice Client</title>"
                        + "<style>body{background:#0d0f14;color:#f8fafc;font-family:Segoe UI,sans-serif;"
                        + "display:flex;align-items:center;justify-content:center;height:100vh;margin:0;}"
                        + ".c{background:#181c26;padding:36px 52px;border-radius:16px;border:1px solid #38bdf8;"
                        + "text-align:center;}h1{color:#38bdf8;}p{color:#94a3b8;}</style></head>"
                        + "<body><div class='c'><h1>✓ Sign-in Successful!</h1>"
                        + "<p>You can now close this window and return to Ice Client.</p></div></body></html>";
                    byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }

                    if (qp.containsKey("code") && captured.compareAndSet(false, true)) {
                        codeRef.complete(qp.get("code"));
                    } else if (qp.containsKey("error") && captured.compareAndSet(false, true)) {
                        codeRef.completeExceptionally(new RuntimeException(
                            "OAuth Error: " + qp.getOrDefault("error_description", qp.get("error"))));
                    }
                });

                localServer.start();

                // ---- 4. Load auth URL in JavaFX WebView ----
                final String finalAuthUrl = authUrl.toString();
                Platform.runLater(() -> {
                    WebView    webView = new WebView();
                    WebEngine  engine  = webView.getEngine();

                    engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
                        if (newLoc == null) return;
                        SwingUtilities.invokeLater(() -> {
                            if (newLoc.contains("login.microsoftonline.com") || newLoc.contains("login.live.com")) {
                                statusLabel.setText("Enter your email and password in the Microsoft login above");
                            } else if (newLoc.startsWith("http://127.0.0.1:")) {
                                statusLabel.setText("Authentication received — processing your account...");
                            }
                        });
                    });

                    engine.load(finalAuthUrl);
                    fxPanel.setScene(new Scene(new StackPane(webView)));
                });

                // ---- 5. Wait for auth code ----
                String code = codeFuture.get();
                stopServer();

                // ---- 6. Exchange code → Xbox Live → XSTS → Minecraft ----
                SwingUtilities.invokeLater(() -> statusLabel.setText("Authenticating with Xbox Live & Minecraft Services..."));

                MicrosoftPKCEAuthenticator auth = new MicrosoftPKCEAuthenticator();
                MinecraftSession session = auth.exchangeTokensAsync(
                        code,
                        pkce.getCodeVerifier(),
                        redirectUri,
                        msg -> SwingUtilities.invokeLater(() -> statusLabel.setText(msg))
                ).get();

                // ---- 7. Save account ----
                accountManager.addSession(session);

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Signed in as " + session.username() + "!");
                    // Blank out WebView before close
                    Platform.runLater(() -> {
                        if (fxPanel.getScene() != null) {
                            StackPane sp = (StackPane) fxPanel.getScene().getRoot();
                            sp.getChildren().stream()
                              .filter(n -> n instanceof WebView)
                              .map(n -> (WebView) n)
                              .forEach(wv -> wv.getEngine().load("about:blank"));
                        }
                    });
                    dispose();
                    if (onLoginComplete != null) onLoginComplete.run();
                });

            } catch (Exception ex) {
                stopServer();
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Sign-in failed: " + msg);
                    cancelBtn.setText("Close");
                });
                System.err.println("Microsoft login failed: " + msg);
            }
        }, "ms-auth-thread").start();
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private void closeAndCleanup() {
        stopServer();
        if (codeFuture != null && !codeFuture.isDone()) {
            codeFuture.completeExceptionally(new RuntimeException("User cancelled sign-in"));
        }
        Platform.runLater(() -> {
            if (fxPanel.getScene() != null) {
                StackPane sp = (StackPane) fxPanel.getScene().getRoot();
                sp.getChildren().stream()
                  .filter(n -> n instanceof WebView)
                  .map(n -> (WebView) n)
                  .forEach(wv -> wv.getEngine().load("about:blank"));
            }
        });
        dispose();
    }

    private void stopServer() {
        if (localServer != null) {
            try { localServer.stop(0); } catch (Exception ignored) {}
            localServer = null;
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                try {
                    map.put(java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private static JPanel buildMicrosoftLogo(int cellSize) {
        JPanel grid = new JPanel(new GridLayout(2, 2, 2, 2));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(cellSize, cellSize));
        Color[] colors = {new Color(242, 80, 34), new Color(127, 186, 0),
                          new Color(0, 164, 239),  new Color(255, 185, 0)};
        for (Color c : colors) {
            JPanel sq = new JPanel();
            sq.setBackground(c);
            grid.add(sq);
        }
        return grid;
    }
}
