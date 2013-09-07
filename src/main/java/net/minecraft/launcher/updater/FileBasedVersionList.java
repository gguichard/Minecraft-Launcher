package net.minecraft.launcher.updater;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public abstract class FileBasedVersionList extends VersionList {
    @Override
    protected String getContent(final String path) throws IOException {
        return IOUtils.toString(getFileInputStream(path)).replaceAll("\\r\\n", "\r").replaceAll("\\r", "\n");
    }

    protected abstract InputStream getFileInputStream(String paramString) throws FileNotFoundException;
}