package net.minecraft.launcher.updater;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.PartialVersion;
import net.minecraft.launcher.versions.ReleaseType;
import net.minecraft.launcher.versions.Version;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class VersionList {
    private static class RawVersionList {
        private final List<PartialVersion> versions = new ArrayList<PartialVersion>();
        private final Map<ReleaseType, String> latest = new EnumMap<ReleaseType, String>(ReleaseType.class);

        public Map<ReleaseType, String> getLatestVersions() {
            return latest;
        }

        public List<PartialVersion> getVersions() {
            return versions;
        }
    }

    protected final Gson gson;
    private final Map<String, Version> versionsByName = new HashMap<String, Version>();
    private final List<Version> versions = new ArrayList<Version>();

    private final Map<ReleaseType, Version> latestVersions = new EnumMap<ReleaseType, Version>(ReleaseType.class);

    public VersionList() {
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.enableComplexMapKeySerialization();
        builder.setPrettyPrinting();

        gson = builder.create();
    }

    public CompleteVersion addVersion(final CompleteVersion version) {
        if(version.getId() == null)
            throw new IllegalArgumentException("Cannot add blank version");
        if(getVersion(version.getId()) != null)
            throw new IllegalArgumentException("Version '" + version.getId() + "' is already tracked");

        versions.add(version);
        versionsByName.put(version.getId(), version);

        return version;
    }

    protected void clearCache() {
        versionsByName.clear();
        versions.clear();
        latestVersions.clear();
    }

    public CompleteVersion getCompleteVersion(final String name) throws IOException {
        if(name == null || name.length() == 0)
            throw new IllegalArgumentException("Name cannot be null or empty");
        final Version version = getVersion(name);
        if(version == null)
            throw new IllegalArgumentException("Unknown version - cannot get complete version of null");
        return getCompleteVersion(version);
    }

    public CompleteVersion getCompleteVersion(final Version version) throws IOException {
        if(version instanceof CompleteVersion)
            return (CompleteVersion) version;
        if(version == null)
            throw new IllegalArgumentException("Version cannot be null");

        final CompleteVersion complete = gson.fromJson(getContent("versions/" + version.getId() + "/" + version.getId() + ".json"), CompleteVersion.class);
        final ReleaseType type = version.getType();

        Collections.replaceAll(versions, version, complete);
        versionsByName.put(version.getId(), complete);

        if(latestVersions.get(type) == version)
            latestVersions.put(type, complete);

        return complete;
    }

    protected abstract String getContent(String paramString) throws IOException;

    public Version getLatestVersion(final ReleaseType type) {
        if(type == null)
            throw new IllegalArgumentException("Type cannot be null");
        return latestVersions.get(type);
    }

    public Version getVersion(final String name) {
        if(name == null || name.length() == 0)
            throw new IllegalArgumentException("Name cannot be null or empty");
        return versionsByName.get(name);
    }

    public Collection<Version> getVersions() {
        return versions;
    }

    public abstract boolean hasAllFiles(CompleteVersion paramCompleteVersion, OperatingSystem paramOperatingSystem);

    public void refreshVersions() throws IOException {
        clearCache();

        final RawVersionList versionList = gson.fromJson(getContent("versions/versions.json"), RawVersionList.class);

        for(final Version version : versionList.getVersions()) {
            versions.add(version);
            versionsByName.put(version.getId(), version);
        }

        for(final ReleaseType type : ReleaseType.values())
            latestVersions.put(type, versionsByName.get(versionList.getLatestVersions().get(type)));
    }

    public void removeVersion(final String name) {
        if(name == null || name.length() == 0)
            throw new IllegalArgumentException("Name cannot be null or empty");
        final Version version = getVersion(name);
        if(version == null)
            throw new IllegalArgumentException("Unknown version - cannot remove null");
        removeVersion(version);
    }

    public void removeVersion(final Version version) {
        if(version == null)
            throw new IllegalArgumentException("Cannot remove null version");
        versions.remove(version);
        versionsByName.remove(version.getId());

        for(final ReleaseType type : ReleaseType.values())
            if(getLatestVersion(type) == version)
                latestVersions.remove(type);
    }

    public String serializeVersion(final CompleteVersion version) {
        if(version == null)
            throw new IllegalArgumentException("Cannot serialize null!");
        return gson.toJson(version);
    }

    public String serializeVersionList() {
        final RawVersionList list = new RawVersionList();

        for(final ReleaseType type : ReleaseType.values()) {
            final Version latest = getLatestVersion(type);
            if(latest != null)
                list.getLatestVersions().put(type, latest.getId());
        }

        for(final Version version : getVersions()) {
            PartialVersion partial = null;

            if(version instanceof PartialVersion)
                partial = (PartialVersion) version;
            else
                partial = new PartialVersion(version);

            list.getVersions().add(partial);
        }

        return gson.toJson(list);
    }

    public void setLatestVersion(final String name) {
        if(name == null || name.length() == 0)
            throw new IllegalArgumentException("Name cannot be null or empty");
        final Version version = getVersion(name);
        if(version == null)
            throw new IllegalArgumentException("Unknown version - cannot set latest version to null");
        setLatestVersion(version);
    }

    public void setLatestVersion(final Version version) {
        if(version == null)
            throw new IllegalArgumentException("Cannot set latest version to null");
        latestVersions.put(version.getType(), version);
    }
}