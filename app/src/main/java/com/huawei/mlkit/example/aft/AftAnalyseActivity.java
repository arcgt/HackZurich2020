/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.huawei.mlkit.example.aft;

import java.io.File;
import java.util.List;
import java.util.Timer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hms.mlsdk.aft.cloud.MLRemoteAftEngine;
import com.huawei.hms.mlsdk.aft.cloud.MLRemoteAftListener;
import com.huawei.hms.mlsdk.aft.cloud.MLRemoteAftResult;
import com.huawei.hms.mlsdk.aft.cloud.MLRemoteAftSetting;
import com.huawei.hms.mlsdk.common.MLApplication;
import com.huawei.mlkit.example.MainActivity;
import com.huawei.hackzurich.R;

public class AftAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = AftAnalyseActivity.class.getSimpleName();
    private static final int RECORD_REQUEST_CODE = 3;
    private static final int THRESHOLD_TIME = 60000; // 60s
    private TextView tvFileName;
    private TextView tvText;
    private ImageView imgVoice;
    private ImageView imgPlay;
    private String taskId;
    private Uri uri;
    private MLRemoteAftEngine engine;
    private MLRemoteAftSetting setting;
    private static Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_voice_aft);
        this.tvFileName = this.findViewById(R.id.file_name);
        this.tvText = this.findViewById(R.id.text_output);
        this.imgVoice = this.findViewById(R.id.voice_input);
        this.imgPlay = this.findViewById(R.id.voice_play);
        this.imgVoice.setOnClickListener(this);
        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) {
            this.requestAudioPermission();
        }

        this.setting = new MLRemoteAftSetting.Factory()
                // Set the transcription language code, complying with the BCP 47 standard. Currently, zh (Chinese) and en-US (English) are supported.
                .setLanguageCode("en-US")
                .enablePunctuation(true)
                .enableWordTimeOffset(true)
                .enableSentenceTimeOffset(true)
                .create();
        // Set ApiKey.
        MLApplication.getInstance().setApiKey(MainActivity.apiKey);
        this.engine = MLRemoteAftEngine.getInstance();
        this.engine.init(this);
        // Pass the listener callback to the audio file transcription engine.
        this.engine.setAftListener(this.aftListener);
    }

    private void requestAudioPermission() {
        final String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(this, permissions, this.RECORD_REQUEST_CODE);
            return;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.voice_input:
                this.tvFileName.setText("add_voice");
                AftAnalyseActivity.this.imgPlay.setImageResource(R.drawable.icon_voice_new);
                this.tvText.setText("");
                this.startRecord();
                break;
            default:
                break;
        }
    }

    private void startRecord() {
        Intent intent = new Intent();
        intent.setAction(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, this.uri);
        this.startActivityForResult(intent, AftAnalyseActivity.RECORD_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AftAnalyseActivity.RECORD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            this.uri = data.getData();
            String filePath = this.getAudioFilePathFromUri(this.uri);
            File file = new File(filePath);
            this.setFileName(file.getName());
            Long audioTime = getAudioFileTimeFromUri(uri);
            if (audioTime < THRESHOLD_TIME) {
                // Transfer the audio less than one minute to the audio file transcription engine.
                this.taskId = this.engine.shortRecognize(this.uri, this.setting);
                Log.i(TAG, "Short audio transcription.");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setFileName(final String path) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AftAnalyseActivity.this.tvFileName.setText(path);
                AftAnalyseActivity.this.imgPlay.setImageResource(R.drawable.icon_voice_accent_new);
            }
        });
    }

    private String getAudioFilePathFromUri(Uri uri) {
        Cursor cursor = this.getContentResolver()
                .query(uri, null, null, null, null);
        cursor.moveToFirst();
        int index = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
        return cursor.getString(index);
    }

    private Long getAudioFileTimeFromUri(Uri uri) {
        Cursor cursor = this.getContentResolver()
                .query(uri, null, null, null, null);
        cursor.moveToFirst();
        Long time = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
        return time;
    }

    private void displayFailure(int errorCode, String errorMsg) {
        this.tvText.setText("Failure, errorCode is:" + errorCode + ", errorMessage is:" + errorMsg);
    }

    /**
     * Audio file transcription callback function. If you want to use AFT,
     * you need to apply for an agconnect-services.json file in the developer
     * alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
     * replacing the sample-agconnect-services.json in the project.
     */
    private MLRemoteAftListener aftListener = new MLRemoteAftListener() {
        @Override
        public void onInitComplete(String taskId, Object ext) {
            // Reserved.
        }

        @Override
        public void onUploadProgress(String taskId, double progress, Object ext) {
            // Reserved.
        }

        @Override
        public void onEvent(String taskId, int eventId, Object ext) {
            // Reserved.
        }

        // Get notification of transcription results, where developers can process the transcription results.
        @Override
        public void onResult(String taskId, MLRemoteAftResult result, Object ext) {
            Log.i(AftAnalyseActivity.TAG, taskId + " ");
            if (result.isComplete()) {
                Log.i(AftAnalyseActivity.TAG, "result" + result.getText());
                cancelTimer();
                AftAnalyseActivity.this.tvText.setText(result.getText());
                List<MLRemoteAftResult.Segment> words = result.getWords();
                if (words != null && words.size() != 0) {
                    for (MLRemoteAftResult.Segment word : words) {
                        Log.e(TAG, "MLAsrCallBack word  text is : " + word.getText() + ", startTime is : " + word.getStartTime() + ". endTime is : " + word.getEndTime());
                    }
                }

                List<MLRemoteAftResult.Segment> sentences = result.getSentences();
                if (sentences != null && sentences.size() != 0) {
                    for (MLRemoteAftResult.Segment sentence : sentences) {
                        Log.e(TAG, "MLAsrCallBack sentence  text is : " + sentence.getText() + ", startTime is : " + sentence.getStartTime() + ". endTime is : " + sentence.getEndTime());
                    }
                }
            } else {
                tvText.setText("Loading...");
                return;
            }
            if (result == null || result.getTaskId() == null || result.getText().equals("")) {
                AftAnalyseActivity.this.tvText.setText("No speech recognized, please re-enter.");
                return;
            }
        }

        // Transliteration error callback function.
        @Override
        public void onError(String taskId, int errorCode, String message) {
            AftAnalyseActivity.this.displayFailure(errorCode, message);
            Log.e(AftAnalyseActivity.TAG, "onError." + errorCode + " task:" + taskId + " errorMessage：" + message);
        }
    };

    private void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.engine.close();
    }
}
