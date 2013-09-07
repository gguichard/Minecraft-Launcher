package net.minecraft.launcher.ui.bottombar;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.events.RefreshedProfilesListener;
import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.updater.VersionManager;
import net.minecraft.launcher.updater.VersionSyncInfo;

public class PlayerInfoPanel extends JPanel implements RefreshedProfilesListener, RefreshedVersionsListener {
    private final Launcher launcher;
    private final JLabel welcomeText = new JLabel("", 0);
    private final JLabel versionText = new JLabel("", 0);
    private final JButton logOutButton = new JButton("Log Out");

    public PlayerInfoPanel(final Launcher launcher) {
        this.launcher = launcher;

        launcher.getProfileManager().addRefreshedProfilesListener(this);
        checkState();
        createInterface();

        logOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                launcher.getProfileManager().getSelectedProfile().setPlayerUUID(null);
                launcher.getProfileManager().trimAuthDatabase();
                launcher.showLoginPrompt();
            }
        });
    }

    public void checkState() {
        final Profile profile = launcher.getProfileManager().getProfiles().isEmpty() ? null : launcher.getProfileManager().getSelectedProfile();
        final AuthenticationService auth = profile == null ? null : launcher.getProfileManager().getAuthDatabase().getByUUID(profile.getPlayerUUID());
        final List versions = profile == null ? null : launcher.getVersionManager().getVersions(profile.getVersionFilter());
        VersionSyncInfo version = profile == null || versions.isEmpty() ? null : (VersionSyncInfo) versions.get(0);

        if(profile != null && profile.getLastVersionId() != null) {
            final VersionSyncInfo requestedVersion = launcher.getVersionManager().getVersionSyncInfo(profile.getLastVersionId());
            if(requestedVersion != null && requestedVersion.getLatestVersion() != null)
                version = requestedVersion;
        }

        if(auth == null || !auth.isLoggedIn()) {
            welcomeText.setText("Welcome, guest! Please log in.");
            logOutButton.setEnabled(false);
        }
        else if(auth.getSelectedProfile() == null) {
            welcomeText.setText("<html>Welcome, player!</html>");
            logOutButton.setEnabled(true);
        }
        else {
            welcomeText.setText("<html>Welcome, <b>" + auth.getSelectedProfile().getName() + "</b></html>");
            logOutButton.setEnabled(true);
        }

        if(version == null)
            versionText.setText("Loading versions...");
        else if(version.isUpToDate())
            versionText.setText("Ready to play Minecraft " + version.getLatestVersion().getId());
        else if(version.isInstalled())
            versionText.setText("Ready to update & play Minecraft " + version.getLatestVersion().getId());
        else if(version.isOnRemote())
            versionText.setText("Ready to download & play Minecraft " + version.getLatestVersion().getId());
    }

    protected void createInterface() {
        setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;

        constraints.gridy = 0;

        constraints.weightx = 1.0D;
        constraints.gridwidth = 2;
        add(welcomeText, constraints);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0D;

        constraints.gridy += 1;

        constraints.weightx = 1.0D;
        constraints.gridwidth = 2;
        add(versionText, constraints);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0D;

        constraints.gridy += 1;

        constraints.weightx = 0.5D;
        constraints.fill = 0;
        add(logOutButton, constraints);
        constraints.weightx = 0.0D;

        constraints.gridy += 1;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public void onProfilesRefreshed(final ProfileManager manager) {
        checkState();
    }

    public void onVersionsRefreshed(final VersionManager manager) {
        checkState();
    }

    public boolean shouldReceiveEventsInUIThread() {
        return true;
    }
}