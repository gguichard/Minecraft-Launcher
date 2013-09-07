package net.minecraft.launcher.authentication.yggdrasil;

import net.minecraft.launcher.authentication.GameProfile;

public class RefreshRequest {
    private final String clientToken;
    private final String accessToken;
    private final GameProfile selectedProfile;

    public RefreshRequest(final YggdrasilAuthenticationService authenticationService) {
        this(authenticationService, null);
    }

    public RefreshRequest(final YggdrasilAuthenticationService authenticationService, final GameProfile profile) {
        clientToken = authenticationService.getClientToken();
        accessToken = authenticationService.getAccessToken();
        selectedProfile = profile;
    }
}