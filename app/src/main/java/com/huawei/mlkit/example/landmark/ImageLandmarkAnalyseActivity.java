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

package com.huawei.mlkit.example.landmark;

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
import com.huawei.hms.mlsdk.common.MLApplication;
import com.huawei.hms.mlsdk.common.MLCoordinate;
import com.huawei.hms.mlsdk.common.MLException;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.landmark.MLRemoteLandmark;
import com.huawei.hms.mlsdk.landmark.MLRemoteLandmarkAnalyzer;
import com.huawei.hms.mlsdk.landmark.MLRemoteLandmarkAnalyzerSetting;
import com.huawei.mlkit.example.MainActivity;
import com.huawei.hackzurich.R;

import java.io.IOException;
import java.util.List;

/* If you want to use landmark analyzer,
 you need to apply for an agconnect-services.json file in the developer alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
 replacing the sample-agconnect-services.json in the project.
 */
public class ImageLandmarkAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = ImageLandmarkAnalyseActivity.class.getSimpleName();

    private TextView mTextView;

    private MLRemoteLandmarkAnalyzer analyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_landmark_analyse);
        this.mTextView = this.findViewById(R.id.landmark_result);
        this.findViewById(R.id.landmark_detect).setOnClickListener(this);
    }

    private void analyzer() {
        // Create a landmark analyzer.
        // Use default parameter settings.
        // analyzer = MLAnalyzerFactory.getInstance().getRemoteLandmarkAnalyzer();
        // Use customized parameter settings.
        /**
         * setLargestNumOfReturns: maximum number of recognition results.
         * setPatternType: analyzer mode.
         * MLRemoteLandmarkAnalyzerSetting.STEADY_PATTERN: The value 1 indicates the stable mode.
         * MLRemoteLandmarkAnalyzerSetting.NEWEST_PATTERN: The value 2 indicates the latest mode.
         */
        MLRemoteLandmarkAnalyzerSetting settings = new MLRemoteLandmarkAnalyzerSetting.Factory()
                .setLargestNumOfReturns(1)
                .setPatternType(MLRemoteLandmarkAnalyzerSetting.STEADY_PATTERN)
                .create();
        this.analyzer = MLAnalyzerFactory.getInstance()
                .getRemoteLandmarkAnalyzer(settings);
        // Create an MLFrame by using android.graphics.Bitmap. Recommended image size: large than 640*640.
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.landmark_image);
        MLFrame mlFrame = new MLFrame.Creator().setBitmap(bitmap).create();
                // Set ApiKey.
        MLApplication.getInstance().setApiKey(MainActivity.apiKey);
        Task<List<MLRemoteLandmark>> task = this.analyzer.asyncAnalyseFrame(mlFrame);
        task.addOnSuccessListener(new OnSuccessListener<List<MLRemoteLandmark>>() {
            @Override
            public void onSuccess(List<MLRemoteLandmark> landmarkResults) {
                // Processing logic for recognition success.
                ImageLandmarkAnalyseActivity.this.displaySuccess(landmarkResults.get(0));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Processing logic for recognition failur
                ImageLandmarkAnalyseActivity.this.displayFailure(e);
            }
        });
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

    private void displaySuccess(MLRemoteLandmark landmark) {
        String result = "";
        if (landmark.getLandmark() != null) {
            result = "Landmark: " + landmark.getLandmark();
        }
        result += "\nPositions: ";
        if (landmark.getPositionInfos() != null) {
            for (MLCoordinate coordinate : landmark.getPositionInfos()) {
                result += "\nLatitude:" + coordinate.getLat();
                result += "\nLongitude:" + coordinate.getLng();
            }
        }
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
            Log.e(ImageLandmarkAnalyseActivity.TAG, "Stop failed: " + e.getMessage());
        }
    }
}
