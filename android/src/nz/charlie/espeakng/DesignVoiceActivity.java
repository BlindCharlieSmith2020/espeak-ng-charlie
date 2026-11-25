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
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DesignVoiceActivity extends Activity {
    private static final String PREVIEW_NAME = "designed-_preview";
    private static final String SAMPLE =
            "Gidday. Let's have some fish and chips in Whangarei, then drive over to Taupo.";

    private EditText mName;
    private SeekBar mPitch;
    private SeekBar mPitchRange;
    private SeekBar mSpeed;
    private SeekBar mFlutter;
    private SeekBar mRoughness;
    private SeekBar mBreathiness;
    private SeekBar mFormant1;
    private SeekBar mFormant2;
    private SeekBar mFormant3;
    private TextView mPitchVal, mPitchRangeVal, mSpeedVal, mFlutterVal, mRoughnessVal,
            mBreathinessVal, mFormant1Val, mFormant2Val, mFormant3Val;
    private TextView mStatus;
    private SpeechSynthesis mEngine;
    private AudioTrack mTrack;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final List<byte[]> mPreviewBuffers = new ArrayList<byte[]>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_design_voice);

        mName = (EditText) findViewById(R.id.voice_name);
        mPitch = bindSeek(R.id.pitch, R.id.pitch_value, 50, 150, 80);
        mPitchRange = bindSeek(R.id.pitch_range, R.id.pitch_range_value, 50, 200, 118);
        mSpeed = bindSeek(R.id.speed, R.id.speed_value, 50, 200, 100);
        mFlutter = bindSeek(R.id.flutter, R.id.flutter_value, 0, 20, 0);
        mRoughness = bindSeek(R.id.roughness, R.id.roughness_value, 0, 20, 0);
        mBreathiness = bindSeek(R.id.breathiness, R.id.breathiness_value, 0, 100, 0);
        mFormant1 = bindSeek(R.id.formant1, R.id.formant1_value, 80, 120, 100);
        mFormant2 = bindSeek(R.id.formant2, R.id.formant2_value, 80, 120, 100);
        mFormant3 = bindSeek(R.id.formant3, R.id.formant3_value, 80, 120, 100);
        mPitchVal = (TextView) findViewById(R.id.pitch_value);
        mPitchRangeVal = (TextView) findViewById(R.id.pitch_range_value);
        mSpeedVal = (TextView) findViewById(R.id.speed_value);
        mFlutterVal = (TextView) findViewById(R.id.flutter_value);
        mRoughnessVal = (TextView) findViewById(R.id.roughness_value);
        mBreathinessVal = (TextView) findViewById(R.id.breathiness_value);
        mFormant1Val = (TextView) findViewById(R.id.formant1_value);
        mFormant2Val = (TextView) findViewById(R.id.formant2_value);
        mFormant3Val = (TextView) findViewById(R.id.formant3_value);
        mStatus = (TextView) findViewById(R.id.design_status);

        if (!CheckVoiceData.hasBaseResources(this)) {
            CheckVoiceData.extractVoiceData(this);
        }
        mEngine = new SpeechSynthesis(this, new SpeechSynthesis.SynthReadyCallback() {
            @Override public void onSynthDataReady(byte[] audioData) {
                if (audioData != null) {
                    synchronized (mPreviewBuffers) {
                        mPreviewBuffers.add(audioData);
                    }
                }
            }
            @Override public void onSynthDataComplete() {
                playCollected();
            }
            @Override public void onSynthWordBoundary(int textPosition, int textLength, int markerInFrames) {}
        });

        ((Button) findViewById(R.id.btn_preview)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { preview(); }
        });
        ((Button) findViewById(R.id.btn_save)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
    }

    @Override
    protected void onDestroy() {
        if (mTrack != null) {
            mTrack.release();
            mTrack = null;
        }
        super.onDestroy();
    }

    private SeekBar bindSeek(int seekId, int labelId, int min, int max, int initial) {
        final SeekBar bar = (SeekBar) findViewById(seekId);
        final TextView label = (TextView) findViewById(labelId);
        // SeekBar is 0..range; store min in tag.
        bar.setTag(min);
        bar.setMax(max - min);
        bar.setProgress(initial - min);
        label.setText(String.valueOf(initial));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + (Integer) seekBar.getTag();
                label.setText(String.valueOf(value));
                seekBar.setContentDescription(label.getContentDescription() + ": " + value);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        return bar;
    }

    private int valueOf(SeekBar bar) {
        return bar.getProgress() + (Integer) bar.getTag();
    }

    private DesignedVoice current() {
        DesignedVoice voice = new DesignedVoice();
        voice.name = mName.getText().toString();
        if (voice.name.trim().length() == 0) voice.name = "My voice";
        voice.pitchBase = valueOf(mPitch);
        voice.pitchRange = valueOf(mPitchRange);
        voice.speedPercent = valueOf(mSpeed);
        voice.flutter = valueOf(mFlutter);
        voice.roughness = valueOf(mRoughness);
        voice.breathiness = valueOf(mBreathiness);
        voice.formant1 = valueOf(mFormant1);
        voice.formant2 = valueOf(mFormant2);
        voice.formant3 = valueOf(mFormant3);
        return voice;
    }

    private void preview() {
        DesignedVoice voice = current();
        String savedName = voice.name;
        voice.name = PREVIEW_NAME;
        File dest = VoiceFolder.getEngineVariantsDir(CheckVoiceData.getDataPath(this));
        try {
            voice.writeTo(dest);
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        voice.name = savedName;
        synchronized (mPreviewBuffers) {
            mPreviewBuffers.clear();
        }
        if (mTrack != null) {
            mTrack.stop();
        }
        // Use English (New Zealand) when it is present, otherwise English.
        Voice selected = null;
        for (Voice v : mEngine.getAvailableVoices()) {
            if ("en".equals(v.locale.getLanguage()) && "NZ".equalsIgnoreCase(v.locale.getCountry())) {
                selected = v;
                break;
            }
            if (selected == null && "en".equals(v.locale.getLanguage())) {
                selected = v;
            }
        }
        if (selected != null) {
            mEngine.setVoice(selected, VoiceVariant.parseVoiceVariant(PREVIEW_NAME));
        }
        mEngine.synthesize(SAMPLE, false);
        mStatus.setText(R.string.design_previewing);
        mStatus.announceForAccessibility(getString(R.string.design_previewing));
    }

    private void playCollected() {
        final List<byte[]> chunks;
        synchronized (mPreviewBuffers) {
            chunks = new ArrayList<byte[]>(mPreviewBuffers);
        }
        int total = 0;
        for (byte[] c : chunks) total += c.length;
        if (total == 0) return;
        final byte[] pcm = new byte[total];
        int o = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, pcm, o, c.length);
            o += c.length;
        }
        final int rate = mEngine.getSampleRate() > 0 ? mEngine.getSampleRate() : 22050;
        mHandler.post(new Runnable() {
            @Override public void run() {
                if (mTrack != null) {
                    mTrack.release();
                }
                int min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, rate,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(min, pcm.length), AudioTrack.MODE_STREAM);
                mTrack.play();
                mTrack.write(pcm, 0, pcm.length);
            }
        });
    }

    private void save() {
        DesignedVoice voice = current();
        VoiceFolder.ensureStructure(VoiceFolder.getPublicRoot());
        File publicVariants = VoiceFolder.getVariantsDir(VoiceFolder.getPublicRoot());
        File engineVariants = VoiceFolder.getEngineVariantsDir(CheckVoiceData.getDataPath(this));
        try {
            File saved = voice.writeTo(publicVariants);
            voice.writeTo(engineVariants);
            String msg = getString(R.string.design_saved_fmt, saved.getName(), publicVariants.getAbsolutePath());
            mStatus.setText(msg);
            mStatus.announceForAccessibility(msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            sendBroadcast(new Intent(DownloadVoiceData.BROADCAST_LANGUAGES_UPDATED));
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
