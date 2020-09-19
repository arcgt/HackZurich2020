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

package com.huawei.mlkit.example.bankCard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hms.mlplugin.card.bcr.MLBcrCapture;
import com.huawei.hms.mlplugin.card.bcr.MLBcrCaptureConfig;
import com.huawei.hms.mlplugin.card.bcr.MLBcrCaptureFactory;
import com.huawei.hms.mlplugin.card.bcr.MLBcrCaptureResult;
import com.huawei.hackzurich.R;

/**
 * It provides the identification function of the bank card,
 * and recognizes formatted text information from the images with bank card information.
 * Bank Card identification provides on-device API.
 */
public class BcrAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = BcrAnalyseActivity.class.getSimpleName();
    private int CAMERA_PERMISSION_CODE = 1;
    private int READ_EXTERNAL_STORAGE_CODE = 2;
    public final int REQUEST_CODE_BK = 3;
    private TextView mTextView;

    private ImageView previewImage;

    private String cardResultFront = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_bcr_analyse);
        this.mTextView = this.findViewById(R.id.text_result);
        this.previewImage = this.findViewById(R.id.Bank_Card_image);
        this.previewImage.setScaleType(ImageView.ScaleType.FIT_XY);
        this.findViewById(R.id.detect).setOnClickListener(this);
        this.findViewById(R.id.detect2).setOnClickListener(this);
        this.previewImage.setOnClickListener(this);
        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
            this.requestCameraPermission();
        }
        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            this.requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, this.CAMERA_PERMISSION_CODE);
        }
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, permissions, this.READ_EXTERNAL_STORAGE_CODE);
        }
    }

    @Override
    public void onClick(View v) {
        this.mTextView.setText("");
        if (v.getId() == R.id.detect2) {
            // Jump to custom interface
            Intent intent = new Intent(BcrAnalyseActivity.this, CustomActivity.class);
            startActivityForResult(intent, REQUEST_CODE_BK);
        } else {
            this.startCaptureActivity(this.banCallback);
        }
    }

    private String formatIdCardResult(MLBcrCaptureResult bankCardResult) {
        StringBuilder resultBuilder = new StringBuilder();

        resultBuilder.append("Number：");
        resultBuilder.append(bankCardResult.getNumber());
        resultBuilder.append("\r\n");

        resultBuilder.append("Issuer：");
        resultBuilder.append(bankCardResult.getIssuer());
        resultBuilder.append("\r\n");

        resultBuilder.append("Expire: ");
        resultBuilder.append(bankCardResult.getExpire());
        resultBuilder.append("\r\n");

        resultBuilder.append("Type: ");
        resultBuilder.append(bankCardResult.getType());
        resultBuilder.append("\r\n");

        resultBuilder.append("Organization: ");
        resultBuilder.append(bankCardResult.getOrganization());
        resultBuilder.append("\r\n");

        Log.i(BcrAnalyseActivity.TAG, "front result: " + resultBuilder.toString());
        return resultBuilder.toString();
    }

    private void displayFailure() {
        this.mTextView.setText("Failure");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Use the bank card pre-processing plug-in to identify video stream bank cards.
     * Create a recognition result callback function to process the identification result of the card.
     */
    private MLBcrCapture.Callback banCallback = new MLBcrCapture.Callback() {
        // Identify successful processing.
        @Override
        public void onSuccess(MLBcrCaptureResult bankCardResult) {
            Log.i(BcrAnalyseActivity.TAG, "CallBack onRecSuccess");
            if (bankCardResult == null) {
                Log.i(BcrAnalyseActivity.TAG, "CallBack onRecSuccess idCardResult is null");
                return;
            }
            Bitmap bitmap = bankCardResult.getOriginalBitmap();
            BcrAnalyseActivity.this.previewImage.setImageBitmap(bitmap);
            BcrAnalyseActivity.this.cardResultFront = BcrAnalyseActivity.this.formatIdCardResult(bankCardResult);
            BcrAnalyseActivity.this.mTextView.setText(BcrAnalyseActivity.this.cardResultFront);
        }

        // User cancellation processing.
        @Override
        public void onCanceled() {
            Log.i(BcrAnalyseActivity.TAG, "CallBackonRecCanceled");
        }

        // Identify failure processing.
        @Override
        public void onFailure(int retCode, Bitmap bitmap) {
            BcrAnalyseActivity.this.displayFailure();
            Log.i(BcrAnalyseActivity.TAG, "CallBackonRecFailed");
        }

        @Override
        public void onDenied() {
            BcrAnalyseActivity.this.displayFailure();
            Log.i(BcrAnalyseActivity.TAG, "CallBackonCameraDenied");
        }
    };

    /**
     * Set the recognition parameters, call the recognizer capture interface for recognition,
     * and the recognition result will be returned through the callback function.
     *
     * @param Callback The callback of band cards analyse.
     */
    private void startCaptureActivity(MLBcrCapture.Callback Callback) {
        MLBcrCaptureConfig config = new MLBcrCaptureConfig.Factory()
                // Set the expected result type of bank card recognition.
                // MLBcrCaptureConfig.SIMPLE_RESULT: Recognize only the card number and effective date.
                // MLBcrCaptureConfig.ALL_RESULT: Recognize information such as the card number, effective date, card issuing bank, card organization, and card type.
                .setResultType(MLBcrCaptureConfig.RESULT_ALL)
                // Set the screen orientation of the plugin page.
                // MLBcrCaptureConfig.ORIENTATION_AUTO: Adaptive mode, the display direction is determined by the physical sensor.
                // MLBcrCaptureConfig.ORIENTATION_LANDSCAPE: Horizontal screen.
                // MLBcrCaptureConfig.ORIENTATION_PORTRAIT: Vertical screen.
                .setOrientation(MLBcrCaptureConfig.ORIENTATION_AUTO)
                .create();
        MLBcrCapture bcrCapture = MLBcrCaptureFactory.getInstance().getBcrCapture(config);
        bcrCapture.captureFrame(this, Callback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.i(TAG, "onActivityResult requestCode " + requestCode + ", resultCode " + resultCode);
        // Handle the recognition results of the custom interface
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_BK) {
                Bitmap bitmap = intent.getParcelableExtra("bitmap");
                BcrAnalyseActivity.this.previewImage.setImageBitmap(bitmap);
                BcrAnalyseActivity.this.cardResultFront = formatIdCardResult(intent);
                BcrAnalyseActivity.this.mTextView.setText(BcrAnalyseActivity.this.cardResultFront);
            }
        }
    }

    private String formatIdCardResult(Intent intent) {
        StringBuilder resultBuilder = new StringBuilder();

        resultBuilder.append("Number：");
        resultBuilder.append(intent.getStringExtra("number"));
        resultBuilder.append("\r\n");

        resultBuilder.append("Issuer：");
        resultBuilder.append(intent.getStringExtra("Issuer"));
        resultBuilder.append("\r\n");

        resultBuilder.append("Expire: ");
        resultBuilder.append(intent.getStringExtra("Expire"));
        resultBuilder.append("\r\n");

        resultBuilder.append("Type: ");
        resultBuilder.append(intent.getStringExtra("Type"));
        resultBuilder.append("\r\n");

        resultBuilder.append("Organization: ");
        resultBuilder.append(intent.getStringExtra("Organization"));
        resultBuilder.append("\r\n");

        Log.i(BcrAnalyseActivity.TAG, "front result: " + resultBuilder.toString());
        return resultBuilder.toString();
    }
}