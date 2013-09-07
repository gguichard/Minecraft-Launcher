package net.minecraft.launcher.updater;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.updater.download.DownloadJob;
import net.minecraft.launcher.updater.download.Downloadable;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.ReleaseType;
import net.minecraft.launcher.versions.Version;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class VersionManager {
    private final VersionList localVersionList;
    private final VersionList remoteVersionList;
    private final ThreadPoolExecutor executorService = new ExceptionalThreadPoolExecutor(8);
    private final List<RefreshedVersionsListener> refreshedVersionsListeners = Collections.synchronizedList(new ArrayList<RefreshedVersionsListener>());
    private final Object refreshLock = new Object();
    private boolean isRefreshing;

    public VersionManager(final VersionList localVersionList, final VersionList remoteVersionList) {
        this.localVersionList = localVersionList;
        this.remoteVersionList = remoteVersionList;
    }

    public void addRefreshedVersionsListener(final RefreshedVersionsListener listener) {
        refreshedVersionsListeners.add(listener);
    }

    public DownloadJob downloadResources(final DownloadJob job) throws IOException {
        final File baseDirectory = ((LocalVersionList) localVersionList).getBaseDirectory();

        job.addDownloadables(getResourceFiles(((RemoteVersionList) remoteVersionList).getProxy(), baseDirectory));

        return job;
    }

    public DownloadJob downloadVersion(final VersionSyncInfo syncInfo, final DownloadJob job) throws IOException {
        if(!(localVersionList instanceof LocalVersionList))
            throw new IllegalArgumentException("Cannot download if local repo isn't a LocalVersionList");
        if(!(remoteVersionList instanceof RemoteVersionList))
            throw new IllegalArgumentException("Cannot download if local repo isn't a RemoteVersionList");
        final CompleteVersion version = getLatestCompleteVersion(syncInfo);
        final File baseDirectory = ((LocalVersionList) localVersionList).getBaseDirectory();
        final Proxy proxy = ((RemoteVersionList) remoteVersionList).getProxy();

        job.addDownloadables(version.getRequiredDownloadables(OperatingSystem.getCurrentPlatform(), proxy, baseDirectory, false));

        final String jarFile = "versions/" + version.getId() + "/" + version.getId() + ".jar";
        job.addDownloadables(new Downloadable[] { new Downloadable(proxy, new URL(LauncherConstants.URL_DOWNLOAD_BASE + jarFile), new File(baseDirectory, jarFile), false) });

        return job;
    }

    public ThreadPoolExecutor getExecutorService() {
        return executorService;
    }

    public List<VersionSyncInfo> getInstalledVersions() {
        final List<VersionSyncInfo> result = new ArrayList<VersionSyncInfo>();

        for(final Version version : localVersionList.getVersions())
            if(version.getType() != null && version.getUpdatedTime() != null) {
                final VersionSyncInfo syncInfo = getVersionSyncInfo(version, remoteVersionList.getVersion(version.getId()));
                result.add(syncInfo);
            }
        return result;
    }

    public CompleteVersion getLatestCompleteVersion(final VersionSyncInfo syncInfo) throws IOException {
        if(syncInfo.getLatestSource() == VersionSyncInfo.VersionSource.REMOTE) {
            CompleteVersion result = null;
            IOException exception = null;
            try {
                result = remoteVersionList.getCompleteVersion(syncInfo.getLatestVersion());
            }
            catch(final IOException e) {
                exception = e;
                try {
                    result = localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
                }
                catch(final IOException localIOException1) {
                }
            }
            if(result != null)
                return result;
            throw exception;
        }

        return localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
    }

    public VersionList getLocalVersionList() {
        return localVersionList;
    }

    public VersionList getRemoteVersionList() {
        return remoteVersionList;
    }

    private Set<Downloadable> getResourceFiles(final Proxy proxy, final File baseDirectory) {
        final Set<Downloadable> result = new HashSet<Downloadable>();
        try {
            final URL resourceUrl = new URL(LauncherConstants.URL_RESOURCE_BASE);
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.parse(resourceUrl.openConnection(proxy).getInputStream());
            final NodeList nodeLst = doc.getElementsByTagName("Contents");

            final long start = System.nanoTime();
            for(int i = 0; i < nodeLst.getLength(); i++) {
                final Node node = nodeLst.item(i);

                if(node.getNodeType() == 1) {
                    final Element element = (Element) node;
                    final String key = element.getElementsByTagName("Key").item(0).getChildNodes().item(0).getNodeValue();
                    String etag = element.getElementsByTagName("ETag") != null ? element.getElementsByTagName("ETag").item(0).getChildNodes().item(0).getNodeValue() : "-";
                    final long size = Long.parseLong(element.getElementsByTagName("Size").item(0).getChildNodes().item(0).getNodeValue());

                    if(size > 0L) {
                        final File file = new File(baseDirectory, "assets/" + key);
                        if(etag.length() > 1) {
                            etag = Downloadable.getEtag(etag);
                            if(file.isFile() && file.length() == size) {
                                final String localMd5 = Downloadable.getMD5(file);
                                if(localMd5.equals(etag))
                                    continue;
                            }
                        }
                        final Downloadable downloadable = new Downloadable(proxy, new URL(LauncherConstants.URL_RESOURCE_BASE + key), file, false);
                        downloadable.setExpectedSize(size);
                        result.add(downloadable);
                    }
                }
            }
            final long end = System.nanoTime();
            final long delta = end - start;
            Launcher.getInstance().println("Delta time to compare resources: " + delta / 1000000L + " ms ");
        }
        catch(final Exception ex) {
            Launcher.getInstance().println("Couldn't download resources", ex);
        }

        return result;
    }

    public List<VersionSyncInfo> getVersions() {
        return getVersions(null);
    }

    public List<VersionSyncInfo> getVersions(final VersionFilter filter) {
        synchronized(refreshLock) {
            if(isRefreshing)
                return new ArrayList<VersionSyncInfo>();
        }

        final List<VersionSyncInfo> result = new ArrayList<VersionSyncInfo>();
        final Map<String, VersionSyncInfo> lookup = new HashMap<String, VersionSyncInfo>();
        final Map<ReleaseType, Integer> counts = new EnumMap<ReleaseType, Integer>(ReleaseType.class);

        for(final ReleaseType type : ReleaseType.values())
            counts.put(type, Integer.valueOf(0));

        for(final Version version : localVersionList.getVersions())
            if(version.getType() != null && version.getUpdatedTime() != null && (filter == null || filter.getTypes().contains(version.getType()) && counts.get(version.getType()).intValue() < filter.getMaxCount())) {
                final VersionSyncInfo syncInfo = getVersionSyncInfo(version, remoteVersionList.getVersion(version.getId()));
                lookup.put(version.getId(), syncInfo);
                result.add(syncInfo);
            }
        for(final Version version : remoteVersionList.getVersions())
            if(version.getType() != null && version.getUpdatedTime() != null && !lookup.containsKey(version.getId()) && (filter == null || filter.getTypes().contains(version.getType()) && counts.get(version.getType()).intValue() < filter.getMaxCount())) {
                final VersionSyncInfo syncInfo = getVersionSyncInfo(localVersionList.getVersion(version.getId()), version);
                lookup.put(version.getId(), syncInfo);
                result.add(syncInfo);

                if(filter != null)
                    counts.put(version.getType(), Integer.valueOf(counts.get(version.getType()).intValue() + 1));
            }
        if(result.isEmpty())
            for(final Version version : localVersionList.getVersions())
                if(version.getType() != null && version.getUpdatedTime() != null) {
                    final VersionSyncInfo syncInfo = getVersionSyncInfo(version, remoteVersionList.getVersion(version.getId()));
                    lookup.put(version.getId(), syncInfo);
                    result.add(syncInfo);
                }

        Collections.sort(result, new Comparator<VersionSyncInfo>() {
            public int compare(final VersionSyncInfo a, final VersionSyncInfo b) {
                final Version aVer = a.getLatestVersion();
                final Version bVer = b.getLatestVersion();

                if(aVer.getReleaseTime() != null && bVer.getReleaseTime() != null)
                    return bVer.getReleaseTime().compareTo(aVer.getReleaseTime());
                return bVer.getUpdatedTime().compareTo(aVer.getUpdatedTime());
            }
        });
        return result;
    }

    public VersionSyncInfo getVersionSyncInfo(final String name) {
        return getVersionSyncInfo(localVersionList.getVersion(name), remoteVersionList.getVersion(name));
    }

    public VersionSyncInfo getVersionSyncInfo(final Version version) {
        return getVersionSyncInfo(version.getId());
    }

    public VersionSyncInfo getVersionSyncInfo(final Version localVersion, final Version remoteVersion) {
        final boolean installed = localVersion != null;
        boolean upToDate = installed;

        if(installed && remoteVersion != null)
            upToDate = !remoteVersion.getUpdatedTime().after(localVersion.getUpdatedTime());
        if(localVersion instanceof CompleteVersion)
            upToDate &= localVersionList.hasAllFiles((CompleteVersion) localVersion, OperatingSystem.getCurrentPlatform());

        return new VersionSyncInfo(localVersion, remoteVersion, installed, upToDate);
    }

    public void refreshVersions() throws IOException {
        synchronized(refreshLock) {
            isRefreshing = true;
        }
        try {
            Launcher.getInstance().println("Refreshing local version list...");
            localVersionList.refreshVersions();
            Launcher.getInstance().println("Refreshing remote version list...");
            remoteVersionList.refreshVersions();
        }
        catch(final IOException ex) {
            synchronized(refreshLock) {
                isRefreshing = false;
            }
            throw ex;
        }

        Launcher.getInstance().println("Refresh complete.");

        synchronized(refreshLock) {
            isRefreshing = false;
        }

        final List<RefreshedVersionsListener> listeners = new ArrayList<RefreshedVersionsListener>(refreshedVersionsListeners);
        for(final Iterator<RefreshedVersionsListener> iterator = listeners.iterator(); iterator.hasNext();) {
            final RefreshedVersionsListener listener = iterator.next();

            if(!listener.shouldReceiveEventsInUIThread()) {
                listener.onVersionsRefreshed(this);
                iterator.remove();
            }
        }

        if(!listeners.isEmpty())
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for(final RefreshedVersionsListener listener : listeners)
                        listener.onVersionsRefreshed(VersionManager.this);
                }
            });
    }

    public void removeRefreshedVersionsListener(final RefreshedVersionsListener listener) {
        refreshedVersionsListeners.remove(listener);
    }
}