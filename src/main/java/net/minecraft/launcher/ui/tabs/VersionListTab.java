package net.minecraft.launcher.ui.tabs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.updater.VersionManager;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.Version;

public class VersionListTab extends JScrollPane implements RefreshedVersionsListener {
    private class VersionTableModel extends AbstractTableModel {
        private final List<Version> versions = new ArrayList<Version>();

        private VersionTableModel() {
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            if(columnIndex == 3 || columnIndex == 2)
                return Date.class;

            return String.class;
        }

        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(final int column) {
            switch(column) {
            case 3:
                return "Last modified";
            case 1:
                return "Version type";
            case 4:
                return "Library count";
            case 0:
                return "Version name";
            case 5:
                return "Sync status";
            case 2:
                return "Release Date";
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return versions.size();
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            final Version version = versions.get(rowIndex);

            switch(columnIndex) {
            case 0:
                return version.getId();
            case 3:
                return version.getUpdatedTime();
            case 4:
                if(version instanceof CompleteVersion) {
                    final CompleteVersion complete = (CompleteVersion) version;
                    final int total = complete.getLibraries().size();
                    final int relevant = complete.getRelevantLibraries().size();
                    if(total == relevant)
                        return Integer.valueOf(total);
                    return String.format("%d (%d relevant to %s)", new Object[] { Integer.valueOf(total), Integer.valueOf(relevant), OperatingSystem.getCurrentPlatform().getName() });
                }

                return "?";
            case 5:
                final VersionSyncInfo syncInfo = launcher.getVersionManager().getVersionSyncInfo(version);
                if(syncInfo.isOnRemote()) {
                    if(syncInfo.isUpToDate())
                        return "Up to date with remote";
                    return "Update avail from remote";
                }

                return "Local only";
            case 1:
                return version.getType().getName();
            case 2:
                return version.getReleaseTime();
            }

            return null;
        }

        public void setVersions(final Collection<Version> versions) {
            this.versions.clear();
            this.versions.addAll(versions);
            fireTableDataChanged();
        }
    }

    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_TYPE = 1;
    private static final int COLUMN_RELEASE_DATE = 2;
    private static final int COLUMN_UPDATE_DATE = 3;
    private static final int COLUMN_LIBRARIES = 4;
    private static final int COLUMN_STATUS = 5;
    private static final int NUM_COLUMNS = 6;
    private final Launcher launcher;
    private final VersionTableModel dataModel = new VersionTableModel();

    private final JTable table = new JTable(dataModel);

    public VersionListTab(final Launcher launcher) {
        this.launcher = launcher;

        setViewportView(table);
        createInterface();

        launcher.getVersionManager().addRefreshedVersionsListener(this);
    }

    protected void createInterface() {
        table.setFillsViewportHeight(true);
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public void onVersionsRefreshed(final VersionManager manager) {
        dataModel.setVersions(manager.getLocalVersionList().getVersions());
    }

    public boolean shouldReceiveEventsInUIThread() {
        return true;
    }
}