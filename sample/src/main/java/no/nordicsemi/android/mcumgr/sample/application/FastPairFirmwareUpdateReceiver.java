package no.nordicsemi.android.mcumgr.sample.application;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.PendingIntentCompat;

import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.mcumgr.sample.ScannerActivity;
import timber.log.Timber;

/**
 * A broadcast received to FastPair firmware update notifications.
 * <p>
 * Read more <a href="https://developers.google.com/nearby/fast-pair/companion-apps#firmware_update_intent">here</a>.
 */
public class FastPairFirmwareUpdateReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.google.android.gms.nearby.fastpair.ACTION_FIRMWARE_UPDATE_BROADCAST";
    public static final String EXTRA_LOCAL_VERSION = "com.google.android.gms.nearby.fastpair.EXTRA_LOCAL_FIRMWARE_VERSION";
    public static final String EXTRA_NOTIF_SHOWN = "com.google.android.gms.nearby.fastpair.EXTRA_UPDATE_NOTIFICATION_SHOWN";

    public static final String NOTIFICATION_CHANNEL_ID = "fastpair_dfu";
    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        final String localVersion = intent.getStringExtra(EXTRA_LOCAL_VERSION);
        final boolean notificationShown = intent.getBooleanExtra(EXTRA_NOTIF_SHOWN, false);
        Timber.tag("FastPair").w("An outdated FastPair accessory with version %s found (notification shown = %b)", localVersion, notificationShown);

        // Note:
        // Unfortunately, the current version of FastPair firmware update intents
        // (https://developers.google.com/nearby/fast-pair/companion-apps#firmware_update_intent)
        // doesn't give BluetoothDevice or any indication about the device.
        // At this moment, the device is already disconnected, so it's not possible to
        // narrow the list of possible targets to only connected ones.
        // Also, some devices need special action to switch to DFU mode.
        // The companion app should display an instruction how to make the device updatable.

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            final PendingIntent openApp = PendingIntentCompat.getActivity(context, 0,
                    new Intent(context, ScannerActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT,
                    false
            );

            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Update available")
                    .setContentText("New version available for your Fast Pair accessory.")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setContentIntent(openApp);

            NotificationManagerCompat.from(context)
                    .notify(NOTIFICATION_ID, builder.build());
        }
    }
}