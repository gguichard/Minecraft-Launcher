package net.minecraft.launcher.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.ui.tabs.LauncherTabPanel;

public class LauncherPanel extends JPanel {
    public static final String CARD_DIRT_BACKGROUND = "loading";
    public static final String CARD_LOGIN = "login";
    public static final String CARD_LAUNCHER = "launcher";
    private final CardLayout cardLayout;
    private final LauncherTabPanel tabPanel;
    private final BottomBarPanel bottomBar;
    private final JProgressBar progressBar;
    private final Launcher launcher;
    private final JPanel loginPanel;

    public LauncherPanel(final Launcher launcher) {
        this.launcher = launcher;
        cardLayout = new CardLayout();
        setLayout(cardLayout);

        progressBar = new JProgressBar();
        bottomBar = new BottomBarPanel(launcher);
        tabPanel = new LauncherTabPanel(launcher);
        loginPanel = new TexturedPanel("/dirt.png");
        createInterface();
    }

    protected JPanel createDirtInterface() {
        return new TexturedPanel("/dirt.png");
    }

    protected void createInterface() {
        add(createLauncherInterface(), "launcher");
        add(createDirtInterface(), "loading");
        add(createLoginInterface(), "login");
    }

    protected JPanel createLauncherInterface() {
        final JPanel result = new JPanel(new BorderLayout());

        tabPanel.getBlog().setPage(LauncherConstants.URL_BLOG);

        final JPanel topWrapper = new JPanel();
        topWrapper.setLayout(new BorderLayout());
        topWrapper.add(tabPanel, "Center");
        topWrapper.add(progressBar, "South");

        progressBar.setVisible(false);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);

        result.add(topWrapper, "Center");
        result.add(bottomBar, "South");

        return result;
    }

    protected JPanel createLoginInterface() {
        loginPanel.setLayout(new GridBagLayout());
        return loginPanel;
    }

    public BottomBarPanel getBottomBar() {
        return bottomBar;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public LauncherTabPanel getTabPanel() {
        return tabPanel;
    }

    public void setCard(final String card, final JPanel additional) {
        if(card.equals("login")) {
            loginPanel.removeAll();
            loginPanel.add(additional);
        }
        cardLayout.show(this, card);
    }
}