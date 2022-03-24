/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.utils;

import android.content.SharedPreferences;

import java.util.Set;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

public class ShellUtils {
    private static final String PREFS_RECENTS = "shell_recents";

    private final SharedPreferences preferences;
    private final Set<String> recents;

    public ShellUtils(@NonNull final SharedPreferences preferences) {
        this.preferences = preferences;

        this.recents = preferences.getStringSet(PREFS_RECENTS, new ArraySet<>());
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
