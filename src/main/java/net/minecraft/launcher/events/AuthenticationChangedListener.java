package net.minecraft.launcher.events;

import net.minecraft.launcher.authentication.AuthenticationService;

public abstract interface AuthenticationChangedListener {
    public abstract void onAuthenticationChanged(AuthenticationService paramAuthenticationService);

    public abstract boolean shouldReceiveEventsInUIThread();
}