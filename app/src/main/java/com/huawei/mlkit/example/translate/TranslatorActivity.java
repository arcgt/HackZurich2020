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

package com.huawei.mlkit.example.translate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.common.MLApplication;
import com.huawei.hms.mlsdk.common.MLException;
import com.huawei.hms.mlsdk.langdetect.MLDetectedLang;
import com.huawei.hms.mlsdk.langdetect.MLLangDetectorFactory;
import com.huawei.hms.mlsdk.langdetect.cloud.MLRemoteLangDetector;
import com.huawei.hms.mlsdk.langdetect.cloud.MLRemoteLangDetectorSetting;
import com.huawei.hms.mlsdk.langdetect.local.MLLocalLangDetector;
import com.huawei.hms.mlsdk.langdetect.local.MLLocalLangDetectorSetting;
import com.huawei.hms.mlsdk.model.download.MLLocalModelManager;
import com.huawei.hms.mlsdk.model.download.MLModelDownloadListener;
import com.huawei.hms.mlsdk.model.download.MLModelDownloadStrategy;
import com.huawei.hms.mlsdk.translate.MLTranslateLanguage;
import com.huawei.hms.mlsdk.translate.MLTranslatorFactory;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslateSetting;
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslator;
import com.huawei.hms.mlsdk.translate.local.MLLocalTranslateSetting;
import com.huawei.hms.mlsdk.translate.local.MLLocalTranslator;
import com.huawei.hms.mlsdk.translate.local.MLLocalTranslatorModel;
import com.huawei.mlkit.example.MainActivity;
import com.huawei.hackzurich.R;

public class TranslatorActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = TranslatorActivity.class.getSimpleName();

    private TextView mTextView;

    private TextView localLanguageSport;

    private TextView cloudLanguageSport;

    private EditText mEditText;

    private MLRemoteTranslator remoteTranslator;

    private MLRemoteLangDetector remoteLangDetector;

    private MLLocalLangDetector localLangDetector;

    private MLLocalTranslator localTranslator;

    private MLLocalModelManager manager;

    private MLModelDownloadListener modelDownloadListener;

    private MLModelDownloadStrategy downloadStrategy;

    private static Map<String, String> nationAndCode = new HashMap<String, String>();

    private final static long M = 1024 * 1024;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_language_detection_translation);
        this.mTextView = this.findViewById(R.id.tv_output);
        this.mEditText = this.findViewById(R.id.et_input);
        this.localLanguageSport = this.findViewById(R.id.loaclLanguage);
        localLanguageSport.setMovementMethod(ScrollingMovementMethod.getInstance());
        this.cloudLanguageSport = this.findViewById(R.id.cloudLanguage);
        cloudLanguageSport.setMovementMethod(ScrollingMovementMethod.getInstance());
        this.findViewById(R.id.btn_local_translator).setOnClickListener(this);
        this.findViewById(R.id.btn_local_detector).setOnClickListener(this);
        this.findViewById(R.id.btn_remote_translator).setOnClickListener(this);
        this.findViewById(R.id.btn_remote_detector).setOnClickListener(this);
        this.findViewById(R.id.btn_delete_model).setOnClickListener(this);
        this.findViewById(R.id.btn_download_model).setOnClickListener(this);
        TranslatorActivity.nationAndCode = this.readNationAndCode();
        // Set ApiKey.
        MLApplication.getInstance().setApiKey(MainActivity.apiKey);
        initLocalModelManagement();
        getCloundAllLanguages();
        getLocalAllLanguages();
    }

    private void initLocalModelManagement(){
        manager = MLLocalModelManager.getInstance();

        // Listening to the download progress.
        modelDownloadListener = new MLModelDownloadListener() {
            @Override
            public void onProcess(long alreadyDownLength, long totalLength) {
                double downDone = alreadyDownLength * 1.0 / M;
                double downTotal = totalLength * 1.0 / M;
                String downD = String.format("%.2f", downDone);
                String downT = String.format("%.2f", downTotal);
                final String text = downD + "M" + "/" + downT + "M";

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TranslatorActivity.this.displaySuccess(text, true);
                    }
                });
            }
        };

        // Download the offline model required for local offline translation.
        downloadStrategy = new MLModelDownloadStrategy.Factory()
                // It is recommended that you download the package in a Wi-Fi environment.
                .needWifi()
                .create();
    }

    /**
     * Translation on the cloud. If you want to use cloud remoteTranslator,
     * you need to apply for an agconnect-services.json file in the developer
     * alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
     * replacing the sample-agconnect-services.json in the project.
     */
    private void remoteTranslator() {
        // Create an analyzer. You can customize the analyzer by creating MLRemoteTranslateSetting
        MLRemoteTranslateSetting setting =
            new MLRemoteTranslateSetting.Factory().setTargetLangCode("zh").create();
        this.remoteTranslator = MLTranslatorFactory.getInstance().getRemoteTranslator(setting);
        // Use default parameter settings.
        // analyzer = MLTranslatorFactory.getInstance().getRemoteTranslator();
        // Read text in edit box.
        String sourceText = this.mEditText.getText().toString();
        Task<String> task = this.remoteTranslator.asyncTranslate(sourceText);
        task.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String text) {
                // Recognition success.
                TranslatorActivity.this.displaySuccess(text, true);
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Recognition failure.
                TranslatorActivity.this.displayFailure(e);
            }
        });
    }

    /**
     * Text translation on the device.
     */
    private void localTranslator() {
        // Create an offline translator.
        MLLocalTranslateSetting setting = new MLLocalTranslateSetting
                .Factory()
                // Set the source language code. The ISO 639-1 standard is used. This parameter is mandatory. If this parameter is not set, an error may occur.
                .setSourceLangCode("en")
                // Set the target language code. The ISO 639-1 standard is used. This parameter is mandatory. If this parameter is not set, an error may occur.
                .setTargetLangCode("zh")
                .create();
        this.localTranslator = MLTranslatorFactory.getInstance().getLocalTranslator(setting);

        localTranslator.preparedModel(downloadStrategy, modelDownloadListener).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Called when the model package is successfully downloaded.
                String input = mEditText.getText().toString();
                final Task<String> task = localTranslator.asyncTranslate(input);
                task.addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String text) {
                        // Recognition success.
                        TranslatorActivity.this.displaySuccess(text, true);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        // Recognition failure.
                        TranslatorActivity.this.displayFailure(e);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                TranslatorActivity.this.displayFailure(e);
            }
        });
    }

    /**
     * Language detection on the cloud. If you want to use cloud language detector,
     * you need to apply for an agconnect-services.json file in the developer
     * alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
     * replacing the sample-agconnect-services.json in the project.
     */
    private void remoteLangDetection() {
        // Create an analyzer. You can customize the analyzer by creating MLRemoteTextSetting
        MLRemoteLangDetectorSetting setting = new MLRemoteLangDetectorSetting.Factory().create();
        this.remoteLangDetector = MLLangDetectorFactory.getInstance().getRemoteLangDetector(setting);
        // Use default parameter settings.
        // analyzer = MLLangDetectorFactory.getInstance().getRemoteLangDetector();
        // Read text in edit box.
        String sourceText = this.mEditText.getText().toString();
        Task<List<MLDetectedLang>> task = this.remoteLangDetector.probabilityDetect(sourceText);
        task.addOnSuccessListener(new OnSuccessListener<List<MLDetectedLang>>() {
            @Override
            public void onSuccess(List<MLDetectedLang> text) {
                // Recognition success.
                TranslatorActivity.this.LangDetectionDisplaySuccess(text);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Recognition failure.
                TranslatorActivity.this.displayFailure(e);
            }
        });
        // Returns the language code with the highest confidence, sourceText represents the language to be detected.
        /**
        Task<String> taskFirstBest = this.remoteLangDetector.firstBestDetect(sourceText);
        taskFirstBest.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String text) {
                // Recognition success.
                TranslatorActivity.this.displaySuccess(text, false);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Recognition failure.
                TranslatorActivity.this.displayFailure(e);
            }
        });
         */
    }

    // Language detection on the device.
    private void localDetectLanguage() {
        // Create a local language detector.
        MLLangDetectorFactory factory = MLLangDetectorFactory.getInstance();
        MLLocalLangDetectorSetting setting = new MLLocalLangDetectorSetting.Factory().setTrustedThreshold(0.01f).create();
        localLangDetector = factory.getLocalLangDetector(setting);
        String input = mEditText.getText().toString();
        // Return the code of the language with the highest confidence.
        localLangDetector.firstBestDetect(input).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String text) {
                // Called when language detection is successful.
                TranslatorActivity.this.displaySuccess(text, false);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Called when language detection fails.
                TranslatorActivity.this.displayFailure(e);
            }
        });

        // Return multiple language detection results, including the language codes and confidences.
        /**
        localLangDetector.probabilityDetect(input).addOnSuccessListener(new OnSuccessListener<List<MLDetectedLang>>() {
            @Override
            public void onSuccess(List<MLDetectedLang> mlDetectedLangs) {
                // Called when language detection is successful.
                LangDetectionDisplaySuccess(mlDetectedLangs);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Called when language detection fails.
                TranslatorActivity.this.displayFailure(e);
            }
        });*/
    }

    private void displayFailure(Exception exception) {
        String error = "Failure. ";
        try {
            MLException mlException = (MLException) exception;
            error += "error code: " + mlException.getErrCode() + "\n" + "error message: " + mlException.getMessage();
        } catch (Exception e) {
            error += e.getMessage();
        }
        this.mTextView.setText(error);
    }

    private void displaySuccess(String text, boolean isTranslator) {
        if (isTranslator) {
            this.mTextView.setText(text);
        } else {
            this.mTextView.setText("Language=" + TranslatorActivity.nationAndCode.get(text) + "(" + text + ").");
        }
    }

    private void LangDetectionDisplaySuccess(List<MLDetectedLang> result) {
        StringBuilder stringBuilder = new StringBuilder();
        for (MLDetectedLang recognizedLang : result) {
            String langCode = recognizedLang.getLangCode();
            float probability = recognizedLang.getProbability();
            stringBuilder.append("Language=" + TranslatorActivity.nationAndCode.get(langCode) + "(" + langCode + "), score=" + probability + ".\n");
        }
        this.mTextView.setText(stringBuilder.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.remoteLangDetector != null) {
            this.remoteLangDetector.stop();
        }
        if (this.remoteTranslator != null) {
            this.remoteTranslator.stop();
        }
        if (this.localLangDetector != null) {
            this.localLangDetector.stop();
        }
        if (this.localTranslator != null) {
            this.localTranslator.stop();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_local_translator:
                this.localTranslator();
                break;
            case R.id.btn_local_detector:
                this.localDetectLanguage();
                break;
            case R.id.btn_remote_translator:
                this.remoteTranslator();
                break;
            case R.id.btn_remote_detector:
                this.remoteLangDetection();
                break;
            case R.id.btn_delete_model:
                this.deleteModel("zh");
                break;
            case R.id.btn_download_model:
                this.downloadModel("zh" );
                break;
            default:
                break;
        }
    }

    /**
     * Obtains the languages supported by local translation.
     */
    private void getLocalAllLanguages() {
        MLTranslateLanguage.getLocalAllLanguages().addOnSuccessListener(
                new OnSuccessListener<Set<String>>() {
                    @Override
                    public void onSuccess(Set<String> strings) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Languages supported by local translation:");
                        for (String str : strings) {
                            stringBuilder.append(TranslatorActivity.nationAndCode.get(str));
                            stringBuilder.append(", ");
                        }
                        stringBuilder.append("\n");
                        localLanguageSport.setText(stringBuilder.toString());
                    }
                });
    }

    /**
     * Obtains the languages supported by cloud translation.
     */
    private void getCloundAllLanguages() {
        MLTranslateLanguage.getCloudAllLanguages().addOnSuccessListener(
                new OnSuccessListener<Set<String>>() {
                    @Override
                    public void onSuccess(Set<String> strings) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Languages supported by cloud translation:");
                        for (String str : strings) {
                            stringBuilder.append(TranslatorActivity.nationAndCode.get(str));
                            stringBuilder.append(", ");
                        }
                        stringBuilder.append("\n");
                        cloudLanguageSport.setText(stringBuilder.toString());
                    }
                });
    }

    /**
     * Read the list of languages supported by language detection.
     *
     * @return Returns a map that stores the country name and language code of the ISO 639-1.
     */
    private Map<String, String> readNationAndCode() {
        Map<String, String> nationMap = new HashMap<String, String>();
        InputStreamReader inputStreamReader = null;
        try {
            InputStream inputStream = this.getAssets().open("Country_pair_new.txt");
            inputStreamReader = new InputStreamReader(inputStream, "utf-8");
        } catch (IOException e) {
            Log.d(TranslatorActivity.TAG, "Read Country_pair_new.txt failed.");
        }
        BufferedReader reader = new BufferedReader(inputStreamReader);
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] nationAndCodeList = line.split(" ");
                if (nationAndCodeList.length == 2) {
                    nationMap.put(nationAndCodeList[1], nationAndCodeList[0]);
                }
            }
        } catch (IOException e) {
            Log.d(TranslatorActivity.TAG, "Read Country_pair_new.txt line by line failed.");
        }
        return nationMap;
    }

    private void deleteModel(String langCode) {
        MLLocalTranslatorModel model = new MLLocalTranslatorModel.Factory(langCode).create();
        manager.deleteModel(model).addOnSuccessListener(new OnSuccessListener<Void>() {
            public void onSuccess(Void aVoid) {
                // Delete success.
                TranslatorActivity.this.displaySuccess("Delete success.", true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                TranslatorActivity.this.displayFailure(e);
            }
        });
    }

    private void downloadModel(String sourceLangCode){
        MLLocalTranslatorModel model = new MLLocalTranslatorModel.Factory(sourceLangCode).create();

        manager.downloadModel(model, downloadStrategy, modelDownloadListener).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Called when the model package is successfully downloaded.
                TranslatorActivity.this.displaySuccess("download success.", true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Called when the model package fails to be downloaded.
                TranslatorActivity.this.displayFailure(e);
            }
        });
    }
}
