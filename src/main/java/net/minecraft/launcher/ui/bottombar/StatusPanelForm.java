package net.minecraft.launcher.ui.bottombar;

import java.awt.GridBagConstraints;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;

import net.minecraft.launcher.Http;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.updater.LowerCaseEnumTypeAdapterFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class StatusPanelForm extends SidebarGridForm {
    public static enum ServerStatus {
        GREEN("Online, no problems detected."), YELLOW("May be experiencing issues."), RED("Offline, experiencing problems.");

        private final String title;

        private ServerStatus(final String title) {
            this.title = title;
        }
    }

    private static final String SERVER_SESSION = "session.minecraft.net";
    private static final String SERVER_LOGIN = "login.minecraft.net";
    private final Launcher launcher;
    private final JLabel sessionStatus = new JLabel("???");
    private final JLabel loginStatus = new JLabel("???");

    private final Gson gson = new GsonBuilder().registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory()).create();

    public StatusPanelForm(final Launcher launcher) {
        this.launcher = launcher;

        createInterface();
        refreshStatuses();
    }

    public JLabel getLoginStatus() {
        return loginStatus;
    }

    public JLabel getSessionStatus() {
        return sessionStatus;
    }

    @Override
    protected void populateGrid(final GridBagConstraints constraints) {
        add(new JLabel("Multiplayer:", 2), constraints, 0, 0, 0, 1, 17);
        add(sessionStatus, constraints, 1, 0, 1, 1);

        add(new JLabel("Login:", 2), constraints, 0, 1, 0, 1, 17);
        add(loginStatus, constraints, 1, 1, 1, 1);
    }

    public void refreshStatuses() {
        launcher.getVersionManager().getExecutorService().submit(new Runnable() {
            public void run() {
                try {
                    final TypeToken<Type> token = new TypeToken<Type>() {
                    };
                    final Map<String, String> statuses = gson.fromJson(Http.performGet(new URL(LauncherConstants.URL_STATUS_CHECKER), launcher.getProxy()), token.getType());

                    for(final Entry<String, String> serverStatusInformation : statuses.entrySet())
                        if(serverStatusInformation.getKey().equals("login.minecraft.net"))
                            loginStatus.setText(serverStatusInformation.getValue());
                        else if(serverStatusInformation.getKey().equals("session.minecraft.net"))
                            sessionStatus.setText(serverStatusInformation.getValue());
                }
                catch(final Exception e) {
                    Launcher.getInstance().println("Couldn't get server status", e);
                }
            }
        });
    }
}