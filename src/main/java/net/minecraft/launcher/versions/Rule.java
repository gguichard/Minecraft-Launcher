package net.minecraft.launcher.versions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.launcher.OperatingSystem;

public class Rule {
    public static enum Action {
        ALLOW, DISALLOW;
    }

    public class OSRestriction {
        private OperatingSystem name;
        private String version;

        public OSRestriction() {
        }

        public boolean isCurrentOperatingSystem() {
            if(name != null && name != OperatingSystem.getCurrentPlatform())
                return false;

            if(version != null)
                try {
                    final Pattern pattern = Pattern.compile(version);
                    final Matcher matcher = pattern.matcher(System.getProperty("os.version"));
                    if(!matcher.matches())
                        return false;
                }
                catch(final Throwable localThrowable) {
                }
            return true;
        }

        @Override
        public String toString() {
            return "OSRestriction{name=" + name + ", version='" + version + '\'' + '}';
        }
    }

    private final Action action = Action.ALLOW;

    private OSRestriction os;

    public Action getAppliedAction() {
        if(os != null && !os.isCurrentOperatingSystem())
            return null;

        return action;
    }

    @Override
    public String toString() {
        return "Rule{action=" + action + ", os=" + os + '}';
    }
}