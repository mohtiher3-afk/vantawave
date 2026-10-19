# Theme Customization Implementation Plan

Extend the VantaWave theme to support dynamic accent color presets (Neon Cyan, Electric Lime, Sunset Orange) that update instantly and persist across sessions, integrated into the existing Settings dialog.

---

### Implementation Plan

1.  **Define Accent Presets (`Theme.kt` / `Color.kt`)**:
    -   Create a `ThemeAccent` enum representing the presets with their associated `primary` and `secondary` colors.
2.  **State Management (`MainViewModel.kt`)**:
    -   Expose a `MutableStateFlow<ThemeAccent>` to manage the active theme.
    -   Persist the selected accent using `DataStore` (or existing settings preference logic) for automatic saving.
3.  **Theme Update (`Theme.kt`)**:
    -   Update `VantaWaveTheme` to accept the selected `ThemeAccent` and dynamically configure the Material 3 `ColorScheme`.
4.  **Settings UI Integration (`SettingsDialog.kt`)**:
    -   Add a new section in the `SettingsDialog` with a set of selectable chips/buttons representing the available accent presets.
    -   Update the active accent in the ViewModel when a user selects a preset.

---

### Technical Steps

1.  **Update `Color.kt` & `Theme.kt`**: Introduce the new colors and theme logic.
2.  **ViewModel Integration**: Add logic to handle accent persistence.
3.  **UI Update**: Implement the accent selector in `SettingsDialog.kt`.
4.  **Verification**: Ensure all components (player controls, visualizer) react to the new theme accent colors.
