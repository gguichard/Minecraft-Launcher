package net.minecraft.launcher.process;

import java.util.List;

public class JavaProcess {
    private static final int MAX_SYSOUT_LINES = 5;
    private final List<String> commands;
    private final Process process;
    private final LimitedCapacityList<String> sysOutLines = new LimitedCapacityList(String.class, 5);
    private JavaProcessRunnable onExit;
    private final ProcessMonitorThread monitor = new ProcessMonitorThread(this);

    public JavaProcess(final List<String> commands, final Process process) {
        this.commands = commands;
        this.process = process;

        monitor.start();
    }

    public int getExitCode() {
        try {
            return process.exitValue();
        }
        catch(final IllegalThreadStateException ex) {
            ex.fillInStackTrace();
            throw ex;
        }
    }

    public JavaProcessRunnable getExitRunnable() {
        return onExit;
    }

    public Process getRawProcess() {
        return process;
    }

    public String getStartupCommand() {
        return process.toString();
    }

    public List<String> getStartupCommands() {
        return commands;
    }

    public LimitedCapacityList<String> getSysOutLines() {
        return sysOutLines;
    }

    public boolean isRunning() {
        try {
            process.exitValue();
        }
        catch(final IllegalThreadStateException ex) {
            return true;
        }

        return false;
    }

    public void safeSetExitRunnable(final JavaProcessRunnable runnable) {
        setExitRunnable(runnable);

        if(!isRunning() && runnable != null)
            runnable.onJavaProcessEnded(this);
    }

    public void setExitRunnable(final JavaProcessRunnable runnable) {
        onExit = runnable;
    }

    public void stop() {
        process.destroy();
    }

    @Override
    public String toString() {
        return "JavaProcess[commands=" + commands + ", isRunning=" + isRunning() + "]";
    }
}