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

package com.huawei.mlkit.example.document;

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
import com.huawei.hms.mlsdk.document.MLDocument;
import com.huawei.hms.mlsdk.document.MLDocumentAnalyzer;
import com.huawei.hms.mlsdk.document.MLDocumentSetting;
import com.huawei.hms.mlsdk.text.MLRemoteTextSetting;
import com.huawei.mlkit.example.MainActivity;
import com.huawei.hackzurich.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* If you want to use document analyzer,
 you need to apply for an agconnect-services.json file in the developer alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
 replacing the sample-agconnect-services.json in the project.
 */
public class ImageDocumentAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = ImageDocumentAnalyseActivity.class.getSimpleName();

    private TextView mTextView;

    private MLDocumentAnalyzer analyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_document_analyse);
        this.mTextView = this.findViewById(R.id.document_result);
        this.findViewById(R.id.document_detect).setOnClickListener(this);
    }

    private void analyzer() {
        // Set the list of languages to be recognized.
        List<String> languageList = new ArrayList();
        languageList.add("zh");
        languageList.add("en");
        // Create a document analyzer. You can create an analyzer using the provided custom document recognition
        // parameter MLDocumentSetting
        MLDocumentSetting setting = new MLDocumentSetting.Factory()
                .setBorderType(MLRemoteTextSetting.ARC)
                .setLanguageList(languageList)
                .create();
        // Create a document analyzer that uses the customized configuration.
        this.analyzer = MLAnalyzerFactory.getInstance().getRemoteDocumentAnalyzer(setting);

        // Create a document analyzer that uses the default configuration.
        // analyzer = MLAnalyzerFactory.getInstance().getRemoteDocumentAnalyzer();
        // Pass the MLFrame object to the asyncAnalyseFrame method for document recognition.
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.document_image);
        MLFrame frame = MLFrame.fromBitmap(bitmap);
                // Set ApiKey.
        MLApplication.getInstance().setApiKey(MainActivity.apiKey);
        Task<MLDocument> task = this.analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<MLDocument>() {
            @Override
            public void onSuccess(MLDocument document) {
                // Recognition success.
                ImageDocumentAnalyseActivity.this.displaySuccess(document);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Recognition failure.
                ImageDocumentAnalyseActivity.this.displayFailure(e);
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

    private void displaySuccess(MLDocument document) {
        String result = "";
        List<MLDocument.Block> blocks = document.getBlocks();
        for (MLDocument.Block block : blocks) {
            List<MLDocument.Section> sections = block.getSections();
            for (MLDocument.Section section : sections) {
                List<MLDocument.Line> lines = section.getLineList();
                for (MLDocument.Line line : lines) {
                    List<MLDocument.Word> words = line.getWordList();
                    for (MLDocument.Word word : words) {
                        result += word.getStringValue() + " ";
                    }
                }
            }
            result += "\n";
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
            Log.e(ImageDocumentAnalyseActivity.TAG, "Stop failed: " + e.getMessage());
        }
    }
}
