package net.minecraft.launcher.profile;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.launcher.updater.VersionFilter;
import net.minecraft.launcher.versions.ReleaseType;

public class Profile {
    public static class Resolution {
        private int width;
        private int height;

        public Resolution() {
        }

        public Resolution(final int width, final int height) {
            this.width = width;
            this.height = height;
        }

        public Resolution(final Resolution resolution) {
            this(resolution.getWidth(), resolution.getHeight());
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }
    }

    public static final String DEFAULT_JRE_ARGUMENTS_64BIT = "-Xmx1G";
    public static final String DEFAULT_JRE_ARGUMENTS_32BIT = "-Xmx512M";
    public static final Resolution DEFAULT_RESOLUTION = new Resolution(854, 480);
    public static final LauncherVisibilityRule DEFAULT_LAUNCHER_VISIBILITY = LauncherVisibilityRule.CLOSE_LAUNCHER;
    public static final Set<ReleaseType> DEFAULT_RELEASE_TYPES = new HashSet<ReleaseType>(Arrays.asList(new ReleaseType[] { ReleaseType.RELEASE }));
    private String name;
    private File gameDir;
    private String lastVersionId;
    private String javaDir;
    private String javaArgs;
    private Resolution resolution;
    private Set<ReleaseType> allowedReleaseTypes;
    private String playerUUID;
    private Boolean useHopperCrashService;

    private LauncherVisibilityRule launcherVisibilityOnGameClose;

    @Deprecated
    private Map<String, String> authentication;

    public Profile() {
    }

    public Profile(final Profile copy) {
        name = copy.name;
        gameDir = copy.gameDir;
        playerUUID = copy.playerUUID;
        lastVersionId = copy.lastVersionId;
        javaDir = copy.javaDir;
        javaArgs = copy.javaArgs;
        resolution = copy.resolution == null ? null : new Resolution(copy.resolution);
        allowedReleaseTypes = copy.allowedReleaseTypes == null ? null : new HashSet<ReleaseType>(copy.allowedReleaseTypes);
        useHopperCrashService = copy.useHopperCrashService;
        launcherVisibilityOnGameClose = copy.launcherVisibilityOnGameClose;
    }

    public Profile(final String name) {
        this.name = name;
    }

    public Set<ReleaseType> getAllowedReleaseTypes() {
        return allowedReleaseTypes;
    }

    @Deprecated
    public Map<String, String> getAuthentication() {
        return authentication;
    }

    public File getGameDir() {
        return gameDir;
    }

    public String getJavaArgs() {
        return javaArgs;
    }

    public String getJavaPath() {
        return javaDir;
    }

    public String getLastVersionId() {
        return lastVersionId;
    }

    public LauncherVisibilityRule getLauncherVisibilityOnGameClose() {
        return launcherVisibilityOnGameClose;
    }

    public String getName() {
        return name;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public boolean getUseHopperCrashService() {
        return useHopperCrashService == null;
    }

    public VersionFilter getVersionFilter() {
        final VersionFilter filter = new VersionFilter().setMaxCount(2147483647);

        if(allowedReleaseTypes == null)
            filter.onlyForTypes(DEFAULT_RELEASE_TYPES.toArray(new ReleaseType[DEFAULT_RELEASE_TYPES.size()]));
        else
            filter.onlyForTypes(allowedReleaseTypes.toArray(new ReleaseType[allowedReleaseTypes.size()]));

        return filter;
    }

    public void setAllowedReleaseTypes(final Set<ReleaseType> allowedReleaseTypes) {
        this.allowedReleaseTypes = allowedReleaseTypes;
    }

    @Deprecated
    public void setAuthentication(final Map<String, String> authentication) {
        this.authentication = authentication;
    }

    public void setGameDir(final File gameDir) {
        this.gameDir = gameDir;
    }

    public void setJavaArgs(final String javaArgs) {
        this.javaArgs = javaArgs;
    }

    public void setJavaDir(final String javaDir) {
        this.javaDir = javaDir;
    }

    public void setLastVersionId(final String lastVersionId) {
        this.lastVersionId = lastVersionId;
    }

    public void setLauncherVisibilityOnGameClose(final LauncherVisibilityRule launcherVisibilityOnGameClose) {
        this.launcherVisibilityOnGameClose = launcherVisibilityOnGameClose;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setPlayerUUID(final String playerUUID) {
        this.playerUUID = playerUUID;
    }

    public void setResolution(final Resolution resolution) {
        this.resolution = resolution;
    }

    public void setUseHopperCrashService(final boolean useHopperCrashService) {
        this.useHopperCrashService = useHopperCrashService ? null : Boolean.valueOf(false);
    }
}