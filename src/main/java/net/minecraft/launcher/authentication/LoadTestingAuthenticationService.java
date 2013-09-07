package net.minecraft.launcher.authentication;

import java.io.File;
import java.util.Map;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.authentication.exceptions.AuthenticationException;
import net.minecraft.launcher.authentication.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.launcher.events.AuthenticationChangedListener;

public class LoadTestingAuthenticationService implements AuthenticationService {
    private final AuthenticationService primary = new LegacyAuthenticationService();
    private final AuthenticationService secondary = new YggdrasilAuthenticationService();

    public void addAuthenticationChangedListener(final AuthenticationChangedListener listener) {
        primary.addAuthenticationChangedListener(listener);
    }

    public boolean canLogIn() {
        return primary.canLogIn();
    }

    public boolean canPlayOnline() {
        return primary.canPlayOnline();
    }

    public GameProfile[] getAvailableProfiles() {
        return primary.getAvailableProfiles();
    }

    public GameProfile getSelectedProfile() {
        return primary.getSelectedProfile();
    }

    public String getSessionToken() {
        return primary.getSessionToken();
    }

    public String getUsername() {
        return primary.getUsername();
    }

    public String guessPasswordFromSillyOldFormat(final File lastlogin) {
        return primary.guessPasswordFromSillyOldFormat(lastlogin);
    }

    public boolean isLoggedIn() {
        return primary.isLoggedIn();
    }

    public void loadFromStorage(final Map<String, String> credentials) {
        primary.loadFromStorage(credentials);
        secondary.loadFromStorage(credentials);
    }

    public void logIn() throws AuthenticationException {
        primary.logIn();
        try {
            secondary.logIn();
        }
        catch(final AuthenticationException e) {
            Launcher.getInstance().println("Couldn't load-test new authentication service (method: logIn)", e);
        }
    }

    public void logOut() {
        primary.logOut();
        secondary.logOut();
    }

    public void removeAuthenticationChangedListener(final AuthenticationChangedListener listener) {
        primary.removeAuthenticationChangedListener(listener);
    }

    public Map<String, String> saveForStorage() {
        return primary.saveForStorage();
    }

    public void selectGameProfile(final GameProfile profile) throws AuthenticationException {
        primary.selectGameProfile(profile);
        try {
            secondary.selectGameProfile(profile);
        }
        catch(final AuthenticationException e) {
            Launcher.getInstance().println("Couldn't load-test new authentication service (method: selectGameProfile)", e);
        }
    }

    public void setPassword(final String password) {
        primary.setPassword(password);
        secondary.setPassword(password);
    }

    public void setRememberMe(final boolean rememberMe) {
        primary.setRememberMe(rememberMe);
        secondary.setRememberMe(rememberMe);
    }

    public void setUsername(final String username) {
        primary.setUsername(username);
        secondary.setUsername(username);
    }

    public boolean shouldRememberMe() {
        return primary.shouldRememberMe();
    }
}