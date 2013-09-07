package net.minecraft.launcher.ui.tabs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.AbstractTableModel;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.authentication.AuthenticationDatabase;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.events.RefreshedProfilesListener;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.ui.popups.profile.ProfileEditorPopup;

public class ProfileListTab extends JScrollPane implements RefreshedProfilesListener {
    private class ProfileTableModel extends AbstractTableModel {
        private final List<Profile> profiles = new ArrayList<Profile>();

        private ProfileTableModel() {
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            return String.class;
        }

        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(final int column) {
            switch(column) {
            case 2:
                return "Username";
            case 1:
                return "Version";
            case 0:
                return "Version name";
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return profiles.size();
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            final Profile profile = profiles.get(rowIndex);
            final AuthenticationDatabase authDatabase = launcher.getProfileManager().getAuthDatabase();
            final AuthenticationService auth = authDatabase.getByUUID(profile.getPlayerUUID());

            switch(columnIndex) {
            case 0:
                return profile.getName();
            case 2:
                if(auth != null && auth.getSelectedProfile() != null)
                    return auth.getSelectedProfile().getName();
                return "(Not logged in)";
            case 1:
                if(profile.getLastVersionId() == null)
                    return "(Latest version)";
                return profile.getLastVersionId();
            }

            return null;
        }

        public void setProfiles(final Collection<Profile> profiles) {
            this.profiles.clear();
            this.profiles.addAll(profiles);
            fireTableDataChanged();
        }
    }

    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_VERSION = 1;
    private static final int COLUMN_AUTHENTICATION = 2;
    private static final int NUM_COLUMNS = 3;
    private final Launcher launcher;
    private final ProfileTableModel dataModel = new ProfileTableModel();
    private final JTable table = new JTable(dataModel);
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final JMenuItem addProfileButton = new JMenuItem("Add Profile");
    private final JMenuItem copyProfileButton = new JMenuItem("Copy Profile");

    private final JMenuItem deleteProfileButton = new JMenuItem("Delete Profile");

    public ProfileListTab(final Launcher launcher) {
        this.launcher = launcher;

        setViewportView(table);
        createInterface();

        launcher.getProfileManager().addRefreshedProfilesListener(this);
    }

    protected void createInterface() {
        popupMenu.add(addProfileButton);
        popupMenu.add(copyProfileButton);
        popupMenu.add(deleteProfileButton);

        table.setFillsViewportHeight(true);
        table.setSelectionMode(0);

        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(final PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                final int[] selection = table.getSelectedRows();
                final boolean hasSelection = selection != null && selection.length > 0;

                copyProfileButton.setEnabled(hasSelection);
                deleteProfileButton.setEnabled(hasSelection);
            }
        });
        addProfileButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Profile profile = new Profile();
                profile.setName("New Profile");

                while(launcher.getProfileManager().getProfiles().containsKey(profile.getName()))
                    profile.setName(profile.getName() + "_");

                ProfileEditorPopup.showEditProfileDialog(getLauncher(), profile);
            }
        });
        copyProfileButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final int selection = table.getSelectedRow();
                if(selection < 0 || selection >= table.getRowCount())
                    return;

                final Profile current = dataModel.profiles.get(selection);
                final Profile copy = new Profile(current);
                copy.setName("Copy of " + current.getName());

                while(launcher.getProfileManager().getProfiles().containsKey(copy.getName()))
                    copy.setName(copy.getName() + "_");

                ProfileEditorPopup.showEditProfileDialog(getLauncher(), copy);
            }
        });
        deleteProfileButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final int selection = table.getSelectedRow();
                if(selection < 0 || selection >= table.getRowCount())
                    return;

                final Profile current = dataModel.profiles.get(selection);

                final int result = JOptionPane.showOptionDialog(launcher.getFrame(), "Are you sure you want to delete this profile?", "Profile Confirmation", 0, 2, null, new String[] { "Delete profile", "Cancel" }, "Delete profile");

                if(result == 0) {
                    launcher.getProfileManager().getProfiles().remove(current.getName());
                    try {
                        launcher.getProfileManager().saveProfiles();
                        launcher.getProfileManager().fireRefreshEvent();
                    }
                    catch(final IOException ex) {
                        launcher.println("Couldn't save profiles whilst deleting '" + current.getName() + "'", ex);
                    }
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if(e.getClickCount() == 2) {
                    final int row = table.getSelectedRow();

                    if(row >= 0 && row < dataModel.profiles.size())
                        ProfileEditorPopup.showEditProfileDialog(getLauncher(), dataModel.profiles.get(row));
                }
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                if(e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    final int r = table.rowAtPoint(e.getPoint());
                    if(r >= 0 && r < table.getRowCount())
                        table.setRowSelectionInterval(r, r);
                    else
                        table.clearSelection();

                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                if(e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    final int r = table.rowAtPoint(e.getPoint());
                    if(r >= 0 && r < table.getRowCount())
                        table.setRowSelectionInterval(r, r);
                    else
                        table.clearSelection();

                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public void onProfilesRefreshed(final ProfileManager manager) {
        dataModel.setProfiles(manager.getProfiles().values());
    }

    public boolean shouldReceiveEventsInUIThread() {
        return true;
    }
}