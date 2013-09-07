package net.minecraft.launcher.authentication;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.launcher.Http;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.Util;
import net.minecraft.launcher.authentication.exceptions.AuthenticationException;
import net.minecraft.launcher.authentication.exceptions.InvalidCredentialsException;

import org.apache.commons.lang3.StringUtils;

public class LegacyAuthenticationService extends BaseAuthenticationService {
    private static final URL AUTHENTICATION_URL = Util.constantURL("https://login.minecraft.net");
    private static final int AUTHENTICATION_VERSION = 14;
    private static final int RESPONSE_PART_PROFILE_NAME = 2;
    private static final int RESPONSE_PART_SESSION_TOKEN = 3;
    private static final int RESPONSE_PART_PROFILE_ID = 4;
    private String sessionToken;

    public GameProfile[] getAvailableProfiles() {
        if(getSelectedProfile() != null)
            return new GameProfile[] { getSelectedProfile() };
        return new GameProfile[0];
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void logIn() throws AuthenticationException {
        if(StringUtils.isBlank(getUsername()))
            throw new InvalidCredentialsException("Invalid username");
        if(StringUtils.isBlank(getPassword()))
            throw new InvalidCredentialsException("Invalid password");
        final Map<String, Object> args = new HashMap<String, Object>();
        args.put("user", getUsername());
        args.put("password", getPassword());
        args.put("version", Integer.valueOf(14));
        String response;
        try {
            response = Http.performPost(AUTHENTICATION_URL, args, Launcher.getInstance().getProxy()).trim();
        }
        catch(final IOException e) {
            throw new AuthenticationException("Authentication server is not responding", e);
        }

        final String[] split = response.split(":");

        if(split.length == 5) {
            final String profileId = split[4];
            final String profileName = split[2];
            final String sessionToken = split[3];

            if(StringUtils.isBlank(profileId) || StringUtils.isBlank(profileName) || StringUtils.isBlank(sessionToken))
                throw new AuthenticationException("Unknown response from authentication server: " + response);

            setSelectedProfile(new GameProfile(profileId, profileName));
            this.sessionToken = sessionToken;
            fireAuthenticationChangedEvent();
        }
        else
            throw new InvalidCredentialsException(response);
    }

    @Override
    public void logOut() {
        super.logOut();
        sessionToken = null;
        fireAuthenticationChangedEvent();
    }

    public void selectGameProfile(final GameProfile profile) throws AuthenticationException {
        throw new UnsupportedOperationException("Game profiles cannot be changed in the legacy authentication service");
    }
}