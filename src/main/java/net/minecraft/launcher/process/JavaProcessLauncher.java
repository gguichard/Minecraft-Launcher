package net.minecraft.launcher.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.launcher.OperatingSystem;

public class JavaProcessLauncher {
    private final String jvmPath;
    private final List<String> commands;
    private File directory;

    public JavaProcessLauncher(String jvmPath, final String[] commands) {
        if(jvmPath == null)
            jvmPath = OperatingSystem.getCurrentPlatform().getJavaDir();
        this.jvmPath = jvmPath;
        this.commands = new ArrayList<String>(commands.length);
        addCommands(commands);
    }

    public void addCommands(final String[] commands) {
        this.commands.addAll(Arrays.asList(commands));
    }

    public void addSplitCommands(final String commands) {
        addCommands(commands.split(" "));
    }

    public JavaProcessLauncher directory(final File directory) {
        this.directory = directory;

        return this;
    }

    public List<String> getCommands() {
        return commands;
    }

    public File getDirectory() {
        return directory;
    }

    public List<String> getFullCommands() {
        final List<String> result = new ArrayList<String>(commands);
        result.add(0, getJavaPath());
        return result;
    }

    protected String getJavaPath() {
        return jvmPath;
    }

    public JavaProcess start() throws IOException {
        final List<String> full = getFullCommands();
        return new JavaProcess(full, new ProcessBuilder(full).directory(directory).redirectErrorStream(true).start());
    }

    @Override
    public String toString() {
        return "JavaProcessLauncher[commands=" + commands + ", java=" + jvmPath + "]";
    }
}