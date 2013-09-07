package net.minecraft.launcher.profile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.swing.SwingUtilities;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.authentication.AuthenticationDatabase;
import net.minecraft.launcher.events.RefreshedProfilesListener;
import net.minecraft.launcher.updater.DateTypeAdapter;
import net.minecraft.launcher.updater.FileTypeAdapter;
import net.minecraft.launcher.updater.LowerCaseEnumTypeAdapterFactory;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ProfileManager {
    private static class RawProfileList {
        public Map<String, Profile> profiles = new HashMap<String, Profile>();
        public String selectedProfile;
        public UUID clientToken = UUID.randomUUID();
        public AuthenticationDatabase authenticationDatabase = new AuthenticationDatabase();
    }

    private final Launcher launcher;
    private final Gson gson;
    private final Map<String, Profile> profiles = new HashMap<String, Profile>();
    private final File profileFile;
    private final List<RefreshedProfilesListener> refreshedProfilesListeners = Collections.synchronizedList(new ArrayList<RefreshedProfilesListener>());
    private String selectedProfile;

    private AuthenticationDatabase authDatabase = new AuthenticationDatabase();

    public ProfileManager(final Launcher launcher) {
        this.launcher = launcher;
        profileFile = new File(launcher.getWorkingDirectory(), "launcher_profiles.json");

        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.registerTypeAdapter(File.class, new FileTypeAdapter());
        builder.registerTypeAdapter(AuthenticationDatabase.class, new AuthenticationDatabase.Serializer());
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    public void addRefreshedProfilesListener(final RefreshedProfilesListener listener) {
        refreshedProfilesListeners.add(listener);
    }

    public void fireRefreshEvent() {
        final List<RefreshedProfilesListener> listeners = new ArrayList<RefreshedProfilesListener>(refreshedProfilesListeners);
        for(final Iterator<RefreshedProfilesListener> iterator = listeners.iterator(); iterator.hasNext();) {
            final RefreshedProfilesListener listener = iterator.next();

            if(!listener.shouldReceiveEventsInUIThread()) {
                listener.onProfilesRefreshed(this);
                iterator.remove();
            }
        }

        if(!listeners.isEmpty())
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for(final RefreshedProfilesListener listener : listeners)
                        listener.onProfilesRefreshed(ProfileManager.this);
                }
            });
    }

    public AuthenticationDatabase getAuthDatabase() {
        return authDatabase;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public Map<String, Profile> getProfiles() {
        return profiles;
    }

    public Profile getSelectedProfile() {
        if(selectedProfile == null || !profiles.containsKey(selectedProfile))
            if(profiles.get(LauncherConstants.DEFAULT_PROFILE_NAME) != null)
                selectedProfile = LauncherConstants.DEFAULT_PROFILE_NAME;
            else if(profiles.size() > 0)
                selectedProfile = profiles.values().iterator().next().getName();
            else {
                selectedProfile = LauncherConstants.DEFAULT_PROFILE_NAME;
                profiles.put(LauncherConstants.DEFAULT_PROFILE_NAME, new Profile(selectedProfile));
            }

        return profiles.get(selectedProfile);
    }

    public boolean loadProfiles() throws IOException {
        profiles.clear();
        selectedProfile = null;

        if(profileFile.isFile()) {
            final RawProfileList rawProfileList = gson.fromJson(FileUtils.readFileToString(profileFile), RawProfileList.class);

            profiles.putAll(rawProfileList.profiles);
            selectedProfile = rawProfileList.selectedProfile;
            authDatabase = rawProfileList.authenticationDatabase;
            launcher.setClientToken(rawProfileList.clientToken);

            fireRefreshEvent();
            return true;
        }
        fireRefreshEvent();
        return false;
    }

    public void saveProfiles() throws IOException {
        final RawProfileList rawProfileList = new RawProfileList();
        rawProfileList.profiles = profiles;
        rawProfileList.selectedProfile = getSelectedProfile().getName();
        rawProfileList.clientToken = launcher.getClientToken();
        rawProfileList.authenticationDatabase = authDatabase;

        FileUtils.writeStringToFile(profileFile, gson.toJson(rawProfileList));
    }

    public void setSelectedProfile(final String selectedProfile) {
        final boolean update = !this.selectedProfile.equals(selectedProfile);
        this.selectedProfile = selectedProfile;

        if(update)
            fireRefreshEvent();
    }

    public void trimAuthDatabase() {
        final Set<String> uuids = new HashSet<String>(authDatabase.getknownUUIDs());

        for(final Profile profile : profiles.values())
            uuids.remove(profile.getPlayerUUID());

        for(final String uuid : uuids)
            authDatabase.removeUUID(uuid);
    }
}