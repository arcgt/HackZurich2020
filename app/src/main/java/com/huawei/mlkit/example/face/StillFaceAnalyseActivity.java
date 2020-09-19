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

package com.huawei.mlkit.example.face;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.face.MLFace;
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer;
import com.huawei.hackzurich.R;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Static image detection
 */
public class StillFaceAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = StillFaceAnalyseActivity.class.getSimpleName();

    private TextView mTextView;

    private MLFaceAnalyzer analyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_face_analyse);
        this.mTextView = this.findViewById(R.id.result);
        this.findViewById(R.id.face_detect).setOnClickListener(this);
    }

    private void analyzer() {
        // Use default parameter settings.
        this.analyzer = MLAnalyzerFactory.getInstance().getFaceAnalyzer();
        // Create an MLFrame by using the bitmap. Recommended image size: large than 320*320, less than 1920*1920.
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.face_image);
        MLFrame frame = MLFrame.fromBitmap(bitmap);
        // Call the asyncAnalyseFrame method to perform face detection
        Task<List<MLFace>> task = this.analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<List<MLFace>>() {
            @Override
            public void onSuccess(List<MLFace> faces) {
                // Detection success.
                if (faces.size() > 0) {
                    StillFaceAnalyseActivity.this.displaySuccess(faces.get(0));
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Detection failure.
                StillFaceAnalyseActivity.this.displayFailure();
            }
        });
    }

    private void displayFailure() {
        this.mTextView.setText("Failure");
    }

    private void displaySuccess(MLFace mFace) {
        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        String result =
            "Left eye open Probability: " + decimalFormat.format(mFace.getFeatures().getLeftEyeOpenProbability());
        result +=
            "\nRight eye open Probability: " + decimalFormat.format(mFace.getFeatures().getRightEyeOpenProbability());
        result += "\nMoustache Probability: " + decimalFormat.format(mFace.getFeatures().getMoustacheProbability());
        result += "\nGlass Probability: " + decimalFormat.format(mFace.getFeatures().getSunGlassProbability());
        result += "\nHat Probability: " + decimalFormat.format(mFace.getFeatures().getHatProbability());
        result += "\nAge: " + mFace.getFeatures().getAge();
        result += "\nGender: " + ((mFace.getFeatures().getSexProbability() > 0.5f) ? "Female" : "Male");
        result += "\nRotationAngleY: " + decimalFormat.format(mFace.getRotationAngleY());
        result += "\nRotationAngleZ: " + decimalFormat.format(mFace.getRotationAngleZ());
        result += "\nRotationAngleX: " + decimalFormat.format(mFace.getRotationAngleX());
        this.mTextView.setText(result);
    }

    @Override
    public void onClick(View v) {
        this.analyzer();
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
            Log.e(StillFaceAnalyseActivity.TAG, "Stop failed: " + e.getMessage());
        }
    }
}
