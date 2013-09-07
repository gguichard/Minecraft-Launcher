package net.minecraft.launcher.ui.bottombar;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.events.RefreshedProfilesListener;
import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.updater.VersionManager;

public class PlayButtonPanel extends JPanel implements RefreshedProfilesListener, RefreshedVersionsListener {
    private final Launcher launcher;
    private final JButton playButton = new JButton("Play");

    public PlayButtonPanel(final Launcher launcher) {
        this.launcher = launcher;

        launcher.getProfileManager().addRefreshedProfilesListener(this);
        checkState();
        createInterface();

        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getLauncher().getVersionManager().getExecutorService().submit(new Runnable() {
                    public void run() {
                        getLauncher().getGameLauncher().playGame();
                    }
                });
            }
        });
    }

    public void checkState() {
        final Profile profile = launcher.getProfileManager().getProfiles().isEmpty() ? null : launcher.getProfileManager().getSelectedProfile();
        final AuthenticationService auth = profile == null ? null : launcher.getProfileManager().getAuthDatabase().getByUUID(profile.getPlayerUUID());

        if(auth == null || !auth.isLoggedIn() || launcher.getVersionManager().getVersions(profile.getVersionFilter()).isEmpty()) {
            playButton.setEnabled(false);
            playButton.setText("Play");
        }
        else if(auth.getSelectedProfile() == null) {
            playButton.setEnabled(true);
            playButton.setText("Play Demo");
        }
        else if(auth.canPlayOnline()) {
            playButton.setEnabled(true);
            playButton.setText("Play");
        }
        else {
            playButton.setEnabled(true);
            playButton.setText("Play Offline");
        }

        if(launcher.getGameLauncher().isWorking())
            playButton.setEnabled(false);
    }

    protected void createInterface() {
        setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 1;
        constraints.weightx = 1.0D;
        constraints.weighty = 1.0D;

        constraints.gridy = 0;
        constraints.gridx = 0;
        add(playButton, constraints);

        playButton.setFont(playButton.getFont().deriveFont(1, playButton.getFont().getSize() + 2));
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