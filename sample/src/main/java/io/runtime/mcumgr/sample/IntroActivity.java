package io.runtime.mcumgr.sample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        findViewById(R.id.button_continue).setOnClickListener(v -> finish());

        findViewById(R.id.intro_link_nrf_connect_sdk).setOnClickListener(v ->
                open(Uri.parse("https://www.nordicsemi.com/Software-and-Tools/Software/nRF-Connect-SDK"))
        );
        findViewById(R.id.intro_link_smp_sample).setOnClickListener(v ->
                open(Uri.parse("https://developer.nordicsemi.com/nRF_Connect_SDK/doc/latest/zephyr/samples/subsys/mgmt/mcumgr/smp_svr/README.html"))
        );
        findViewById(R.id.intro_link_source_code).setOnClickListener(v ->
                open(Uri.parse("https://github.com/NordicSemiconductor/Android-nRF-Connect-Device-Manager"))
        );
        findViewById(R.id.intro_link_mcu_manager).setOnClickListener(v ->
                open(Uri.parse("https://github.com/JuulLabs-OSS/mcumgr-android"))
        );

        try {
            final TextView versionView = findViewById(R.id.version);
            final PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            versionView.setText(getResources().getString(R.string.intro_version, info.versionName));
        } catch (final PackageManager.NameNotFoundException e) {
            // Ignore
        }

    }

    private void open(@NonNull final Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_DEFAULT, uri);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

}