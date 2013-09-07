package net.minecraft.launcher.events;

import net.minecraft.launcher.updater.VersionManager;

public abstract interface RefreshedVersionsListener {
    public abstract void onVersionsRefreshed(VersionManager paramVersionManager);

    public abstract boolean shouldReceiveEventsInUIThread();
}