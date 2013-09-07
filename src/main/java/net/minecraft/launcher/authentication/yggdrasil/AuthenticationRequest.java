package net.minecraft.launcher.authentication.yggdrasil;

public class AuthenticationRequest {
    private final Agent agent;
    private final String username;
    private final String password;
    private final String clientToken;

    public AuthenticationRequest(final YggdrasilAuthenticationService authenticationService, final String password) {
        agent = authenticationService.getAgent();
        username = authenticationService.getUsername();
        clientToken = authenticationService.getClientToken();
        this.password = password;
    }
}