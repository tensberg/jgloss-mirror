package jgloss.ui;

import java.awt.Component;

public interface PreferencesPanel {
    void savePreferences();
    void loadPreferences();
    void applyPreferences();
    String getTitle();
    Component getComponent();
} // interface PreferencesPanel
