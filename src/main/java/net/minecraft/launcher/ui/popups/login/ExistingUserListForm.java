package net.minecraft.launcher.ui.popups.login;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.minecraft.launcher.authentication.AuthenticationDatabase;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.authentication.exceptions.AuthenticationException;

public class ExistingUserListForm extends JPanel implements ActionListener {
    private final LogInPopup popup;
    private final JComboBox userDropdown = new JComboBox();
    private final AuthenticationDatabase authDatabase;
    private final JButton playButton = new JButton("Play");

    public ExistingUserListForm(final LogInPopup popup) {
        this.popup = popup;
        authDatabase = popup.getLauncher().getProfileManager().getAuthDatabase();

        fillUsers();
        createInterface();

        playButton.addActionListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        if(e.getSource() == playButton) {
            popup.setCanLogIn(false);

            popup.getLauncher().getVersionManager().getExecutorService().execute(new Runnable() {
                public void run() {
                    final Object selected = userDropdown.getSelectedItem();
                    String uuid;
                    AuthenticationService auth;
                    if(selected != null && selected instanceof String) {
                        auth = authDatabase.getByName((String) selected);
                        if(auth.getSelectedProfile() == null)
                            uuid = "demo-" + auth.getUsername();
                        else
                            uuid = auth.getSelectedProfile().getId();
                    }
                    else {
                        auth = null;
                        uuid = null;
                    }

                    if(auth != null && uuid != null)
                        try {
                            auth.logIn();
                            popup.setLoggedIn(uuid);
                        }
                        catch(final AuthenticationException ex) {
                            popup.getErrorForm().displayError(new String[] { "We couldn't log you back in as " + selected + ".", "Please try to log in again." });

                            userDropdown.removeItem(selected);

                            if(userDropdown.getItemCount() == 0)
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        popup.remove(ExistingUserListForm.this);
                                    }
                                });

                            popup.setCanLogIn(true);
                        }
                    else
                        popup.setCanLogIn(true);
                }
            });
        }
    }

    protected void createInterface() {
        setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;
        constraints.gridx = 0;
        constraints.gridy = -1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0D;

        add(Box.createGlue());

        final String currentUser = authDatabase.getKnownNames().size() + " different users";
        final String thisOrThese = authDatabase.getKnownNames().size() == 1 ? "this account" : "one of these accounts";
        add(new JLabel("You're already logged in as " + currentUser + "."), constraints);
        add(new JLabel("You may use " + thisOrThese + " and skip authentication."), constraints);

        add(Box.createVerticalStrut(5), constraints);

        final JLabel usernameLabel = new JLabel("Existing User:");
        final Font labelFont = usernameLabel.getFont().deriveFont(1);

        usernameLabel.setFont(labelFont);
        add(usernameLabel, constraints);

        constraints.gridwidth = 1;
        add(userDropdown, constraints);

        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 0.0D;
        constraints.insets = new Insets(0, 5, 0, 0);
        add(playButton, constraints);
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.weightx = 1.0D;
        constraints.gridx = 0;
        constraints.gridy = -1;

        constraints.gridwidth = 2;

        add(Box.createVerticalStrut(5), constraints);
        add(new JLabel("Alternatively, log in with a new account below:"), constraints);
        add(new JPopupMenu.Separator(), constraints);
    }

    private void fillUsers() {
        for(final String user : authDatabase.getKnownNames())
            userDropdown.addItem(user);
    }
}