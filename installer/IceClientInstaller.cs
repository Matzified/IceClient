using System;
using System.Drawing;
using System.IO;
using System.Reflection;
using System.Windows.Forms;

namespace IceClientInstaller
{
    public class InstallerForm : Form
    {
        private TextBox txtInstallPath;
        private Button btnBrowse;
        private Button btnInstall;
        private ProgressBar progressBar;
        private Label lblStatus;
        private CheckBox chkDesktopShortcut;
        private CheckBox chkStartMenuShortcut;
        private CheckBox chkLaunchAfter;
        private PictureBox picLogo;

        public InstallerForm()
        {
            InitializeComponent();
        }

        private void InitializeComponent()
        {
            this.Text = "🧊 ICE CLIENT SETUP — Minecraft 1.21 - 1.21.11";
            this.Size = new Size(640, 500);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.FormBorderStyle = FormBorderStyle.FixedSingle;
            this.MaximizeBox = false;
            this.BackColor = Color.FromArgb(10, 15, 29);

            // Banner Header
            Panel headerPanel = new Panel();
            headerPanel.Dock = DockStyle.Top;
            headerPanel.Height = 90;
            headerPanel.BackColor = Color.FromArgb(13, 19, 36);

            picLogo = new PictureBox();
            picLogo.Size = new Size(60, 60);
            picLogo.Location = new Point(20, 15);
            picLogo.SizeMode = PictureBoxSizeMode.Zoom;

            string baseDir = AppDomain.CurrentDomain.BaseDirectory;
            string logoPath = Path.Combine(baseDir, "logo.png");
            if (!File.Exists(logoPath)) logoPath = Path.Combine(baseDir, "logo.jpg");
            if (File.Exists(logoPath))
            {
                try { picLogo.Image = Image.FromFile(logoPath); } catch {}
            }
            headerPanel.Controls.Add(picLogo);

            Label lblTitle = new Label();
            lblTitle.Text = "❄️ ICE CLIENT SETUP";
            lblTitle.Font = new Font("Segoe UI", 16, FontStyle.Bold);
            lblTitle.ForeColor = Color.FromArgb(56, 189, 248);
            lblTitle.Location = new Point(90, 18);
            lblTitle.AutoSize = true;

            Label lblSubtitle = new Label();
            lblSubtitle.Text = "Minecraft 1.21 - 1.21.11 Fabric FPS Boost · Isolated .iceclient Storage";
            lblSubtitle.Font = new Font("Segoe UI", 9, FontStyle.Regular);
            lblSubtitle.ForeColor = Color.FromArgb(148, 163, 184);
            lblSubtitle.Location = new Point(92, 48);
            lblSubtitle.AutoSize = true;

            headerPanel.Controls.Add(lblTitle);
            headerPanel.Controls.Add(lblSubtitle);
            this.Controls.Add(headerPanel);

            // Path Selector
            Label lblPath = new Label();
            lblPath.Text = "STORAGE DIRECTORY (.iceclient):";
            lblPath.Font = new Font("Segoe UI", 10, FontStyle.Bold);
            lblPath.ForeColor = Color.FromArgb(148, 163, 184);
            lblPath.Location = new Point(30, 110);
            lblPath.AutoSize = true;
            this.Controls.Add(lblPath);

            string defaultPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".iceclient");

            txtInstallPath = new TextBox();
            txtInstallPath.Text = defaultPath;
            txtInstallPath.Font = new Font("Segoe UI", 10);
            txtInstallPath.BackColor = Color.FromArgb(17, 24, 39);
            txtInstallPath.ForeColor = Color.White;
            txtInstallPath.Location = new Point(30, 135);
            txtInstallPath.Size = new Size(450, 30);
            this.Controls.Add(txtInstallPath);

            btnBrowse = new Button();
            btnBrowse.Text = "Browse...";
            btnBrowse.Font = new Font("Segoe UI", 9, FontStyle.Bold);
            btnBrowse.BackColor = Color.FromArgb(2, 132, 199);
            btnBrowse.ForeColor = Color.White;
            btnBrowse.FlatStyle = FlatStyle.Flat;
            btnBrowse.Location = new Point(490, 134);
            btnBrowse.Size = new Size(100, 30);
            btnBrowse.Click += BtnBrowse_Click;
            this.Controls.Add(btnBrowse);

            // Options Checkboxes
            chkDesktopShortcut = new CheckBox();
            chkDesktopShortcut.Text = "Create Desktop Shortcut (Ice Client)";
            chkDesktopShortcut.Checked = true;
            chkDesktopShortcut.Font = new Font("Segoe UI", 10);
            chkDesktopShortcut.ForeColor = Color.White;
            chkDesktopShortcut.Location = new Point(35, 185);
            chkDesktopShortcut.AutoSize = true;
            this.Controls.Add(chkDesktopShortcut);

            chkStartMenuShortcut = new CheckBox();
            chkStartMenuShortcut.Text = "Create Start Menu Shortcut";
            chkStartMenuShortcut.Checked = true;
            chkStartMenuShortcut.Font = new Font("Segoe UI", 10);
            chkStartMenuShortcut.ForeColor = Color.White;
            chkStartMenuShortcut.Location = new Point(35, 215);
            chkStartMenuShortcut.AutoSize = true;
            this.Controls.Add(chkStartMenuShortcut);

            chkLaunchAfter = new CheckBox();
            chkLaunchAfter.Text = "Launch Ice Client Launcher when setup finishes";
            chkLaunchAfter.Checked = true;
            chkLaunchAfter.Font = new Font("Segoe UI", 10);
            chkLaunchAfter.ForeColor = Color.White;
            chkLaunchAfter.Location = new Point(35, 245);
            chkLaunchAfter.AutoSize = true;
            this.Controls.Add(chkLaunchAfter);

            // Status & Progress Bar
            lblStatus = new Label();
            lblStatus.Text = "Ready to install Ice Client.";
            lblStatus.Font = new Font("Segoe UI", 9, FontStyle.Italic);
            lblStatus.ForeColor = Color.FromArgb(56, 189, 248);
            lblStatus.Location = new Point(30, 300);
            lblStatus.AutoSize = true;
            this.Controls.Add(lblStatus);

            progressBar = new ProgressBar();
            progressBar.Location = new Point(30, 325);
            progressBar.Size = new Size(560, 25);
            this.Controls.Add(progressBar);

            // Install Button
            btnInstall = new Button();
            btnInstall.Text = "▶ INSTALL NOW";
            btnInstall.Font = new Font("Segoe UI", 12, FontStyle.Bold);
            btnInstall.BackColor = Color.FromArgb(16, 185, 129);
            btnInstall.ForeColor = Color.White;
            btnInstall.FlatStyle = FlatStyle.Flat;
            btnInstall.Location = new Point(210, 375);
            btnInstall.Size = new Size(220, 48);
            btnInstall.Click += BtnInstall_Click;
            this.Controls.Add(btnInstall);
        }

        private void BtnBrowse_Click(object sender, EventArgs e)
        {
            using (FolderBrowserDialog dlg = new FolderBrowserDialog())
            {
                dlg.Description = "Select Ice Client (.iceclient) storage directory";
                if (dlg.ShowDialog() == DialogResult.OK)
                {
                    txtInstallPath.Text = dlg.SelectedPath;
                }
            }
        }

        private void BtnInstall_Click(object sender, EventArgs e)
        {
            btnInstall.Enabled = false;
            btnBrowse.Enabled = false;
            txtInstallPath.Enabled = false;

            string targetDir = txtInstallPath.Text.Trim();
            if (string.IsNullOrEmpty(targetDir))
            {
                targetDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".iceclient");
            }

            try
            {
                lblStatus.Text = "Creating .iceclient directory tree...";
                progressBar.Value = 20;
                Application.DoEvents();

                Directory.CreateDirectory(targetDir);
                Directory.CreateDirectory(Path.Combine(targetDir, "mods"));
                Directory.CreateDirectory(Path.Combine(targetDir, "profiles"));
                Directory.CreateDirectory(Path.Combine(targetDir, "versions"));
                Directory.CreateDirectory(Path.Combine(targetDir, "config"));

                lblStatus.Text = "Extracting Launcher and Mod binaries...";
                progressBar.Value = 50;
                Application.DoEvents();

                string baseDir = AppDomain.CurrentDomain.BaseDirectory;
                string launcherJarSource = Path.Combine(baseDir, "IceClientLauncher.jar");
                string modJarSource = Path.Combine(baseDir, "IceClient-1.0.0.jar");

                string launcherJarDest = Path.Combine(targetDir, "IceClientLauncher.jar");
                if (File.Exists(launcherJarSource))
                {
                    File.Copy(launcherJarSource, launcherJarDest, true);
                }

                string defaultProfileDir = Path.Combine(targetDir, "profiles", "1.21.1-Default");
                Directory.CreateDirectory(defaultProfileDir);
                Directory.CreateDirectory(Path.Combine(defaultProfileDir, "mods"));

                if (File.Exists(modJarSource))
                {
                    File.Copy(modJarSource, Path.Combine(targetDir, "mods", "IceClient-1.0.0.jar"), true);
                    File.Copy(modJarSource, Path.Combine(defaultProfileDir, "mods", "IceClient-1.0.0.jar"), true);
                }

                string exeWrapperPath = Path.Combine(targetDir, "IceClientLauncher.bat");
                string cmdContent = "@echo off\r\nstart \"Ice Client\" javaw -jar \"" + launcherJarDest + "\" %*\r\n";
                File.WriteAllText(exeWrapperPath, cmdContent);

                string icoSource = Path.Combine(baseDir, "logo.ico");
                string icoDest = Path.Combine(targetDir, "logo.ico");
                if (File.Exists(icoSource))
                {
                    File.Copy(icoSource, icoDest, true);
                }

                lblStatus.Text = "Creating shortcuts...";
                progressBar.Value = 80;
                Application.DoEvents();

                if (chkDesktopShortcut.Checked)
                {
                    string desktopPath = Environment.GetFolderPath(Environment.SpecialFolder.Desktop);
                    CreateShortcut(Path.Combine(desktopPath, "Ice Client.lnk"), exeWrapperPath, targetDir, icoDest);
                }

                if (chkStartMenuShortcut.Checked)
                {
                    string startMenuPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.StartMenu), "Programs");
                    CreateShortcut(Path.Combine(startMenuPath, "Ice Client.lnk"), exeWrapperPath, targetDir, icoDest);
                }

                progressBar.Value = 100;
                lblStatus.Text = "✓ Installation Complete!";

                MessageBox.Show("Ice Client has been successfully installed to:\n" + targetDir + "\n\nAll profiles and mods will be stored in this directory.", "Installation Complete", MessageBoxButtons.OK, MessageBoxIcon.Information);

                if (chkLaunchAfter.Checked && File.Exists(exeWrapperPath))
                {
                    System.Diagnostics.Process.Start(exeWrapperPath);
                }

                this.Close();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Installation failed: " + ex.Message, "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                btnInstall.Enabled = true;
            }
        }

        private void CreateShortcut(string shortcutPath, string targetPath, string workingDir, string iconPath)
        {
            try
            {
                Type shellType = Type.GetTypeFromProgID("WScript.Shell");
                dynamic shell = Activator.CreateInstance(shellType);
                dynamic shortcut = shell.CreateShortcut(shortcutPath);
                shortcut.TargetPath = targetPath;
                shortcut.WorkingDirectory = workingDir;
                shortcut.Description = "Ice Client Minecraft 1.21-1.21.11 Fabric Launcher";
                if (!string.IsNullOrEmpty(iconPath) && File.Exists(iconPath))
                {
                    shortcut.IconLocation = iconPath + ",0";
                }
                shortcut.Save();
            }
            catch { }
        }

        [STAThread]
        public static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new InstallerForm());
        }
    }
}
