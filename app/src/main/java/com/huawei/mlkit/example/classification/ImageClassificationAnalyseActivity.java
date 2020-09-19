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

package com.huawei.mlkit.example.classification;

import java.io.IOException;
import java.util.List;

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
import com.huawei.hms.mlsdk.classification.MLImageClassification;
import com.huawei.hms.mlsdk.classification.MLImageClassificationAnalyzer;
import com.huawei.hms.mlsdk.classification.MLLocalClassificationAnalyzerSetting;
import com.huawei.hms.mlsdk.classification.MLRemoteClassificationAnalyzerSetting;
import com.huawei.hms.mlsdk.common.MLApplication;
import com.huawei.hms.mlsdk.common.MLException;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.mlkit.example.MainActivity;
import com.huawei.hackzurich.R;


public class ImageClassificationAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = ImageClassificationAnalyseActivity.class.getSimpleName();

    private TextView mTextView;

    private MLImageClassificationAnalyzer analyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_classification_analyse);
        this.mTextView = this.findViewById(R.id.classification_result);
        this.findViewById(R.id.classification_detect).setOnClickListener(this);
    }

    private void localAnalyzer() {
        // Use customized parameter settings for device-based recognition.
        MLLocalClassificationAnalyzerSetting deviceSetting =
                new MLLocalClassificationAnalyzerSetting.Factory().setMinAcceptablePossibility(0.8f).create();
        this.analyzer = MLAnalyzerFactory.getInstance().getLocalImageClassificationAnalyzer(deviceSetting);
        // Create an MLFrame by using the bitmap. Recommended image size: large than 112*112.
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.classification_image);
        MLFrame frame = MLFrame.fromBitmap(bitmap);
        Task<List<MLImageClassification>> task = this.analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<List<MLImageClassification>>() {
            @Override
            public void onSuccess(List<MLImageClassification> classifications) {
                // Recognition success.
                ImageClassificationAnalyseActivity.this.displaySuccess(classifications);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Recognition failure.
                ImageClassificationAnalyseActivity.this.displayFailure();
            }
        });
    }

    /**
     * Image classification analyzer on the cloud. If you want to use cloud image classification analyzer,
     * you need to apply for an agconnect-services.json file in the developer
     * alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
     * replacing the sample-agconnect-services.json in the project.
     */
    private void remoteAnalyzer() {
        // Use customized parameter settings for device-based recognition.
        MLRemoteClassificationAnalyzerSetting cloudSetting =
                new MLRemoteClassificationAnalyzerSetting.Factory().setMinAcceptablePossibility(0.8f).create();
        this.analyzer = MLAnalyzerFactory.getInstance().getRemoteImageClassificationAnalyzer(cloudSetting);
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.classification_image);
        // Create an MLFrame by using the bitmap.
        MLFrame frame = MLFrame.fromBitmap(bitmap);
                // Set ApiKey.
        MLApplication.getInstance().setApiKey(MainActivity.apiKey);
        Task<List<MLImageClassification>> task = this.analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<List<MLImageClassification>>() {
            @Override
            public void onSuccess(List<MLImageClassification> classifications) {
                // Recognition success.
                ImageClassificationAnalyseActivity.this.displaySuccess(classifications);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Recognition failure.
                ImageClassificationAnalyseActivity.this.displayFailure(e);
            }
        });
    }

    private void displayFailure() {
        this.mTextView.setText("Failure");
    }

    private void displayFailure(Exception exception) {
        String error = "Failure. ";
        try {
            MLException mlException = (MLException)exception;
            error += "error code: " + mlException.getErrCode() + "\n" + "error message: " + mlException.getMessage();
        } catch (Exception e) {
            error += e.getMessage();
        }
        this.mTextView.setText(error);
    }

    private void displaySuccess(List<MLImageClassification> classifications) {
        String result = "";
        int count = 0;
        for (MLImageClassification classification : classifications) {
            count++;
            if (count % 3 == 0) {
                result += classification.getName() + "\n";
            } else {
                result += classification.getName() + "\t\t\t\t\t\t";
            }
        }
        this.mTextView.setText(result);
    }

    @Override
    public void onClick(View v) {
        this.localAnalyzer();
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
            Log.e(ImageClassificationAnalyseActivity.TAG, "Stop failed: " + e.getMessage());
        }
    }
}
