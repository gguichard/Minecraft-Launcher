package net.minecraft.launcher.ui.popups.profile;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;

public class ProfileEditorPopup extends JPanel implements ActionListener {
    public static void showEditProfileDialog(final Launcher launcher, final Profile profile) {
        final JDialog dialog = new JDialog(launcher.getFrame(), "Profile Editor", true);
        final ProfileEditorPopup editor = new ProfileEditorPopup(launcher, profile);
        dialog.add(editor);
        dialog.pack();
        dialog.setLocationRelativeTo(launcher.getFrame());
        dialog.setVisible(true);
    }

    private final Launcher launcher;
    private final Profile originalProfile;
    private final Profile profile;
    private final JButton saveButton = new JButton("Save Profile");
    private final JButton cancelButton = new JButton("Cancel");
    private final ProfileInfoPanel profileInfoPanel;
    private final ProfileVersionPanel profileVersionPanel;

    private final ProfileJavaPanel javaInfoPanel;

    public ProfileEditorPopup(final Launcher launcher, final Profile profile) {
        super(true);

        this.launcher = launcher;
        originalProfile = profile;
        this.profile = new Profile(profile);
        profileInfoPanel = new ProfileInfoPanel(this);
        profileVersionPanel = new ProfileVersionPanel(this);
        javaInfoPanel = new ProfileJavaPanel(this);

        saveButton.addActionListener(this);
        cancelButton.addActionListener(this);

        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout(0, 5));
        createInterface();
    }

    public void actionPerformed(final ActionEvent e) {
        if(e.getSource() == saveButton)
            try {
                final ProfileManager manager = launcher.getProfileManager();
                final Map<String, Profile> profiles = manager.getProfiles();

                if(!originalProfile.getName().equals(profile.getName())) {
                    profiles.remove(originalProfile.getName());

                    while(profiles.containsKey(profile.getName()))
                        profile.setName(profile.getName() + "_");
                }

                profiles.put(profile.getName(), profile);

                manager.saveProfiles();
                manager.fireRefreshEvent();
            }
            catch(final IOException ex) {
                launcher.println("Couldn't save profiles whilst editing " + profile.getName(), ex);
            }

        final Window window = (Window) getTopLevelAncestor();
        window.dispatchEvent(new WindowEvent(window, 201));
    }

    protected void createInterface() {
        final JPanel standardPanels = new JPanel(true);
        standardPanels.setLayout(new BoxLayout(standardPanels, 1));
        standardPanels.add(profileInfoPanel);
        standardPanels.add(profileVersionPanel);
        standardPanels.add(javaInfoPanel);

        add(standardPanels, "Center");

        final JPanel buttonPannel = new JPanel();
        buttonPannel.setLayout(new BoxLayout(buttonPannel, 0));
        buttonPannel.add(cancelButton);
        buttonPannel.add(Box.createGlue());
        buttonPannel.add(saveButton);
        add(buttonPannel, "South");
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public Profile getProfile() {
        return profile;
    }
}