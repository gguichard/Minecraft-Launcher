package net.minecraft.launcher.authentication;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.swing.SwingUtilities;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.events.AuthenticationChangedListener;

import org.apache.commons.lang3.StringUtils;

public abstract class BaseAuthenticationService implements AuthenticationService {
    private static final String LEGACY_LASTLOGIN_PASSWORD = "passwordfile";
    private static final int LEGACY_LASTLOGIN_SEED = 43287234;

    private static Cipher getCipher(final int mode, final String password) throws Exception {
        final Random random = new Random(43287234L);
        final byte[] salt = new byte[8];
        random.nextBytes(salt);
        final PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 5);

        final SecretKey pbeKey = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(new PBEKeySpec(password.toCharArray()));
        final Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        cipher.init(mode, pbeKey, pbeParamSpec);
        return cipher;
    }

    public static String[] getStoredDetails(final File lastLoginFile) {
        if(!lastLoginFile.isFile())
            return null;
        try {
            final Cipher cipher = getCipher(2, "passwordfile");
            DataInputStream dis;
            if(cipher != null)
                dis = new DataInputStream(new CipherInputStream(new FileInputStream(lastLoginFile), cipher));
            else
                dis = new DataInputStream(new FileInputStream(lastLoginFile));

            final String username = dis.readUTF();
            final String password = dis.readUTF();
            dis.close();
            return new String[] { username, password };
        }
        catch(final Exception e) {
            Launcher.getInstance().println("Couldn't load old lastlogin file", e);
        }
        return null;
    }

    private final List<AuthenticationChangedListener> listeners = new ArrayList<AuthenticationChangedListener>();
    private String username;
    private String password;

    private GameProfile selectedProfile;

    private boolean shouldRememberMe = true;

    public void addAuthenticationChangedListener(final AuthenticationChangedListener listener) {
        listeners.add(listener);
    }

    public boolean canLogIn() {
        return !canPlayOnline() && StringUtils.isNotBlank(getUsername()) && StringUtils.isNotBlank(getPassword());
    }

    public boolean canPlayOnline() {
        return isLoggedIn() && getSelectedProfile() != null && getSessionToken() != null;
    }

    protected void fireAuthenticationChangedEvent() {
        final List<AuthenticationChangedListener> listeners = new ArrayList<AuthenticationChangedListener>(this.listeners);

        for(final Iterator<AuthenticationChangedListener> iterator = listeners.iterator(); iterator.hasNext();) {
            final AuthenticationChangedListener listener = iterator.next();

            if(!listener.shouldReceiveEventsInUIThread()) {
                listener.onAuthenticationChanged(this);
                iterator.remove();
            }
        }

        if(!listeners.isEmpty())
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for(final AuthenticationChangedListener listener : listeners)
                        listener.onAuthenticationChanged(BaseAuthenticationService.this);
                }
            });
    }

    protected String getPassword() {
        return password;
    }

    public GameProfile getSelectedProfile() {
        return selectedProfile;
    }

    public String getUsername() {
        return username;
    }

    public String guessPasswordFromSillyOldFormat(final File file) {
        final String[] details = getStoredDetails(file);

        if(details != null && details[0].equals(getUsername()))
            return details[1];

        return null;
    }

    public boolean isLoggedIn() {
        return getSelectedProfile() != null;
    }

    public void loadFromStorage(final Map<String, String> credentials) {
        logOut();

        if(credentials.containsKey("rememberMe"))
            setRememberMe(Boolean.getBoolean(credentials.get("rememberMe")));

        setUsername(credentials.get("username"));

        if(credentials.containsKey("displayName") && credentials.containsKey("uuid"))
            setSelectedProfile(new GameProfile(credentials.get("uuid"), credentials.get("displayName")));
    }

    public void logOut() {
        password = null;
        setSelectedProfile(null);
    }

    public void removeAuthenticationChangedListener(final AuthenticationChangedListener listener) {
        listeners.remove(listener);
    }

    public Map<String, String> saveForStorage() {
        final Map<String, String> result = new HashMap<String, String>();

        if(!shouldRememberMe()) {
            result.put("rememberMe", Boolean.toString(false));
            return result;
        }

        if(getUsername() != null)
            result.put("username", getUsername());

        if(getSelectedProfile() != null) {
            result.put("displayName", getSelectedProfile().getName());
            result.put("uuid", getSelectedProfile().getId());
        }

        return result;
    }

    public void setPassword(final String password) {
        if(isLoggedIn() && canPlayOnline() && StringUtils.isNotBlank(password))
            throw new IllegalStateException("Cannot set password whilst logged in & online");

        this.password = password;
    }

    public void setRememberMe(final boolean rememberMe) {
        shouldRememberMe = rememberMe;
    }

    protected void setSelectedProfile(final GameProfile selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

    public void setUsername(final String username) {
        if(isLoggedIn() && canPlayOnline())
            throw new IllegalStateException("Cannot change username whilst logged in & online");

        this.username = username;
    }

    public boolean shouldRememberMe() {
        return shouldRememberMe;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();

        result.append(getClass().getSimpleName());
        result.append("{");

        if(isLoggedIn()) {
            result.append("Logged in as ");
            result.append(getUsername());

            if(getSelectedProfile() != null) {
                result.append(" / ");
                result.append(getSelectedProfile());
                result.append(" - ");

                if(canPlayOnline()) {
                    result.append("Online with session token '");
                    result.append(getSessionToken());
                    result.append("'");
                }
                else
                    result.append("Offline");
            }
        }
        else
            result.append("Not logged in");

        result.append("}");

        return result.toString();
    }
}