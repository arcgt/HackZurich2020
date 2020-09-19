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

package com.huawei.mlkit.example.skeleton;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.skeleton.MLSkeleton;
import com.huawei.hms.mlsdk.skeleton.MLSkeletonAnalyzer;
import com.huawei.hms.mlsdk.skeleton.MLSkeletonAnalyzerFactory;
import com.huawei.hackzurich.R;
import com.huawei.mlkit.example.camera.GraphicOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Static image detection
 */
public class StillSkeletonAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = StillSkeletonAnalyseActivity.class.getSimpleName();

    private GraphicOverlay graphicOverlay;
    private ImageView previewView;

    private MLSkeletonAnalyzer analyzer;
    private MLFrame mFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_skeleton_analyse);
        previewView = findViewById(R.id.skeleton_previewPane);
        graphicOverlay = findViewById(R.id.skeleton_previewOverlay);
        this.findViewById(R.id.skeleton_detect_sync).setOnClickListener(this);
        this.findViewById(R.id.skeleton_detect_async).setOnClickListener(this);
    }

    private void createAnalyzer() {
        // Create an MLFrame by using the bitmap.
        Bitmap originBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.skeleton_image);

        // Gets the targeted width / height, only portrait.
        int maxHeight = ((View) previewView.getParent()).getHeight();
        int targetWidth = ((View) previewView.getParent()).getWidth();
        // Determine how much to scale down the image
        float scaleFactor =
                Math.max(
                        (float) originBitmap.getWidth() / (float) targetWidth,
                        (float) originBitmap.getHeight() / (float) maxHeight);

        Bitmap resizedBitmap =
                Bitmap.createScaledBitmap(
                        originBitmap,
                        (int) (originBitmap.getWidth() / scaleFactor),
                        (int) (originBitmap.getHeight() / scaleFactor),
                        true);

        mFrame = new MLFrame.Creator().setBitmap(resizedBitmap).create();
        analyzer = MLSkeletonAnalyzerFactory.getInstance().getSkeletonAnalyzer();
    }

    /**
     * Synchronous analyse.
     */
    private void analyzerSync() {
        List<MLSkeleton> list = new ArrayList<>();
        SparseArray<MLSkeleton> sparseArray = analyzer.analyseFrame(mFrame);
        for (int i = 0; i < sparseArray.size(); i++) {
            list.add(sparseArray.get(i));
        }
        // Remove invalid point.
        List<MLSkeleton> skeletons = SkeletonUtils.getValidSkeletons(list);
        if (skeletons != null && !skeletons.isEmpty()) {
            processSuccess(skeletons);
        } else {
            processFailure();
        }
    }

    /**
     * Asynchronous analyse.
     */
    private void analyzerAsync() {
        Task<List<MLSkeleton>> task = analyzer.asyncAnalyseFrame(mFrame);
        task.addOnSuccessListener(new OnSuccessListener<List<MLSkeleton>>() {
            @Override
            public void onSuccess(List<MLSkeleton> results) {
                // Detection success.
                List<MLSkeleton> skeletons = SkeletonUtils.getValidSkeletons(results);
                if(skeletons != null && !skeletons.isEmpty()) {
                    processSuccess(skeletons);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.skeleton_detect_async:
                graphicOverlay.clear();
                createAnalyzer();
                // Asynchronous analyse.
                analyzerAsync();
                break;
            case R.id.skeleton_detect_sync:
                graphicOverlay.clear();
                createAnalyzer();
                // Synchronous analyse.
                analyzerSync();
                break;
            default:
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.analyzer == null) {
            return;
        }
        try {
            this.analyzer.stop();
        } catch (IOException e) {
            Log.e(StillSkeletonAnalyseActivity.TAG, "Stop failed: " + e.getMessage());
        }
    }

    private void processFailure() {
        Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show();
    }

    private void processSuccess(List<MLSkeleton> results) {
        graphicOverlay.clear();
        SkeletonGraphic skeletonGraphic = new SkeletonGraphic(graphicOverlay, results);
        graphicOverlay.add(skeletonGraphic);
    }

}
