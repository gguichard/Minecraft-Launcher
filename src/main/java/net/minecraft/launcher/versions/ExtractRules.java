package net.minecraft.launcher.versions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtractRules {
    private final List<String> exclude = new ArrayList<String>();

    public ExtractRules() {
    }

    public ExtractRules(final String[] exclude) {
        if(exclude != null)
            Collections.addAll(this.exclude, exclude);
    }

    public List<String> getExcludes() {
        return exclude;
    }

    public boolean shouldExtract(final String path) {
        if(exclude != null)
            for(final String rule : exclude)
                if(path.startsWith(rule))
                    return false;

        return true;
    }
}