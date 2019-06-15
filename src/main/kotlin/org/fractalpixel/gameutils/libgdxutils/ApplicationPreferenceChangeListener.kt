package org.fractalpixel.gameutils.libgdxutils

/**
 * Can be called when the preferences of the application changes, and the application should reload them.
 * Used e.g. by live wallpapers whose settings get changed (the android code should listen to setting changes and forward them).
 */
interface ApplicationPreferenceChangeListener {
    fun onPreferencesChanged()
}