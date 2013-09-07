package net.minecraft.launcher.ui.popups.login;

import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.OperatingSystem;

public class LogInPopup extends JPanel implements ActionListener {
    public static abstract interface Callback {
        public abstract void onLogIn(String paramString);
    }

    public static void showLoginPrompt(final Launcher launcher, final Callback callback) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final LogInPopup popup = new LogInPopup(launcher, callback);
                launcher.getLauncherPanel().setCard("login", popup);
            }
        });
    }

    private final Launcher launcher;
    private final Callback callback;
    private final AuthErrorForm errorForm;
    private final ExistingUserListForm existingUserListForm;
    private final LogInForm logInForm;
    private final JButton loginButton = new JButton("Log In");

    private final JButton registerButton = new JButton("Register");

    private final JProgressBar progressBar = new JProgressBar();

    public LogInPopup(final Launcher launcher, final Callback callback) {
        super(true);
        this.launcher = launcher;
        this.callback = callback;
        errorForm = new AuthErrorForm(this);
        existingUserListForm = new ExistingUserListForm(this);
        logInForm = new LogInForm(this);

        createInterface();

        loginButton.addActionListener(this);
        registerButton.addActionListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        if(e.getSource() == loginButton)
            logInForm.tryLogIn();
        else if(e.getSource() == registerButton)
            OperatingSystem.openLink(LauncherConstants.URL_REGISTER);
    }

    protected void createInterface() {
        setLayout(new BoxLayout(this, 1));
        setBorder(new EmptyBorder(5, 15, 5, 15));
        try {
            final InputStream stream = LogInPopup.class.getResourceAsStream("/minecraft_logo.png");
            if(stream != null) {
                final BufferedImage image = ImageIO.read(stream);
                final JLabel label = new JLabel(new ImageIcon(image));
                final JPanel imagePanel = new JPanel();
                imagePanel.add(label);
                add(imagePanel);
                add(Box.createVerticalStrut(10));
            }
        }
        catch(final IOException e) {
            e.printStackTrace();
        }

        if(!launcher.getProfileManager().getAuthDatabase().getKnownNames().isEmpty())
            add(existingUserListForm);
        add(errorForm);
        add(logInForm);

        add(Box.createVerticalStrut(15));

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2, 10, 0));
        buttonPanel.add(registerButton);
        buttonPanel.add(loginButton);

        add(buttonPanel);

        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        add(progressBar);
    }

    public AuthErrorForm getErrorForm() {
        return errorForm;
    }

    public ExistingUserListForm getExistingUserListForm() {
        return existingUserListForm;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public LogInForm getLogInForm() {
        return logInForm;
    }

    public void repack() {
        final Window window = SwingUtilities.windowForComponent(this);
        if(window != null)
            window.pack();
    }

    public void setCanLogIn(final boolean enabled) {
        if(SwingUtilities.isEventDispatchThread()) {
            loginButton.setEnabled(enabled);
            progressBar.setIndeterminate(false);
            progressBar.setIndeterminate(true);
            progressBar.setVisible(!enabled);
            repack();
        }
        else
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setCanLogIn(enabled);
                }
            });
    }

    public void setLoggedIn(final String uuid) {
        callback.onLogIn(uuid);
    }
}