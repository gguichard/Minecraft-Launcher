package net.minecraft.launcher.versions;

import java.util.Date;

public class PartialVersion implements Version {
    private String id;
    private Date time;
    private Date releaseTime;
    private ReleaseType type;

    public PartialVersion() {
    }

    public PartialVersion(final String id, final Date releaseTime, final Date updateTime, final ReleaseType type) {
        if(id == null || id.length() == 0)
            throw new IllegalArgumentException("ID cannot be null or empty");
        if(releaseTime == null)
            throw new IllegalArgumentException("Release time cannot be null");
        if(updateTime == null)
            throw new IllegalArgumentException("Update time cannot be null");
        if(type == null)
            throw new IllegalArgumentException("Release type cannot be null");
        this.id = id;
        this.releaseTime = releaseTime;
        time = updateTime;
        this.type = type;
    }

    public PartialVersion(final Version version) {
        this(version.getId(), version.getReleaseTime(), version.getUpdatedTime(), version.getType());
    }

    public String getId() {
        return id;
    }

    public Date getReleaseTime() {
        return releaseTime;
    }

    public ReleaseType getType() {
        return type;
    }

    public Date getUpdatedTime() {
        return time;
    }

    public void setReleaseTime(final Date time) {
        if(time == null)
            throw new IllegalArgumentException("Time cannot be null");
        releaseTime = time;
    }

    public void setType(final ReleaseType type) {
        if(type == null)
            throw new IllegalArgumentException("Release type cannot be null");
        this.type = type;
    }

    public void setUpdatedTime(final Date time) {
        if(time == null)
            throw new IllegalArgumentException("Time cannot be null");
        this.time = time;
    }

    @Override
    public String toString() {
        return "PartialVersion{id='" + id + '\'' + ", updateTime=" + time + ", releaseTime=" + releaseTime + ", type=" + type + '}';
    }
}