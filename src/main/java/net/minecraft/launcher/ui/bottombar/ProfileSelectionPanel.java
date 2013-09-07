package net.minecraft.launcher.ui.bottombar;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.events.RefreshedProfilesListener;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.ui.popups.profile.ProfileEditorPopup;

public class ProfileSelectionPanel extends JPanel implements ActionListener, ItemListener, RefreshedProfilesListener {
    private static class ProfileListRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(final JList list, Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            if(value instanceof Profile)
                value = ((Profile) value).getName();

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return this;
        }
    }

    private final JComboBox profileList = new JComboBox();
    private final JButton newProfileButton = new JButton("New Profile");
    private final JButton editProfileButton = new JButton("Edit Profile");
    private final Launcher launcher;

    private boolean skipSelectionUpdate;

    public ProfileSelectionPanel(final Launcher launcher) {
        this.launcher = launcher;

        profileList.setRenderer(new ProfileListRenderer());
        profileList.addItemListener(this);
        profileList.addItem("Loading profiles...");

        newProfileButton.addActionListener(this);
        editProfileButton.addActionListener(this);

        createInterface();

        launcher.getProfileManager().addRefreshedProfilesListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        if(e.getSource() == newProfileButton) {
            final Profile profile = new Profile(launcher.getProfileManager().getSelectedProfile());
            profile.setName("Copy of " + profile.getName());

            while(launcher.getProfileManager().getProfiles().containsKey(profile.getName()))
                profile.setName(profile.getName() + "_");

            ProfileEditorPopup.showEditProfileDialog(getLauncher(), profile);
            launcher.getProfileManager().setSelectedProfile(profile.getName());
        }
        else if(e.getSource() == editProfileButton) {
            final Profile profile = launcher.getProfileManager().getSelectedProfile();
            ProfileEditorPopup.showEditProfileDialog(getLauncher(), profile);
        }
    }

    protected void createInterface() {
        setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;
        constraints.weightx = 0.0D;

        constraints.gridy = 0;

        add(new JLabel("Profile: "), constraints);
        constraints.gridx = 1;
        add(profileList, constraints);
        constraints.gridx = 0;

        constraints.gridy += 1;

        final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.setBorder(new EmptyBorder(2, 0, 0, 0));
        buttonPanel.add(newProfileButton);
        buttonPanel.add(editProfileButton);

        constraints.gridwidth = 2;
        add(buttonPanel, constraints);
        constraints.gridwidth = 1;

        constraints.gridy += 1;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public void itemStateChanged(final ItemEvent e) {
        if(e.getStateChange() != 1)
            return;

        if(!skipSelectionUpdate && e.getItem() instanceof Profile) {
            final Profile profile = (Profile) e.getItem();
            launcher.getProfileManager().setSelectedProfile(profile.getName());
            try {
                launcher.getProfileManager().saveProfiles();
            }
            catch(final IOException e1) {
                launcher.println("Couldn't save new selected profile", e1);
            }
            launcher.ensureLoggedIn();
        }
    }

    public void onProfilesRefreshed(final ProfileManager manager) {
        populateProfiles();
    }

    public void populateProfiles() {
        final String previous = launcher.getProfileManager().getSelectedProfile().getName();
        Profile selected = null;
        final Collection<Profile> profiles = launcher.getProfileManager().getProfiles().values();
        profileList.removeAllItems();

        skipSelectionUpdate = true;

        for(final Profile profile : profiles) {
            if(previous.equals(profile.getName()))
                selected = profile;

            profileList.addItem(profile);
        }

        if(selected == null) {
            if(profiles.isEmpty()) {
                selected = launcher.getProfileManager().getSelectedProfile();
                profileList.addItem(selected);
            }

            selected = profiles.iterator().next();
        }

        profileList.setSelectedItem(selected);
        skipSelectionUpdate = false;
    }

    public boolean shouldReceiveEventsInUIThread() {
        return true;
    }
}