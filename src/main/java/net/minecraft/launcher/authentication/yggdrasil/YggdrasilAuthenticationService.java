package net.minecraft.launcher.authentication.yggdrasil;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.Util;
import net.minecraft.launcher.authentication.BaseAuthenticationService;
import net.minecraft.launcher.authentication.GameProfile;
import net.minecraft.launcher.authentication.exceptions.AuthenticationException;
import net.minecraft.launcher.authentication.exceptions.InvalidCredentialsException;
import net.minecraft.launcher.authentication.exceptions.UserMigratedException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class YggdrasilAuthenticationService extends BaseAuthenticationService {
    private static final String BASE_URL = "https://authserver.mojang.com/";
    private static final URL ROUTE_AUTHENTICATE = Util.constantURL("https://authserver.mojang.com/authenticate");
    private static final URL ROUTE_REFRESH = Util.constantURL("https://authserver.mojang.com/refresh");
    private static final URL ROUTE_VALIDATE = Util.constantURL("https://authserver.mojang.com/validate");
    private static final URL ROUTE_INVALIDATE = Util.constantURL("https://authserver.mojang.com/invalidate");
    private static final URL ROUTE_SIGNOUT = Util.constantURL("https://authserver.mojang.com/signout");
    private static final String STORAGE_KEY_ACCESS_TOKEN = "accessToken";
    private final Gson gson = new Gson();
    private final Agent agent = Agent.MINECRAFT;
    private GameProfile[] profiles;
    private String accessToken;
    private boolean isOnline;

    @Override
    public boolean canLogIn() {
        return !canPlayOnline() && StringUtils.isNotBlank(getUsername()) && (StringUtils.isNotBlank(getPassword()) || StringUtils.isNotBlank(getAccessToken()));
    }

    @Override
    public boolean canPlayOnline() {
        return isLoggedIn() && getSelectedProfile() != null && isOnline;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Agent getAgent() {
        return agent;
    }

    public GameProfile[] getAvailableProfiles() {
        return profiles;
    }

    public String getClientToken() {
        return Launcher.getInstance().getClientToken().toString();
    }

    public String getSessionToken() {
        if(isLoggedIn() && getSelectedProfile() != null && canPlayOnline())
            return String.format("token:%s:%s", new Object[] { getAccessToken(), getSelectedProfile().getId() });
        return null;
    }

    @Override
    public boolean isLoggedIn() {
        return StringUtils.isNotBlank(accessToken);
    }

    @Override
    public void loadFromStorage(final Map<String, String> credentials) {
        super.loadFromStorage(credentials);

        accessToken = credentials.get("accessToken");
    }

    public void logIn() throws AuthenticationException {
        if(StringUtils.isBlank(getUsername()))
            throw new InvalidCredentialsException("Invalid username");

        if(StringUtils.isNotBlank(getAccessToken()))
            logInWithToken();
        else if(StringUtils.isNotBlank(getPassword()))
            logInWithPassword();
        else
            throw new InvalidCredentialsException("Invalid password");
    }

    protected void logInWithPassword() throws AuthenticationException {
        if(StringUtils.isBlank(getUsername()))
            throw new InvalidCredentialsException("Invalid username");
        if(StringUtils.isBlank(getPassword()))
            throw new InvalidCredentialsException("Invalid password");

        Launcher.getInstance().println("Logging in with username & password");

        final AuthenticationRequest request = new AuthenticationRequest(this, getPassword());
        final AuthenticationResponse response = (AuthenticationResponse) makeRequest(ROUTE_AUTHENTICATE, request, AuthenticationResponse.class);

        if(!response.getClientToken().equals(getClientToken()))
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");

        accessToken = response.getAccessToken();
        profiles = response.getAvailableProfiles();
        setSelectedProfile(response.getSelectedProfile());

        fireAuthenticationChangedEvent();
    }

    protected void logInWithToken() throws AuthenticationException {
        if(StringUtils.isBlank(getUsername()))
            throw new InvalidCredentialsException("Invalid username");
        if(StringUtils.isBlank(getAccessToken()))
            throw new InvalidCredentialsException("Invalid access token");

        Launcher.getInstance().println("Logging in with access token");

        final RefreshRequest request = new RefreshRequest(this);
        final RefreshResponse response = (RefreshResponse) makeRequest(ROUTE_REFRESH, request, RefreshResponse.class);

        if(!response.getClientToken().equals(getClientToken()))
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");

        accessToken = response.getAccessToken();
        profiles = response.getAvailableProfiles();
        setSelectedProfile(response.getSelectedProfile());

        fireAuthenticationChangedEvent();
    }

    @Override
    public void logOut() {
        super.logOut();

        accessToken = null;
        profiles = null;
        isOnline = false;
    }

    protected <T extends Response> Response makeRequest(final URL url, final Object input, final Class<T> classOfT) throws AuthenticationException {
        try {
            final String jsonResult = Util.performPost(url, gson.toJson(input), Launcher.getInstance().getProxy(), "application/json", true);
            final Response result = gson.fromJson(jsonResult, classOfT);

            if(result == null)
                return null;

            if(StringUtils.isNotBlank(result.getError())) {
                if("UserMigratedException".equals(result.getCause()))
                    throw new UserMigratedException(result.getErrorMessage());
                if(result.getError().equals("ForbiddenOperationException"))
                    throw new InvalidCredentialsException(result.getErrorMessage());
                throw new AuthenticationException(result.getErrorMessage());
            }

            isOnline = true;

            return result;
        }
        catch(final IOException e) {
            throw new AuthenticationException("Cannot contact authentication server", e);
        }
        catch(final IllegalStateException e) {
            throw new AuthenticationException("Cannot contact authentication server", e);
        }
        catch(final JsonParseException e) {
            throw new AuthenticationException("Cannot contact authentication server", e);
        }
    }

    @Override
    public Map<String, String> saveForStorage() {
        final Map<String, String> result = super.saveForStorage();
        if(!shouldRememberMe())
            return result;

        if(StringUtils.isNotBlank(getAccessToken()))
            result.put("accessToken", getAccessToken());

        return result;
    }

    public void selectGameProfile(final GameProfile profile) throws AuthenticationException {
        if(!isLoggedIn())
            throw new AuthenticationException("Cannot change game profile whilst not logged in");
        if(getSelectedProfile() != null)
            throw new AuthenticationException("Cannot change game profile. You must log out and back in.");
        if(profile == null || !ArrayUtils.contains(profiles, profile))
            throw new IllegalArgumentException("Invalid profile '" + profile + "'");

        final RefreshRequest request = new RefreshRequest(this, profile);
        final RefreshResponse response = (RefreshResponse) makeRequest(ROUTE_REFRESH, request, RefreshResponse.class);

        if(!response.getClientToken().equals(getClientToken()))
            throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");

        accessToken = response.getAccessToken();
        setSelectedProfile(response.getSelectedProfile());

        fireAuthenticationChangedEvent();
    }

    @Override
    public String toString() {
        return "YggdrasilAuthenticationService{agent=" + agent + ", profiles=" + Arrays.toString(profiles) + ", selectedProfile=" + getSelectedProfile() + ", sessionToken='" + getSessionToken() + '\'' + ", username='" + getUsername() + '\'' + ", isLoggedIn=" + isLoggedIn() + ", canPlayOnline=" + canPlayOnline() + ", accessToken='" + accessToken + '\'' + ", clientToken='" + getClientToken() + '\'' + '}';
    }
}