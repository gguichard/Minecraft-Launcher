package net.minecraft.launcher;

import java.net.URI;
import java.net.URISyntaxException;

public class LauncherConstants {
    public static final String VERSION_NAME = "1.0";
    public static final int VERSION_NUMERIC = 7;
    public static final String DEFAULT_PROFILE_NAME = "Minecraft";
    public static final String SERVER_NAME = "Minecraft";
    public static final URI URL_REGISTER = constantURI("https://account.mojang.com/register");
    // public static final String URL_DOWNLOAD_BASE =
    // "https://s3.amazonaws.com/Minecraft.Download/";
    public static final String URL_DOWNLOAD_BASE = "http://dl.rellynn.eu/launcher/";
    public static final String URL_RESOURCE_BASE = "https://s3.amazonaws.com/Minecraft.Resources/";
    public static final String LIBRARY_DOWNLOAD_BASE = "https://s3.amazonaws.com/Minecraft.Download/libraries/";
    public static final String URL_BLOG = "http://mcupdate.tumblr.com";
    public static final String URL_STATUS_CHECKER = "http://status.mojang.com/check";
    public static final String URL_BOOTSTRAP_DOWNLOAD = "http://dl.rellynn.eu/launcher/bootstrap.jar";
    public static final URI URL_FORGOT_USERNAME = constantURI("http://help.mojang.com/customer/portal/articles/1233873");
    public static final URI URL_FORGOT_PASSWORD_MINECRAFT = constantURI("http://help.mojang.com/customer/portal/articles/329524-change-or-forgot-password");
    public static final URI URL_FORGOT_MIGRATED_EMAIL = constantURI("http://help.mojang.com/customer/portal/articles/1205055-minecraft-launcher-error---migrated-account");

    public static URI constantURI(final String input) {
        try {
            return new URI(input);
        }
        catch(final URISyntaxException e) {
            throw new Error(e);
        }
    }
}