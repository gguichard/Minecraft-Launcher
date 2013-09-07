package net.minecraft.launcher.ui.popups.login;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class AuthErrorForm extends JPanel {
    private final LogInPopup popup;
    private final JLabel errorLabel = new JLabel();

    public AuthErrorForm(final LogInPopup popup) {
        this.popup = popup;

        createInterface();
        clear();
    }

    public void clear() {
        setVisible(false);
    }

    protected void createInterface() {
        setBorder(new EmptyBorder(0, 0, 15, 0));
        errorLabel.setFont(errorLabel.getFont().deriveFont(1));
        add(errorLabel);
    }

    public void displayError(final String[] lines) {
        if(SwingUtilities.isEventDispatchThread()) {
            String error = "";
            for(final String line : lines)
                error = error + "<p>" + line + "</p>";
            errorLabel.setText("<html><div style='text-align: center;'>" + error + " </div></html>");
            setVisible(true);
        }
        else
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    displayError(lines);
                }
            });
    }

    @Override
    public void setVisible(final boolean value) {
        super.setVisible(value);
        popup.repack();
    }
}