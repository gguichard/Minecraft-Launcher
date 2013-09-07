package net.minecraft.launcher.ui.tabs;

import java.awt.Component;

import javax.swing.JTabbedPane;

import net.minecraft.launcher.Launcher;

public class LauncherTabPanel extends JTabbedPane {
    private final Launcher launcher;
    private final WebsiteTab blog;
    private final ConsoleTab console;

    public LauncherTabPanel(final Launcher launcher) {
        super(1);

        this.launcher = launcher;
        blog = new WebsiteTab(launcher);
        console = new ConsoleTab(launcher);

        createInterface();
    }

    protected void createInterface() {
        addTab("Update Notes", blog);
        addTab("Development Console", console);
        addTab("Profile Editor", new ProfileListTab(launcher));
        addTab("Local Version Editor (NYI)", new VersionListTab(launcher));
    }

    public WebsiteTab getBlog() {
        return blog;
    }

    public ConsoleTab getConsole() {
        return console;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    protected void removeTab(final Component tab) {
        for(int i = 0; i < getTabCount(); i++)
            if(getTabComponentAt(i) == tab) {
                removeTabAt(i);
                break;
            }
    }

    public void showConsole() {
        setSelectedComponent(console);
    }
}