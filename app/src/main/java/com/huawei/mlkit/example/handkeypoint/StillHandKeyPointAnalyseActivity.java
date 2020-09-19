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

package com.huawei.mlkit.example.handkeypoint;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.handkeypoint.MLHandKeypointAnalyzer;
import com.huawei.hms.mlsdk.handkeypoint.MLHandKeypointAnalyzerFactory;
import com.huawei.hms.mlsdk.handkeypoint.MLHandKeypointAnalyzerSetting;
import com.huawei.hms.mlsdk.handkeypoint.MLHandKeypoints;
import com.huawei.hackzurich.R;
import com.huawei.mlkit.example.camera.GraphicOverlay;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class StillHandKeyPointAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "StillHandKeyPointAnalyseActivity";

    private GraphicOverlay mGraphicOverlay;

    private ImageView mPreviewView;

    private Button mDetectSync;

    private Button mDetectAsync;

    private MLFrame mlFrame;

    private MLHandKeypointAnalyzer mAnalyzer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_hand_analyse);
        initView();
    }

    private void initView() {
        mPreviewView = findViewById(R.id.handstill_previewPane);
        mGraphicOverlay = findViewById(R.id.handstill_previewOverlay);
        mDetectSync = findViewById(R.id.handstill_detect_sync);
        mDetectSync.setOnClickListener(this);
        mDetectAsync = findViewById(R.id.handstill_detect_async);
        mDetectAsync.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.handstill_detect_async:
                mGraphicOverlay.clear();
                createAnalyzer();
                // Asynchronous analyse.
                analyzerAsync();
                break;
            case R.id.handstill_detect_sync:
                mGraphicOverlay.clear();
                createAnalyzer();
                // Synchronous analyse.
                analyzerSync();
                break;
        }
    }

    private void createAnalyzer() {
        // Create an MLFrame by using the bitmap.
        Bitmap originBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.hand);

        // Gets the targeted width / height, only portrait.
        int maxHeight = ((View) mPreviewView.getParent()).getHeight();
        int targetWidth = ((View) mPreviewView.getParent()).getWidth();
        // Determine how much to scale down the image
        float scaleFactor = Math.max(
                (float) originBitmap.getWidth() / (float) targetWidth,
                (float) originBitmap.getHeight() / (float) maxHeight);

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                originBitmap,
                (int) (originBitmap.getWidth() / scaleFactor),
                (int) (originBitmap.getHeight() / scaleFactor),
                true);

        mlFrame = new MLFrame.Creator().setBitmap(resizedBitmap).create();

        MLHandKeypointAnalyzerSetting setting =
                new MLHandKeypointAnalyzerSetting.Factory()
                        .setMaxHandResults(2)
                        .setSceneType(MLHandKeypointAnalyzerSetting.TYPE_ALL)
                        .create();

        this.mAnalyzer = MLHandKeypointAnalyzerFactory.getInstance().getHandKeypointAnalyzer(setting);
    }

    /**
     * Synchronous analyse.
     */
    private void analyzerSync() {
        List<MLHandKeypoints> mlHandKeypointsList = new ArrayList<>();
        SparseArray<MLHandKeypoints> mlHandKeypointsSparseArray = mAnalyzer.analyseFrame(mlFrame);
        for (int i = 0; i < mlHandKeypointsSparseArray.size(); i++) {
            mlHandKeypointsList.add(mlHandKeypointsSparseArray.get(i));
        }
        if (mlHandKeypointsList != null && !mlHandKeypointsList.isEmpty()) {
            processSuccess(mlHandKeypointsList);
        } else {
            processFailure();
        }
    }

    /**
     * Asynchronous analyse.
     */
    private void analyzerAsync() {
        Task<List<MLHandKeypoints>> task = mAnalyzer.asyncAnalyseFrame(mlFrame);
        task.addOnSuccessListener(new OnSuccessListener<List<MLHandKeypoints>>() {
            @Override
            public void onSuccess(List<MLHandKeypoints> results) {
                // Detection success.
                if (results != null && !results.isEmpty()) {
                    processSuccess(results);
                } else {
                    processFailure();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Detection failure.
                processFailure();
            }
        });
    }

    private void processFailure() {
        Toast.makeText(this.getApplicationContext(), "Fail", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("LongLogTag")
    private void processSuccess(List<MLHandKeypoints> results) {
        mGraphicOverlay.clear();
        HandKeypointGraphic handGraphic = new HandKeypointGraphic(mGraphicOverlay, results);
        mGraphicOverlay.add(handGraphic);
    }
}
