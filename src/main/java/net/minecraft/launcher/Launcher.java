package net.minecraft.launcher;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.authentication.exceptions.AuthenticationException;
import net.minecraft.launcher.authentication.exceptions.InvalidCredentialsException;
import net.minecraft.launcher.authentication.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.ui.LauncherPanel;
import net.minecraft.launcher.ui.popups.login.LogInPopup;
import net.minecraft.launcher.updater.LocalVersionList;
import net.minecraft.launcher.updater.RemoteVersionList;
import net.minecraft.launcher.updater.VersionManager;
import net.minecraft.launcher.updater.download.DownloadJob;

public class Launcher {
    private static Launcher instance;
    private static final List<String> delayedSysout = new ArrayList<String>();

    public static Launcher getInstance() {
        return instance;
    }

    private static void setLookAndFeel() {
        final JFrame frame = new JFrame();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(final Throwable ignored) {
            try {
                getInstance().println("Your java failed to provide normal look and feel, trying the old fallback now");
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
            catch(final Throwable t) {
                getInstance().println("Unexpected exception setting look and feel");
                t.printStackTrace();
            }
        }
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("test"));
        frame.add(panel);
        try {
            frame.pack();
        }
        catch(final Throwable t) {
            getInstance().println("Custom (broken) theme detected, falling back onto x-platform theme");
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
            catch(final Throwable ex) {
                getInstance().println("Unexpected exception setting look and feel", ex);
            }
        }

        frame.dispose();
    }

    private final VersionManager versionManager;
    private final JFrame frame;
    private final LauncherPanel launcherPanel;
    private final GameLauncher gameLauncher;
    private final File workingDirectory;
    private final Proxy proxy;
    private final PasswordAuthentication proxyAuth;
    private final String[] additionalArgs;
    private final Integer bootstrapVersion;

    private final ProfileManager profileManager;

    private UUID clientToken = UUID.randomUUID();

    public Launcher(final JFrame frame, final File workingDirectory, final Proxy proxy, final PasswordAuthentication proxyAuth, final String[] args) {
        this(frame, workingDirectory, proxy, proxyAuth, args, Integer.valueOf(0));
    }

    public Launcher(final JFrame frame, final File workingDirectory, final Proxy proxy, final PasswordAuthentication proxyAuth, final String[] args, final Integer bootstrapVersion) {
        this.bootstrapVersion = bootstrapVersion;
        instance = this;
        setLookAndFeel();

        this.proxy = proxy;
        this.proxyAuth = proxyAuth;
        additionalArgs = args;
        this.workingDirectory = workingDirectory;
        this.frame = frame;
        gameLauncher = new GameLauncher(this);
        profileManager = new ProfileManager(this);
        versionManager = new VersionManager(new LocalVersionList(workingDirectory), new RemoteVersionList(proxy));
        launcherPanel = new LauncherPanel(this);

        initializeFrame();

        for(final String line : delayedSysout)
            launcherPanel.getTabPanel().getConsole().print(line + "\n");

        if(bootstrapVersion.intValue() < 4) {
            showOutdatedNotice();
            return;
        }

        downloadResources();
        refreshVersionsAndProfiles();

        println("Launcher " + LauncherConstants.VERSION_NAME + " (through bootstrap " + bootstrapVersion + ") started on " + OperatingSystem.getCurrentPlatform().getName() + "...");
        println("Current time is " + DateFormat.getDateTimeInstance(2, 2, Locale.US).format(new Date()));

        if(!OperatingSystem.getCurrentPlatform().isSupported())
            println("This operating system is unknown or unsupported, we cannot guarantee that the game will launch.");
        println("System.getProperty('os.name') == '" + System.getProperty("os.name") + "'");
        println("System.getProperty('os.version') == '" + System.getProperty("os.version") + "'");
        println("System.getProperty('os.arch') == '" + System.getProperty("os.arch") + "'");
        println("System.getProperty('java.version') == '" + System.getProperty("java.version") + "'");
        println("System.getProperty('java.vendor') == '" + System.getProperty("java.vendor") + "'");
        println("System.getProperty('sun.arch.data.model') == '" + System.getProperty("sun.arch.data.model") + "'");
    }

    public void closeLauncher() {
        frame.dispatchEvent(new WindowEvent(frame, 201));
    }

    private void downloadResources() {
        final DownloadJob job = new DownloadJob("Resources", true, gameLauncher);
        gameLauncher.addJob(job);
        versionManager.getExecutorService().submit(new Runnable() {
            public void run() {
                try {
                    versionManager.downloadResources(job);
                    job.startDownloading(versionManager.getExecutorService());
                }
                catch(final IOException e) {
                    Launcher.getInstance().println("Unexpected exception queueing resource downloads", e);
                }
            }
        });
    }

    public void ensureLoggedIn() {
        final Profile selectedProfile = profileManager.getSelectedProfile();
        final AuthenticationService auth = profileManager.getAuthDatabase().getByUUID(selectedProfile.getPlayerUUID());

        if(auth == null)
            showLoginPrompt();
        else if(!auth.isLoggedIn()) {
            if(auth.canLogIn())
                try {
                    auth.logIn();
                    try {
                        profileManager.saveProfiles();
                    }
                    catch(final IOException e) {
                        println("Couldn't save profiles after refreshing auth!", e);
                    }
                    profileManager.fireRefreshEvent();
                }
                catch(final AuthenticationException e) {
                    println(e);
                    showLoginPrompt();
                }
            else
                showLoginPrompt();
        }
        else if(!auth.canPlayOnline())
            try {
                println("Refreshing auth...");
                auth.logIn();
                try {
                    profileManager.saveProfiles();
                }
                catch(final IOException e) {
                    println("Couldn't save profiles after refreshing auth!", e);
                }
                profileManager.fireRefreshEvent();
            }
            catch(final InvalidCredentialsException e) {
                println(e);
                showLoginPrompt();
            }
            catch(final AuthenticationException e) {
                println(e);
            }
    }

    public String[] getAdditionalArgs() {
        return additionalArgs;
    }

    public int getBootstrapVersion() {
        return bootstrapVersion.intValue();
    }

    public UUID getClientToken() {
        return clientToken;
    }

    public JFrame getFrame() {
        return frame;
    }

    public GameLauncher getGameLauncher() {
        return gameLauncher;
    }

    public LauncherPanel getLauncherPanel() {
        return launcherPanel;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public PasswordAuthentication getProxyAuth() {
        return proxyAuth;
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    protected void initializeFrame() {
        frame.getContentPane().removeAll();
        frame.setTitle(LauncherConstants.SERVER_NAME + " Launcher 1.2.2");
        frame.setPreferredSize(new Dimension(900, 580));
        frame.setDefaultCloseOperation(2);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                frame.setVisible(false);
                frame.dispose();
                versionManager.getExecutorService().shutdown();
            }
        });
        try {
            final InputStream in = Launcher.class.getResourceAsStream("/favicon.png");
            if(in != null)
                frame.setIconImage(ImageIO.read(in));
        }
        catch(final IOException localIOException) {
        }
        frame.add(launcherPanel);

        frame.pack();
        frame.setVisible(true);
    }

    public void println(final String line) {
        System.out.println(line);

        if(launcherPanel == null)
            delayedSysout.add(line);
        else
            launcherPanel.getTabPanel().getConsole().print(line + "\n");
    }

    public void println(final String line, final Throwable throwable) {
        println(line);
        println(throwable);
    }

    public void println(final Throwable throwable) {
        StringWriter writer = null;
        PrintWriter printWriter = null;
        String result = throwable.toString();
        try {
            writer = new StringWriter();
            printWriter = new PrintWriter(writer);
            throwable.printStackTrace(printWriter);
            result = writer.toString();
        }
        finally {
            try {
                if(writer != null)
                    writer.close();
                if(printWriter != null)
                    printWriter.close();
            }
            catch(final IOException localIOException1) {
            }

        }
        println(result);
    }

    public void refreshVersionsAndProfiles() {
        versionManager.getExecutorService().submit(new Runnable() {
            public void run() {
                try {
                    versionManager.refreshVersions();
                }
                catch(final Throwable e) {
                    Launcher.getInstance().println("Unexpected exception refreshing version list", e);
                }
                try {
                    profileManager.loadProfiles();
                    println("Loaded " + profileManager.getProfiles().size() + " profile(s); selected '" + profileManager.getSelectedProfile().getName() + "'");
                }
                catch(final Throwable e) {
                    Launcher.getInstance().println("Unexpected exception refreshing profile list", e);
                }

                ensureLoggedIn();
            }
        });
    }

    public void setClientToken(final UUID clientToken) {
        this.clientToken = clientToken;
    }

    public void showLoginPrompt() {
        try {
            profileManager.saveProfiles();
        }
        catch(final IOException e) {
            println("Couldn't save profiles before logging in!", e);
        }

        for(final Profile profile : profileManager.getProfiles().values()) {
            final Map<String, String> credentials = profile.getAuthentication();

            if(credentials != null) {
                final AuthenticationService auth = new YggdrasilAuthenticationService();
                auth.loadFromStorage(credentials);

                if(auth.isLoggedIn()) {
                    final String uuid = auth.getSelectedProfile() == null ? "demo-" + auth.getUsername() : auth.getSelectedProfile().getId();
                    if(profileManager.getAuthDatabase().getByUUID(uuid) == null)
                        profileManager.getAuthDatabase().register(uuid, auth);
                }

                profile.setAuthentication(null);
            }
        }

        final Profile selectedProfile = profileManager.getSelectedProfile();
        LogInPopup.showLoginPrompt(this, new LogInPopup.Callback() {
            public void onLogIn(final String uuid) {
                final AuthenticationService auth = profileManager.getAuthDatabase().getByUUID(uuid);
                selectedProfile.setPlayerUUID(uuid);

                if(selectedProfile.getName().equals(LauncherConstants.DEFAULT_PROFILE_NAME) && auth.getSelectedProfile() != null) {
                    final String playerName = auth.getSelectedProfile().getName();
                    String profileName = auth.getSelectedProfile().getName();
                    int count = 1;

                    while(profileManager.getProfiles().containsKey(profileName))
                        profileName = playerName + " " + ++count;

                    final Profile newProfile = new Profile(selectedProfile);
                    newProfile.setName(profileName);
                    profileManager.getProfiles().put(profileName, newProfile);
                    profileManager.getProfiles().remove(LauncherConstants.DEFAULT_PROFILE_NAME);
                    profileManager.setSelectedProfile(profileName);
                }
                try {
                    profileManager.saveProfiles();
                }
                catch(final IOException e) {
                    println("Couldn't save profiles after logging in!", e);
                }

                if(uuid == null)
                    closeLauncher();
                else
                    profileManager.fireRefreshEvent();

                launcherPanel.setCard("launcher", null);
            }
        });
    }

    private void showOutdatedNotice() {
        final String error = "Sorry, but your launcher is outdated! Please redownload it at " + LauncherConstants.URL_BOOTSTRAP_DOWNLOAD;

        frame.getContentPane().removeAll();

        final int result = JOptionPane.showOptionDialog(frame, error, "Outdated launcher", 0, 0, null, new String[] { "Go to URL", "Close" }, "Go to URL");

        if(result == 0)
            try {
                OperatingSystem.openLink(new URI(LauncherConstants.URL_BOOTSTRAP_DOWNLOAD));
            }
            catch(final URISyntaxException e) {
                println("Couldn't open bootstrap download link. Please visit " + LauncherConstants.URL_BOOTSTRAP_DOWNLOAD + " manually.", e);
            }
        closeLauncher();
    }
}