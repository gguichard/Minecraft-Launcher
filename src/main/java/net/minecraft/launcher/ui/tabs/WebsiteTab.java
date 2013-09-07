package net.minecraft.launcher.ui.tabs;

import java.awt.Color;
import java.net.URL;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.OperatingSystem;

public class WebsiteTab extends JScrollPane {
    private final JTextPane blog = new JTextPane();
    private final Launcher launcher;

    public WebsiteTab(final Launcher launcher) {
        this.launcher = launcher;

        blog.setEditable(false);
        blog.setMargin(null);
        blog.setBackground(Color.DARK_GRAY);
        blog.setContentType("text/html");
        blog.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center><h1>Loading page..</h1></center></font></body></html>");
        blog.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(final HyperlinkEvent he) {
                if(he.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                    try {
                        OperatingSystem.openLink(he.getURL().toURI());
                    }
                    catch(final Exception e) {
                        Launcher.getInstance().println("Unexpected exception opening link " + he.getURL(), e);
                    }
            }
        });
        setViewportView(blog);
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public void setPage(final String url) {
        final Thread thread = new Thread("Update website tab") {
            @Override
            public void run() {
                try {
                    blog.setPage(new URL(url));
                }
                catch(final Exception e) {
                    Launcher.getInstance().println("Unexpected exception loading " + url, e);
                    blog.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center><h1>Failed to get page</h1><br>" + e.toString() + "</center></font></body></html>");
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }
}