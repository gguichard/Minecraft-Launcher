package net.minecraft.launcher.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.ui.bottombar.PlayButtonPanel;
import net.minecraft.launcher.ui.bottombar.PlayerInfoPanel;
import net.minecraft.launcher.ui.bottombar.ProfileSelectionPanel;

public class BottomBarPanel extends JPanel {
    private final Launcher launcher;
    private final ProfileSelectionPanel profileSelectionPanel;
    private final PlayerInfoPanel playerInfoPanel;
    private final PlayButtonPanel playButtonPanel;

    public BottomBarPanel(final Launcher launcher) {
        this.launcher = launcher;

        final int border = 4;
        setBorder(new EmptyBorder(border, border, border, border));

        profileSelectionPanel = new ProfileSelectionPanel(launcher);
        playerInfoPanel = new PlayerInfoPanel(launcher);
        playButtonPanel = new PlayButtonPanel(launcher);

        createInterface();
    }

    protected void createInterface() {
        setLayout(new GridLayout(1, 3));

        add(wrapSidePanel(profileSelectionPanel, 17));
        add(playButtonPanel);
        add(wrapSidePanel(playerInfoPanel, 13));
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public PlayButtonPanel getPlayButtonPanel() {
        return playButtonPanel;
    }

    public PlayerInfoPanel getPlayerInfoPanel() {
        return playerInfoPanel;
    }

    public ProfileSelectionPanel getProfileSelectionPanel() {
        return profileSelectionPanel;
    }

    protected JPanel wrapSidePanel(final JPanel target, final int side) {
        final JPanel wrapper = new JPanel(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = side;
        constraints.weightx = 1.0D;
        constraints.weighty = 1.0D;

        wrapper.add(target, constraints);

        return wrapper;
    }
}