/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.utils;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import java.util.Set;

public class ShellUtils {
    private static final String PREFS_RECENTS = "shell_recents";

    private final SharedPreferences preferences;
    private final Set<String> recents;

    public ShellUtils(@NonNull final SharedPreferences preferences) {
        this.preferences = preferences;

        // Making sure the returned set is not modified.
        // https://developer.android.com/reference/android/content/SharedPreferences#getStringSet(java.lang.String,%20java.util.Set%3Cjava.lang.String%3E)
        final Set<String> recents = preferences.getStringSet(PREFS_RECENTS, new ArraySet<>());
        this.recents = new ArraySet<>(recents);
    }

    /**
     * Adds the command to Recents.
     *
     * @param command the file name.
     */
    public void addRecent(@NonNull final String command) {
        recents.add(command);
        preferences.edit().putStringSet(PREFS_RECENTS, recents).apply();
    }

    /**
     * Returns unordered set of previously added commands.
     *
     * @return A set of commands added using {@link #addRecent(String)}.
     */
    public Set<String> getRecents() {
        return recents;
    }
}
