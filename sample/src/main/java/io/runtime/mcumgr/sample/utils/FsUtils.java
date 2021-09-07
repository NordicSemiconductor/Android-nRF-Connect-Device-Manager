/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.utils;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import java.util.Set;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class FsUtils {
    private static final String PREFS_RECENTS = "recents";
    private static final String PREFS_PARTITION = "partition";
    private static final String PARTITION_DEFAULT = "lfs";

    private final SharedPreferences mPreferences;
    private final MutableLiveData<String> mPartitionLiveData = new MutableLiveData<>();
    private final Set<String> mRecents;

    public FsUtils(@NonNull final SharedPreferences preferences) {
        mPreferences = preferences;
        mPartitionLiveData.setValue(getPartitionString());

        mRecents = preferences.getStringSet(PREFS_RECENTS, new ArraySet<>());
    }

    /**
     * Returns a LiveData object that can observe changes of the partition.
     *
     * @return Observable partition.
     */
    @NonNull
    public LiveData<String> getPartition() {
        return mPartitionLiveData;
    }

    /**
     * Returns the default partition ({@value #PARTITION_DEFAULT}).
     *
     * @return The default partition name.
     */
    @NonNull
    public String getDefaultPartition() {
        return PARTITION_DEFAULT;
    }

    /**
     * Returns the current value of the partition name.
     *
     * @return The partition name.
     */
    @NonNull
    public String getPartitionString() {
        return mPreferences.getString(PREFS_PARTITION, PARTITION_DEFAULT);
    }

    /**
     * Sets the partition name. The observable LiveData will be notified.
     *
     * @param partition The new partition name.
     */
    public void setPartition(@NonNull final String partition) {
        mPreferences.edit().putString(PREFS_PARTITION, partition).apply();
        mPartitionLiveData.postValue(partition);
    }

    /**
     * Adds the file name to Recents.
     *
     * @param fileName the file name.
     */
    public void addRecent(@NonNull final String fileName) {
        mRecents.add(fileName);
        mPreferences.edit().putStringSet(PREFS_RECENTS, mRecents).apply();
    }

    /**
     * Returns unordered set of previously added file names.
     *
     * @return A set of file names added using {@link #addRecent(String)}.
     */
    public Set<String> getRecents() {
        return mRecents;
    }

    /**
     * Tries to create a Bitmap from the given data.
     *
     * @param resources the resources.
     * @param data      the byte array to be decoded.
     * @return A decoded bitmap, or null, if data do not contain an image.
     */
    public static Bitmap toBitmap(@NonNull final Resources resources, @NonNull final byte[] data) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
        options.inTargetDensity = resources.getDisplayMetrics().densityDpi;
        options.inScaled = true;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    /**
     * Generates a sample data with Lorem Ipsum text of given length.
     *
     * @param size the required output length.
     * @return Lorem Ipsum text of given length as byte array.
     */
    public static byte[] generateLoremIpsum(int size) {
        final StringBuilder builder = new StringBuilder();
        while (size > 0) {
            final int chunkSize = Math.min(LOREM.length(), size);
            builder.append(LOREM.substring(0, chunkSize));
            size -= chunkSize;
        }
        return builder.toString().getBytes();
    }

    /**
     * Lorem ipsum dolor sit amet, consectetur adipiscing elit.
     */
    private static final String LOREM =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque ex ante, semper ut " +
                    "faucibus pharetra, accumsan et augue. Vestibulum vulputate elit ligula, eu " +
                    "tincidunt orci lacinia quis. Proin scelerisque dui at magna placerat, id " +
                    "blandit felis vehicula. Maecenas ac nisl a odio varius condimentum luctus " +
                    "eget lorem. In non leo quis lorem faucibus pretium a sit amet sem. Aliquam " +
                    "quis mi ultrices, scelerisque arcu id, posuere neque. Nunc quis est " +
                    "efficitur, vehicula augue eget, imperdiet diam. In vitae fringilla leo. " +
                    "Sed rhoncus porttitor nunc ac lobortis. Ut quis quam urna. Curabitur " +
                    "laoreet odio risus, non pretium orci sodales sed. Vivamus eleifend accumsan " +
                    "dolor, sed tincidunt erat. Nullam sed arcu maximus, vehicula diam gravida, " +
                    "ullamcorper ligula. Donec finibus odio a vestibulum mollis. Quisque non " +
                    "metus ut justo eleifend vulputate.\n\n" +
                    "Etiam iaculis magna non bibendum eleifend. Morbi sodales lorem eros, a " +
                    "eleifend nibh accumsan vel. Integer quis ex feugiat massa venenatis " +
                    "pulvinar sed at turpis. In et orci eget mi efficitur aliquet et non odio. " +
                    "In pellentesque imperdiet convallis. Aliquam tincidunt at augue eleifend " +
                    "elementum. Sed lacinia efficitur tincidunt. Donec ac pharetra nisi, eget " +
                    "sodales tortor. Nulla imperdiet mi id mattis pharetra.\n\n" +
                    "Integer nec pretium ligula. Mauris venenatis, neque eget luctus molestie, " +
                    "neque dolor laoreet leo, ut porta felis velit a enim. Vestibulum id " +
                    "finibus enim, sit amet ullamcorper augue. Maecenas a orci non lacus " +
                    "euismod egestas. Vestibulum volutpat urna sed neque malesuada, sit amet " +
                    "pellentesque ante congue. Phasellus suscipit pellentesque felis et " +
                    "sagittis. Proin gravida ante a imperdiet suscipit. Fusce sit amet euismod " +
                    "dolor, id rhoncus mauris. Phasellus convallis ornare accumsan. Quisque " +
                    "non diam non risus rhoncus congue vel id quam. Etiam risus lacus, egestas " +
                    "a dignissim in, rhoncus in massa.\n\n" +
                    "Sed lacinia mauris neque, sed lacinia dui vehicula et. Donec sit amet " +
                    "convallis enim, eget luctus mi. Nunc elementum consequat arcu non " +
                    "condimentum. In vehicula tempus libero, quis egestas neque scelerisque ac. " +
                    "Nulla tortor leo, volutpat facilisis sagittis nec, dignissim eu tellus. " +
                    "Orci varius natoque penatibus et magnis dis parturient montes, nascetur " +
                    "ridiculus mus. Sed molestie vel est id molestie. Ut id risus ullamcorper, " +
                    "cursus mi eu, luctus dui. Integer aliquam massa sed dui luctus pharetra. " +
                    "Vestibulum at erat condimentum, posuere tortor maximus, luctus nibh. " +
                    "Nam quis mattis metus, et pretium metus. Vivamus augue magna, convallis " +
                    "ut massa sit amet, posuere viverra magna.\n\n" +
                    "In erat metus, porta nec dolor in, porttitor lacinia velit. Donec aliquam " +
                    "dolor sit amet tellus mollis varius. Mauris eget ipsum mollis, condimentum " +
                    "velit eu, elementum odio. Nam suscipit lacinia tristique. Sed neque lacus, " +
                    "porta nec est quis, scelerisque porttitor tellus. Orci varius natoque " +
                    "penatibus et magnis dis parturient montes, nascetur ridiculus mus. " +
                    "In sed volutpat eros. Donec in consequat nulla. Curabitur gravida " +
                    "condimentum dictum. Orci varius natoque penatibus et magnis dis " +
                    "parturient montes, nascetur ridiculus mus. Praesent auctor, dui quis " +
                    "bibendum bibendum, justo quam dictum diam, ac varius tortor elit nec sem.\n\n";
}
