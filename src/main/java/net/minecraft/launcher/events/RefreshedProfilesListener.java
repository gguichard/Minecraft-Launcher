package net.minecraft.launcher.events;

import net.minecraft.launcher.profile.ProfileManager;

public abstract interface RefreshedProfilesListener {
    public abstract void onProfilesRefreshed(ProfileManager paramProfileManager);

    public abstract boolean shouldReceiveEventsInUIThread();
}