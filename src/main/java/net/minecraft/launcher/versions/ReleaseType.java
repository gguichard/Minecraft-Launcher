package net.minecraft.launcher.versions;

import java.util.HashMap;
import java.util.Map;

public enum ReleaseType {
    SNAPSHOT("snapshot", "Enable experimental development versions (\"snapshots\")"), RELEASE("release", null), OLD_BETA("old-beta", "Allow use of old \"Beta\" minecraft versions (From 2010-2011)"), OLD_ALPHA("old-alpha", "Allow use of old \"Alpha\" minecraft versions (From 2010)");

    private static final String POPUP_DEV_VERSIONS = "Are you sure you want to enable development builds?\nThey are not guaranteed to be stable and may corrupt your world.\nYou are advised to run this in a separate directory or run regular backups.";
    private static final String POPUP_OLD_VERSIONS = "These versions are very out of date and may be unstable. Any bugs, crashes, missing features or\nother nasties you may find will never be fixed in these versions.\nIt is strongly recommended you play these in separate directories to avoid corruption.\nWe are not responsible for the damage to your nostalgia or your save files!";
    private static final Map<String, ReleaseType> lookup;

    public static ReleaseType getByName(final String name) {
        return lookup.get(name);
    }

    private final String name;

    private final String description;

    static {
        lookup = new HashMap<String, ReleaseType>();

        for(final ReleaseType type : values())
            lookup.put(type.getName(), type);
    }

    private ReleaseType(final String name, final String description) {
        this.name = name;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getPopupWarning() {
        if(description == null)
            return null;
        if(this == SNAPSHOT)
            return POPUP_DEV_VERSIONS;
        if(this == OLD_BETA)
            return POPUP_OLD_VERSIONS;
        if(this == OLD_ALPHA)
            return POPUP_OLD_VERSIONS;
        return null;
    }
}