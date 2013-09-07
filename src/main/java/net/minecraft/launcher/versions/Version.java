package net.minecraft.launcher.versions;

import java.util.Date;

public abstract interface Version {
    public abstract String getId();

    public abstract Date getReleaseTime();

    public abstract ReleaseType getType();

    public abstract Date getUpdatedTime();

    public abstract void setReleaseTime(Date paramDate);

    public abstract void setType(ReleaseType paramReleaseType);

    public abstract void setUpdatedTime(Date paramDate);
}