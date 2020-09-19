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

package com.huawei.mlkit.example.custommodel;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hmf.tasks.Continuation;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.ml.common.utils.SmartLog;
import com.huawei.hms.mlsdk.common.MLApplication;
import com.huawei.hms.mlsdk.common.MLException;
import com.huawei.hms.mlsdk.custom.MLCustomLocalModel;
import com.huawei.hms.mlsdk.custom.MLCustomRemoteModel;
import com.huawei.hms.mlsdk.custom.MLModelDataType;
import com.huawei.hms.mlsdk.custom.MLModelExecutor;
import com.huawei.hms.mlsdk.custom.MLModelExecutorSettings;
import com.huawei.hms.mlsdk.custom.MLModelInputOutputSettings;
import com.huawei.hms.mlsdk.custom.MLModelInputs;
import com.huawei.hms.mlsdk.custom.MLModelOutputs;
import com.huawei.hms.mlsdk.model.download.MLLocalModelManager;
import com.huawei.hms.mlsdk.model.download.MLModelDownloadListener;
import com.huawei.hms.mlsdk.model.download.MLModelDownloadStrategy;
import com.huawei.mlkit.example.MainActivity;
import com.huawei.hackzurich.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Custom model demo activity
 *
 * @since 2020-07-24
 */
public class CustomModelActivity extends AppCompatActivity {
    private static final String TAG = "ModelexecutorerSample";

    // this model is converted from TensorFlow model, for more details please refer below:
    // https://www.tensorflow.org/lite/models/image_classification/overview
    private static final String MODEL_NAME = "mobilenet_v1_1_0_224_quant";

    private static final String LABEL_FILE_NAME = "labels_mobilenet_quant_v1_224.txt";

    private static final String MODEL_FULL_NAME = MODEL_NAME + ".ms";

    private static final int BITMAP_WIDTH = 224;

    private static final int BITMAP_HEIGHT = 224;

    private static final int OUTPUT_SIZE = 1001;

    private static final int PRINT_LENGTH = 10;


    private TreeMap<String, Float> result;

    private ArrayList<String> mLabels = new ArrayList<>();

    private Bitmap analysisBitmap;

    private MLCustomLocalModel localModel;

    private MLCustomRemoteModel remoteModel;

    private static final long M = 1024 * 1024;

    private ImageView captured;

    private TextView mTvLog;

    private Button download;

    private static final int DOWNLOAD = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_model);
        readLabels(LABEL_FILE_NAME);
        captured = findViewById(R.id.capturedImageView);
        mTvLog = findViewById(R.id.tv_log);
        download = findViewById(R.id.button_download_model);
        MLApplication.getInstance().setApiKey(MainActivity.apiKey);
        analysisBitmap = getBitmap();
        processBitmap();
    }

    /**
     * Remote model detection
     *
     * @param v the view clicked
     */
    public void onRemoteClick(View v) {
        if (analysisBitmap != null) {
            pictureAnalysis(analysisBitmap);
        }
    }

    /**
     * download model.
     *
     * @param v the view clicked
     */
    public void onDownloadClick(View v) {
        downloadModels(MODEL_NAME, DOWNLOAD);
    }

    private void readLabels(String assetFileName) {
        InputStream is = null;
        try {
            is = getAssets().open(assetFileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String readString;
            while ((readString = br.readLine()) != null) {
                mLabels.add(readString);
            }
            br.close();
        } catch (IOException error) {
            Log.e(TAG, "Asset file doesn't exist: " + error.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException error) {
                    Log.e(TAG, "close failed: " + error.getMessage());
                }
            }
        }
    }

    private void downloadModels(String remoteModelName, final int location) {
        final MLCustomRemoteModel customRemoteModel = new MLCustomRemoteModel.Factory(remoteModelName).create();
        final MLModelDownloadStrategy strategy =
                new MLModelDownloadStrategy.Factory()
                        .needWifi()
                        .setRegion(MLModelDownloadStrategy.REGION_DR_CHINA)
                        .create();
        final MLModelDownloadListener modelDownloadListener =
                new MLModelDownloadListener() {
                    @Override
                    public void onProcess(long alreadyDownLength, long totalLength) {
                        showProcess(alreadyDownLength, "DownLoad", totalLength, location);
                    }
                };
        MLLocalModelManager.getInstance().downloadModel(customRemoteModel, strategy, modelDownloadListener);
    }

    private void showProcess(long alreadyDownLength, String buttonText, long totalLength, int location) {
        double downDone = alreadyDownLength * 1.0 / M;
        double downTotal = totalLength * 1.0 / M;
        String downD = String.format("%.2f", downDone);
        String downT = String.format("%.2f", downTotal);

        String text = downD + "M" + "/" + downT + "M";
        Log.e(TAG, "string format:" + downD);
        updateButton(text, location);
        if (downD.equals(downT)) {
            updateButton(buttonText, location);
        }
    }

    private void updateButton(final String text, final int buttonSwitch) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        switch (buttonSwitch) {
                            case DOWNLOAD:
                                download.setText(text);
                                break;
                        }
                    }
                });
    }

    Bitmap getBitmap() {
        InputStream is = null;
        try {
            is = getAssets().open("image.jpg");
        } catch (IOException e) {
            Log.e(TAG, "open failed");
            return null;
        }

        Bitmap bm = BitmapFactory.decodeStream(is);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bm;
    }

    private void processBitmap() {
        if (analysisBitmap != null) {
            dumpBitmapInfo(analysisBitmap);
            final Bitmap local = analysisBitmap.createScaledBitmap(analysisBitmap, BITMAP_WIDTH, BITMAP_HEIGHT, false);
            runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (local != null) {
                                captured.setImageBitmap(local);
                            } else {
                                Toast.makeText(getApplicationContext(), "no picture", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private boolean dumpBitmapInfo(Bitmap bitmap) {
        if (bitmap == null) {
            return true;
        }
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        Log.e(TAG, "bitmap width is " + width + " height " + height);
        return false;
    }

    private void pictureAnalysis(final Bitmap bitmap) {
        localModel = new MLCustomLocalModel.Factory(MODEL_NAME).setAssetPathFile(MODEL_FULL_NAME).create();
        remoteModel = new MLCustomRemoteModel.Factory(MODEL_NAME).create();
        downloadModels(MODEL_NAME, DOWNLOAD);
        MLLocalModelManager.getInstance()
                .isModelExist(remoteModel)
                .continueWithTask(new Continuation<Boolean, Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<Boolean> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        boolean isDownloaded = task.getResult();
                        MLModelExecutorSettings settings;
                        if (isDownloaded) {
                            settings = new MLModelExecutorSettings.Factory(remoteModel).create();
                        } else {
                            settings = new MLModelExecutorSettings.Factory(localModel).create();
                        }
                        try {
                            final MLModelExecutor modelExecutor = MLModelExecutor.getInstance(settings);
                            executorImpl(modelExecutor, bitmap);
                        } catch (MLException e) {
                            SmartLog.e(TAG, "create MLModelExecutor failed");
                        }
                        return null;
                    }
                });
    }

    private void executorImpl(final MLModelExecutor modelExecutor, Bitmap bitmap) {
        final Bitmap inputBitmap = Bitmap.createScaledBitmap(bitmap, BITMAP_WIDTH, BITMAP_HEIGHT, true);
        int batchNum = 0;
        final byte[][][][] input = new byte[1][3][BITMAP_HEIGHT][BITMAP_WIDTH];
        Log.d(TAG, "interpret pre process");

        // prepare the input data, if converted from tensorflow, pls use NCHW format, DO not use NHWC。
        for (int i = 0; i < BITMAP_WIDTH; i++) {
            for (int j = 0; j < BITMAP_HEIGHT; j++) {
                int pixel = inputBitmap.getPixel(i, j);
                input[batchNum][0][j][i] = (byte) Color.red(pixel);
                input[batchNum][1][j][i] = (byte) Color.green(pixel);
                input[batchNum][2][j][i] = (byte) Color.blue(pixel);
            }
        }
        MLModelInputs inputs = null;
        try {
            inputs = new MLModelInputs.Factory().add(input).create();
        } catch (MLException e) {
            Log.e(TAG, "add inputs failed! " + e.getMessage());
        }

        MLModelInputOutputSettings inOutSettings = null;
        try {
            // according to the model requirement, set the in and out format.
            inOutSettings = new MLModelInputOutputSettings.Factory()
                    .setInputFormat(0, MLModelDataType.BYTE, new int[]{1, 3, BITMAP_HEIGHT, BITMAP_WIDTH})
                    .setOutputFormat(0, MLModelDataType.BYTE, new int[]{1, OUTPUT_SIZE})
                    .create();
        } catch (MLException e) {
            Log.e(TAG, "set input output format failed! " + e.getMessage());
        }

        Log.d(TAG, "interpret start");
        modelExecutor.exec(inputs, inOutSettings)
                .addOnSuccessListener(
                        new OnSuccessListener<MLModelOutputs>() {
                            @Override
                            public void onSuccess(MLModelOutputs mlModelOutputs) {
                                Log.i(TAG, "interpret get result");
                                try {
                                    byte[][] output = mlModelOutputs.getOutput(0); // index
                                    byte[] local = output[0];
                                    float[] probabilities = getFloatRest(local);
                                    prepareResult(probabilities);

                                    // display the result
                                    StringBuilder builder = new StringBuilder();
                                    int total = 0;
                                    DecimalFormat df = new DecimalFormat("0.00%");
                                    for (Map.Entry<String, Float> entry : result.entrySet()) {
                                        if (total == PRINT_LENGTH || entry.getValue() <= 0) {
                                            break;
                                        }
                                        builder.append(entry.getKey())
                                                .append(" ")
                                                .append(df.format(entry.getValue()))
                                                .append(System.lineSeparator());
                                        total++;
                                    }
                                    postMsg("result:", builder.toString());
                                    Log.e(TAG, "result is " + builder.toString());
                                } catch (IllegalArgumentException e) {
                                    Log.e(TAG, "getOutput failed! " + e.getMessage());
                                }

                                // release the resource
                                try {
                                    modelExecutor.close();
                                } catch (IOException ex) {
                                    Log.e(TAG, "close failed! " + ex.getMessage());
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "interpret failed, because " + e.getMessage());
                                postMsg(TAG, "interpret failed");
                                try {
                                    modelExecutor.close();
                                } catch (IOException ex) {
                                    Log.e(TAG, "close failed! " + ex.getMessage());
                                }
                            }
                        });
    }

    private float[] getFloatRest(byte[] byteResult) {
        int length = byteResult.length;
        if (length > 0) {
            float[] local = new float[length];
            for (int i = 0; i < length; i++) {
                local[i] = byteResult[i] / 255f;
            }
            return local;
        }
        return new float[0];
    }

    private void prepareResult(float[] probabilities) {
        Map<String, Float> localResult = new HashMap<>();
        ValueComparator compare = new ValueComparator(localResult);
        for (int i = 0; i < OUTPUT_SIZE; i++) {
            localResult.put(mLabels.get(i), probabilities[i]);
        }
        result = new TreeMap<>(compare);
        result.putAll(localResult);

    }

    private void postMsg(final String tag, final String msg) {
        if (mTvLog == null) {
            return;
        }
        mTvLog.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mTvLog == null) {
                            return;
                        }
                        mTvLog.setText(tag + System.lineSeparator() + msg);
                    }
                });
    }

    class ValueComparator implements Comparator<String> {
        Map<String, Float> base;

        ValueComparator(Map<String, Float> base) {
            this.base = base;
        }

        @Override
        public int compare(String o1, String o2) {
            if (base.get(o1) >= base.get(o2)) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}