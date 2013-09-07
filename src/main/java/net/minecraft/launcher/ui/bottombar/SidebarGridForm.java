package net.minecraft.launcher.ui.bottombar;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

public abstract class SidebarGridForm extends JPanel {
    protected <T extends Component> T add(final T component, final GridBagConstraints constraints, final int x, final int y, final int weight, final int width) {
        return add(component, constraints, x, y, weight, width, 10);
    }

    protected <T extends Component> T add(final T component, final GridBagConstraints constraints, final int x, final int y, final int weight, final int width, final int anchor) {
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = weight;
        constraints.weighty = 1.0D;
        constraints.gridwidth = width;
        constraints.anchor = anchor;

        add(component, constraints);
        return component;
    }

    protected void createInterface() {
        final GridBagLayout layout = new GridBagLayout();
        final GridBagConstraints constraints = new GridBagConstraints();
        setLayout(layout);

        populateGrid(constraints);
    }

    protected abstract void populateGrid(GridBagConstraints paramGridBagConstraints);
}