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

package com.huawei.mlkit.example.documentSkewCorrection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.dsc.MLDocumentSkewCorrectionCoordinateInput;
import com.huawei.hms.mlsdk.dsc.MLDocumentSkewCorrectionResult;
import com.huawei.hms.mlsdk.dsc.MLDocumentSkewCorrectionAnalyzer;
import com.huawei.hms.mlsdk.dsc.MLDocumentSkewCorrectionAnalyzerFactory;
import com.huawei.hms.mlsdk.dsc.MLDocumentSkewCorrectionAnalyzerSetting;
import com.huawei.hms.mlsdk.dsc.MLDocumentSkewDetectResult;
import com.huawei.hackzurich.R;

public class DocumentSkewCorrectionActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = DocumentSkewCorrectionActivity.class.getSimpleName();

    private MLDocumentSkewCorrectionAnalyzer analyzer;

    private ImageView mImageView;

    private Bitmap bitmap;

    private MLDocumentSkewCorrectionCoordinateInput input;

    private MLFrame mlFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_document_skew_correction);
        this.findViewById(R.id.image_refine).setOnClickListener(this);
        this.mImageView = this.findViewById(R.id.image_refine_result);
    }

    @Override
    public void onClick(View v) {
        this.analyzer();
    }

    private void analyzer() {
        // Create the setting.
        MLDocumentSkewCorrectionAnalyzerSetting setting = new MLDocumentSkewCorrectionAnalyzerSetting
                .Factory()
                .create();

        // Get the analyzer.
        this.analyzer = MLDocumentSkewCorrectionAnalyzerFactory.getInstance().getDocumentSkewCorrectionAnalyzer(setting);

        // Create the bitmap.
        this.bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.document_correct_image);

        // Create a MLFrame by using the bitmap.
        this.mlFrame = new MLFrame.Creator().setBitmap(this.bitmap).create();

        // Call document skew detect interface to get coordinate data
        Task<MLDocumentSkewDetectResult> detectTask = this.analyzer.asyncDocumentSkewDetect(this.mlFrame);
        detectTask.addOnSuccessListener(new OnSuccessListener<MLDocumentSkewDetectResult>() {
            @Override
            public void onSuccess(MLDocumentSkewDetectResult detectResult) {
                Log.e("gww", detectResult.getResultCode() + ":");
                // Detect success.
                if (detectResult != null && detectResult.getResultCode() == 0) {
                    Point leftTop = detectResult.getLeftTopPosition();
                    Point rightTop = detectResult.getRightTopPosition();
                    Point leftBottom = detectResult.getLeftBottomPosition();
                    Point rightBottom = detectResult.getRightBottomPosition();

                    List<Point> coordinates = new ArrayList<>();
                    coordinates.add(leftTop);
                    coordinates.add(rightTop);
                    coordinates.add(leftBottom);
                    coordinates.add(rightBottom);

                    DocumentSkewCorrectionActivity.this.setDetectData(new MLDocumentSkewCorrectionCoordinateInput(coordinates));
                    DocumentSkewCorrectionActivity.this.refineImg();
                } else {
                    // Detect failure.
                    Log.e("gww", "zhelichucuole");
                    DocumentSkewCorrectionActivity.this.displayFailure();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Processing logic for detect failure.
                Log.e("gww", e.getMessage() + "");
                DocumentSkewCorrectionActivity.this.displayFailure();
            }
        });
    }

    // Show result
    private void displaySuccess(MLDocumentSkewCorrectionResult refineResult) {
        if (this.bitmap == null) {
            this.displayFailure();
            return;
        }
        // Draw the portrait with a transparent background.
        Bitmap corrected = refineResult.getCorrected();
        if (corrected != null) {
            this.mImageView.setImageBitmap(corrected);
        } else {
            this.displayFailure();
        }
    }

    private void displayFailure() {
        Toast.makeText(this.getApplicationContext(), "Fail", Toast.LENGTH_SHORT).show();
    }

    private void setDetectData(MLDocumentSkewCorrectionCoordinateInput input) {
        this.input = input;
    }

    // Refine image
    private void refineImg() {
        // Call refine image interface
        Task<MLDocumentSkewCorrectionResult> correctionTask = this.analyzer.asyncDocumentSkewCorrect(this.mlFrame, this.input);
        correctionTask.addOnSuccessListener(new OnSuccessListener<MLDocumentSkewCorrectionResult>() {
            @Override
            public void onSuccess(MLDocumentSkewCorrectionResult refineResult) {
                if (refineResult != null && refineResult.getResultCode() == 0) {
                    // Refine success.
                    DocumentSkewCorrectionActivity.this.displaySuccess(refineResult);
                } else {
                    // Refine failure.
                    DocumentSkewCorrectionActivity.this.displayFailure();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Processing logic for refine failure.
                DocumentSkewCorrectionActivity.this.displayFailure();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.analyzer != null) {
            try {
                this.analyzer.stop();
            } catch (IOException e) {
                Log.e(DocumentSkewCorrectionActivity.TAG, "Stop failed: " + e.getMessage());
            }
        }
    }
}
