/*
 * Copyright (C) 2026 Charlie Smith
 *
 * This file is part of eSpeak NG Charlie, a fork of eSpeak NG.
 * Licensed under the GNU General Public License, version 3 or later.
 * See the file COPYING in the project root.
 */

package nz.charlie.espeakng;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView mStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStatus = (TextView) findViewById(R.id.status);

        File root = VoiceFolder.getPublicRoot();
        VoiceFolder.ensureStructure(root);

        TextView pathView = (TextView) findViewById(R.id.voice_folder_path);
        pathView.setText(getString(R.string.voice_folder_path_fmt, root.getAbsolutePath()));

        hook(R.id.btn_rescan, new View.OnClickListener() {
            @Override public void onClick(View v) { rescan(); }
        });
        hook(R.id.btn_design, new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DesignVoiceActivity.class));
            }
        });
        hook(R.id.btn_settings, new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TtsSettingsActivity.class));
            }
        });
        hook(R.id.btn_storage, new View.OnClickListener() {
            @Override public void onClick(View v) { requestAllFilesAccess(); }
        });
        hook(R.id.btn_how_to_nz, new View.OnClickListener() {
            @Override public void onClick(View v) {
                mStatus.setText(getString(R.string.how_to_pick_en_nz));
                mStatus.announceForAccessibility(getString(R.string.how_to_pick_en_nz));
            }
        });
        hook(R.id.btn_about, new View.OnClickListener() {
            @Override public void onClick(View v) {
                String about = getString(R.string.about_text, SpeechSynthesis.getVersion(), getRepoUrl());
                mStatus.setText(about);
                mStatus.announceForAccessibility(about);
            }
        });

        updateStorageStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VoiceFolder.ensureStructure(VoiceFolder.getPublicRoot());
        updateStorageStatus();
    }

    private void hook(int id, View.OnClickListener listener) {
        View view = findViewById(id);
        if (view != null) view.setOnClickListener(listener);
    }

    private void rescan() {
        if (!hasStorageAccess()) {
            requestAllFilesAccess();
            Toast.makeText(this, R.string.need_all_files_access, Toast.LENGTH_LONG).show();
            return;
        }
        if (!CheckVoiceData.hasBaseResources(this)) {
            CheckVoiceData.extractVoiceData(this);
        }
        VoiceFolder.Result result = VoiceFolder.rescan(this);
        String summary = result.summary();
        mStatus.setText(summary);
        mStatus.announceForAccessibility(summary);
        Toast.makeText(this, summary, Toast.LENGTH_LONG).show();
        sendBroadcast(new Intent(DownloadVoiceData.BROADCAST_LANGUAGES_UPDATED));
    }

    private boolean hasStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    private void requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                    return;
                } catch (Exception e2) {
                    // fall through
                }
            }
        }
        Intent settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        settings.setData(Uri.parse("package:" + getPackageName()));
        startActivity(settings);
    }

    private void updateStorageStatus() {
        String extra = hasStorageAccess()
                ? getString(R.string.storage_granted)
                : getString(R.string.storage_needed);
        if (mStatus.getText() == null || mStatus.getText().length() == 0) {
            mStatus.setText(extra);
        }
    }

    private static String getRepoUrl() {
        return "https://github.com/BlindCharlieSmith2020/espeak-ng-charlie";
    }
}
