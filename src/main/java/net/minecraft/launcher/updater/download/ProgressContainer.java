package net.minecraft.launcher.updater.download;

public class ProgressContainer {
    private long total;
    private long current;
    private DownloadJob job;

    public void addProgress(final long amount) {
        setCurrent(getCurrent() + amount);
    }

    public long getCurrent() {
        return current;
    }

    public DownloadJob getJob() {
        return job;
    }

    public float getProgress() {
        if(total == 0L)
            return 0.0F;
        return (float) current / (float) total;
    }

    public long getTotal() {
        return total;
    }

    public void setCurrent(final long current) {
        this.current = current;
        if(current > total)
            total = current;
        if(job != null)
            job.updateProgress();
    }

    public void setJob(final DownloadJob job) {
        this.job = job;
        if(job != null)
            job.updateProgress();
    }

    public void setTotal(final long total) {
        this.total = total;
        if(job != null)
            job.updateProgress();
    }
}