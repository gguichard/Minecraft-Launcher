package net.minecraft.launcher.ui.popups.profile;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.updater.VersionManager;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.versions.ReleaseType;
import net.minecraft.launcher.versions.Version;

public class ProfileVersionPanel extends JPanel implements RefreshedVersionsListener {
    private static class ReleaseTypeCheckBox extends JCheckBox {
        private final ReleaseType type;

        private ReleaseTypeCheckBox(final ReleaseType type) {
            super();
            this.type = type;
        }

        public ReleaseType getType() {
            return type;
        }
    }

    private static class VersionListRenderer extends BasicComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(final JList list, Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            if(value instanceof VersionSyncInfo) {
                final VersionSyncInfo syncInfo = (VersionSyncInfo) value;
                final Version version = syncInfo.getLatestVersion();

                value = String.format("%s %s", new Object[] { version.getType().getName(), version.getId() });
            }

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return this;
        }
    }

    private final ProfileEditorPopup editor;

    private final JComboBox versionList = new JComboBox();

    private final List<ReleaseTypeCheckBox> customVersionTypes = new ArrayList<ReleaseTypeCheckBox>();

    public ProfileVersionPanel(final ProfileEditorPopup editor) {
        this.editor = editor;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Version Selection"));

        createInterface();
        addEventHandlers();

        final List<VersionSyncInfo> versions = editor.getLauncher().getVersionManager().getVersions(editor.getProfile().getVersionFilter());

        if(versions.isEmpty())
            editor.getLauncher().getVersionManager().addRefreshedVersionsListener(this);
        else
            populateVersions(versions);
    }

    protected void addEventHandlers() {
        versionList.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                ProfileVersionPanel.this.updateVersionSelection();
            }
        });
        for(final ReleaseTypeCheckBox type : customVersionTypes)
            type.addItemListener(new ItemListener() {
                private boolean isUpdating = false;

                public void itemStateChanged(final ItemEvent e) {
                    if(isUpdating)
                        return;
                    if(e.getStateChange() == 1 && type.getType().getPopupWarning() != null) {
                        final int result = JOptionPane.showConfirmDialog(editor.getLauncher().getFrame(), type.getType().getPopupWarning() + "\n\nAre you sure you want to continue?");

                        isUpdating = true;
                        if(result == 0) {
                            type.setSelected(true);
                            ProfileVersionPanel.this.updateCustomVersionFilter();
                        }
                        else
                            type.setSelected(false);
                        isUpdating = false;
                    }
                    else
                        ProfileVersionPanel.this.updateCustomVersionFilter();
                }
            });
    }

    protected void createInterface() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = 17;

        constraints.gridy = 0;

        for(final ReleaseType type : ReleaseType.values())
            if(type.getDescription() != null) {
                final ReleaseTypeCheckBox checkbox = new ReleaseTypeCheckBox(type);
                checkbox.setSelected(editor.getProfile().getVersionFilter().getTypes().contains(type));
                customVersionTypes.add(checkbox);

                constraints.fill = 2;
                constraints.weightx = 1.0D;
                constraints.gridwidth = 0;
                add(checkbox, constraints);
                constraints.gridwidth = 1;
                constraints.weightx = 0.0D;
                constraints.fill = 0;

                constraints.gridy += 1;
            }
        add(new JLabel("Use version:"), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0D;
        add(versionList, constraints);
        constraints.weightx = 0.0D;
        constraints.fill = 0;

        constraints.gridy += 1;

        versionList.setRenderer(new VersionListRenderer());
    }

    public void onVersionsRefreshed(final VersionManager manager) {
        final List<VersionSyncInfo> versions = manager.getVersions(editor.getProfile().getVersionFilter());
        populateVersions(versions);
        editor.getLauncher().getVersionManager().removeRefreshedVersionsListener(this);
    }

    private void populateVersions(final List<VersionSyncInfo> versions) {
        final String previous = editor.getProfile().getLastVersionId();
        VersionSyncInfo selected = null;

        versionList.removeAllItems();
        versionList.addItem("Use Latest Version");

        for(final VersionSyncInfo version : versions) {
            if(version.getLatestVersion().getId().equals(previous))
                selected = version;

            versionList.addItem(version);
        }

        if(selected == null && !versions.isEmpty())
            versionList.setSelectedIndex(0);
        else
            versionList.setSelectedItem(selected);
    }

    public boolean shouldReceiveEventsInUIThread() {
        return true;
    }

    private void updateCustomVersionFilter() {
        final Profile profile = editor.getProfile();
        final Set<ReleaseType> newTypes = new HashSet<ReleaseType>(Profile.DEFAULT_RELEASE_TYPES);

        for(final ReleaseTypeCheckBox type : customVersionTypes)
            if(type.isSelected())
                newTypes.add(type.getType());
            else
                newTypes.remove(type.getType());

        if(newTypes.equals(Profile.DEFAULT_RELEASE_TYPES))
            profile.setAllowedReleaseTypes(null);
        else
            profile.setAllowedReleaseTypes(newTypes);

        populateVersions(editor.getLauncher().getVersionManager().getVersions(editor.getProfile().getVersionFilter()));
        editor.getLauncher().getVersionManager().removeRefreshedVersionsListener(this);
    }

    private void updateVersionSelection() {
        final Object selection = versionList.getSelectedItem();

        if(selection instanceof VersionSyncInfo) {
            final Version version = ((VersionSyncInfo) selection).getLatestVersion();
            editor.getProfile().setLastVersionId(version.getId());
        }
        else
            editor.getProfile().setLastVersionId(null);
    }
}