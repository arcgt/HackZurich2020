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

package com.huawei.mlkit.example.text;

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
import com.huawei.hms.mlsdk.common.MLException;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.text.MLLocalTextSetting;
import com.huawei.hms.mlsdk.text.MLRemoteTextSetting;
import com.huawei.hms.mlsdk.text.MLText;
import com.huawei.hms.mlsdk.text.MLTextAnalyzer;
import com.huawei.mlkit.example.MainActivity;
import com.huawei.hackzurich.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageTextAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = ImageTextAnalyseActivity.class.getSimpleName();

    private TextView mTextView;

    private MLTextAnalyzer analyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_text_analyse);
        this.mTextView = this.findViewById(R.id.text_result);
        this.findViewById(R.id.text_detect).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        this.localAnalyzer();
    }

    /**
     * Text recognition on the device
     */
    private void localAnalyzer() {
        // Create the text analyzer MLTextAnalyzer to recognize characters in images. You can set MLLocalTextSetting to
        // specify languages that can be recognized.
        // If you do not set the languages, only Romance languages can be recognized by default.
        // Use default parameter settings to configure the on-device text analyzer. Only Romance languages can be
        // recognized.
        // analyzer = MLAnalyzerFactory.getInstance().getLocalTextAnalyzer();
        // Use the customized parameter MLLocalTextSetting to configure the text analyzer on the device.
        MLLocalTextSetting setting = new MLLocalTextSetting.Factory()
                .setOCRMode(MLLocalTextSetting.OCR_DETECT_MODE)
                .setLanguage("en")
                .create();
        this.analyzer = MLAnalyzerFactory.getInstance()
                .getLocalTextAnalyzer(setting);
        // Create an MLFrame by using android.graphics.Bitmap.
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.text_image);
        MLFrame frame = MLFrame.fromBitmap(bitmap);
        Task<MLText> task = this.analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<MLText>() {
            @Override
            public void onSuccess(MLText text) {
                // Recognition success.
                ImageTextAnalyseActivity.this.displaySuccess(text);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Recognition failure.
                ImageTextAnalyseActivity.this.displayFailure();
            }
        });
    }

    /**
     * Text recognition on the cloud. If you want to use cloud text analyzer,
     * you need to apply for an agconnect-services.json file in the developer
     * alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
     * replacing the sample-agconnect-services.json in the project.
     */
    private void remoteAnalyzer() {
        // Set the list of languages to be recognized.
        List<String> languageList = new ArrayList();
        languageList.add("zh");
        languageList.add("en");
        // Create an analyzer. You can customize the analyzer by creating MLRemoteTextSetting
        MLRemoteTextSetting setting =
            new MLRemoteTextSetting.Factory()
                    .setTextDensityScene(MLRemoteTextSetting.OCR_COMPACT_SCENE)
                    .setLanguageList(languageList)
                    .setBorderType(MLRemoteTextSetting.ARC)
                    .create();
        this.analyzer = MLAnalyzerFactory.getInstance().getRemoteTextAnalyzer(setting);
        // Use default parameter settings.
        // analyzer = MLAnalyzerFactory.getInstance().getRemoteTextAnalyzer();
        // Create an MLFrame by using android.graphics.Bitmap.
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.text_image);
        MLFrame frame = MLFrame.fromBitmap(bitmap);
                // Set ApiKey.
        MLApplication.getInstance().setApiKey(MainActivity.apiKey);
        Task<MLText> task = this.analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<MLText>() {
            @Override
            public void onSuccess(MLText text) {
                // Recognition success.
                ImageTextAnalyseActivity.this.remoteDisplaySuccess(text);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Recognition failure.
                ImageTextAnalyseActivity.this.displayFailure(e);
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

    private void remoteDisplaySuccess(MLText mlTexts) {
        String result = "";
        List<MLText.Block> blocks = mlTexts.getBlocks();
        for (MLText.Block block : blocks) {
            List<MLText.TextLine> lines = block.getContents();
            for (MLText.TextLine line : lines) {
                List<MLText.Word> words = line.getContents();
                for (MLText.Word word : words) {
                    result += word.getStringValue() + " ";
                }
            }
            result += "\n";
        }
        this.mTextView.setText(result);
    }

    private void displaySuccess(MLText mlText) {
        String result = "";
        List<MLText.Block> blocks = mlText.getBlocks();
        for (MLText.Block block : blocks) {
            for (MLText.TextLine line : block.getContents()) {
                result += line.getStringValue() + "\n";
            }
        }
        this.mTextView.setText(result);
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
            Log.e(ImageTextAnalyseActivity.TAG, "Stop failed: " + e.getMessage());
        }
    }
}
