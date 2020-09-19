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

package com.huawei.mlkit.example.generalCard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hms.mlplugin.card.gcr.MLGcrCapture;
import com.huawei.hms.mlplugin.card.gcr.MLGcrCaptureConfig;
import com.huawei.hms.mlplugin.card.gcr.MLGcrCaptureFactory;
import com.huawei.hms.mlplugin.card.gcr.MLGcrCaptureResult;
import com.huawei.hms.mlplugin.card.gcr.MLGcrCaptureUIConfig;
import com.huawei.hackzurich.R;

/**
 * It provides the identification function of general cards,
 * and recognizes formatted text information from the images with card information.
 * General card recognition is a plugin that encapsulates text recognition.
 * Developers can integrate this plug-in to obtain the ability of card identification
 * pre-processing (quality inspection, etc.). At the same time, developers need to implement the
 * detection according to their own application scenarios Post-processing of results.
 * In this example, post-processing of the Exit-Entry Permit for Travelling to and from
 * Hong Kong and Macao is implemented for developers' reference
 */
public class GcrAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = GcrAnalyseActivity.class.getSimpleName();
    private int CAMERA_PERMISSION_CODE = 1;
    private TextView mTextView;

    private ImageView previewImage;

    private Bitmap cardImage;

    private int processMode;

    private final int HKIDPROCESS = 1;

    private final int HOMECARDPROCESS = 2;

    private final int PASSCARDPROCESS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_gcr_analyse);
        this.mTextView = this.findViewById(R.id.text_result);
        this.previewImage = this.findViewById(R.id.Card_image);
        this.previewImage.setOnClickListener(this);
        this.previewImage.setScaleType(ImageView.ScaleType.FIT_XY);
        this.findViewById(R.id.detect_picture_HKID).setOnClickListener(this);
        this.findViewById(R.id.detect_picture_homeCard).setOnClickListener(this);
        this.findViewById(R.id.detect_picture_passCard).setOnClickListener(this);
        this.findViewById(R.id.detect_video_HKID).setOnClickListener(this);
        this.findViewById(R.id.detect_video_homeCard).setOnClickListener(this);
        this.findViewById(R.id.detect_video_passCard).setOnClickListener(this);
        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
            this.requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, this.CAMERA_PERMISSION_CODE);
            return;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.detect_picture_passCard:
                this.mTextView.setText("");
                this.processMode = this.PASSCARDPROCESS;
                this.startLocalImageActivity(this.cardImage, null, this.callback);
                break;
            case R.id.detect_video_passCard:
                this.mTextView.setText("");
                this.processMode = this.PASSCARDPROCESS;
                this.startCaptureActivity(null, this.callback);
                break;
            case R.id.detect_picture_HKID:
                this.mTextView.setText("");
                this.processMode = this.HKIDPROCESS;
                this.startLocalImageActivity(this.cardImage, null, this.callback);
                break;
            case R.id.detect_video_HKID:
                this.mTextView.setText("");
                this.processMode = this.HKIDPROCESS;
                this.startCaptureActivity(null, this.callback);
                break;
            case R.id.detect_picture_homeCard:
                this.mTextView.setText("");
                this.processMode = this.HOMECARDPROCESS;
                this.startLocalImageActivity(this.cardImage, null, this.callback);
                break;
            case R.id.detect_video_homeCard:
                this.mTextView.setText("");
                this.processMode = this.HOMECARDPROCESS;
                this.startCaptureActivity(null, this.callback);
                break;
            case R.id.Card_image:
                this.mTextView.setText("");
                this.processMode = this.PASSCARDPROCESS;
                this.startTakePhotoActivity(null, this.callback);
                break;
            default:
                break;
        }
    }

    private void displaySuccess(UniversalCardResult mlIdCard) {
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("IDNum: " + mlIdCard.number + "\r\n");
        resultBuilder.append("ValidDate: " + mlIdCard.valid + "\r\n");
        this.mTextView.setText(resultBuilder.toString());
    }

    private void displayFailure() {
        this.mTextView.setText("Failure");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Use the card recognition plugin to identify cards.
     * Create a recognition result callback function to process the identification result of the card.
     */
    private MLGcrCapture.Callback callback = new MLGcrCapture.Callback() {
        // Identify successful processing.
        @Override
        public int onResult(MLGcrCaptureResult result, Object object) {
            Log.i(GcrAnalyseActivity.TAG, "callback onRecSuccess");
            if (result == null) {
                Log.e(GcrAnalyseActivity.TAG, "callback onRecSuccess result is null");
                // If result is empty, return MLGcrCaptureResult.CAPTURE_CONTINUE, and the detector will continue to detect.
                return MLGcrCaptureResult.CAPTURE_CONTINUE;
            }
            UniversalCardResult cardResult = null;

            switch (processMode) {
                case PASSCARDPROCESS:
                    PassCardProcess passCard = new PassCardProcess(result.text);
                    if (passCard != null) {
                        cardResult = passCard.getResult();
                    }
                    break;
                case HKIDPROCESS:
                    HKIdCardProcess HKIDCard = new HKIdCardProcess(result.text);
                    if (HKIDCard != null) {
                        cardResult = HKIDCard.getResult();
                    }
                    break;
                case HOMECARDPROCESS:
                    HomeCardProcess homeCard = new HomeCardProcess(result.text);
                    if (homeCard != null) {
                        cardResult = homeCard.getResult();
                    }
                    break;
                default:
                    break;
            }

            if (cardResult == null || cardResult.valid.isEmpty() || cardResult.number.isEmpty()) {
                // If detection is not successful, return MLGcrCaptureResult.CAPTURE_CONTINUE, and the detector will continue to detect.
                return MLGcrCaptureResult.CAPTURE_CONTINUE;
            }
            GcrAnalyseActivity.this.cardImage = result.cardBitmap;
            GcrAnalyseActivity.this.previewImage.setImageBitmap(GcrAnalyseActivity.this.cardImage);
            GcrAnalyseActivity.this.displaySuccess(cardResult);
            // If detection is successful, return MLGcrCaptureResult.CAPTURE_STOP, and the detector will stop to detect.
            return MLGcrCaptureResult.CAPTURE_STOP;
        }

        @Override
        public void onCanceled() {
            Log.i(GcrAnalyseActivity.TAG, "callback onRecCanceled");
        }

        @Override
        public void onFailure(int restCode, Bitmap var2) {
            GcrAnalyseActivity.this.displayFailure();
            Log.i(GcrAnalyseActivity.TAG, "callback onFailure");
        }

        @Override
        public void onDenied() {
            GcrAnalyseActivity.this.displayFailure();
            Log.i(GcrAnalyseActivity.TAG, "callback onCameraDenied");
        }
    };

    /**
     * Use the plug-in to take a picture of the card and recognize.
     *
     * @param object
     * @param callback
     */
    private void startTakePhotoActivity(Object object, MLGcrCapture.Callback callback) {
        MLGcrCaptureConfig cardConfig = new MLGcrCaptureConfig.Factory().create();
        MLGcrCaptureUIConfig uiConfig = new MLGcrCaptureUIConfig.Factory()
                .setScanBoxCornerColor(Color.BLUE)
                .setTipText("Taking EEP to HK/Macau picture")
                .setOrientation(MLGcrCaptureUIConfig.ORIENTATION_AUTO).create();
        // Create a general card identification processor using the custom interface.
        MLGcrCapture ocrManager = MLGcrCaptureFactory.getInstance().getGcrCapture(cardConfig, uiConfig);

        // Create a general card identification processor using the default interface.
        //MLGcrCapture ocrManager = MLGcrCaptureFactory.getInstance().getGcrCapture(cardConfig);

        ocrManager.capturePhoto(this, object, callback);
    }

    /**
     * Detect input card bitmap.
     *
     * @param bitmap
     * @param object
     * @param callback
     */
    private void startLocalImageActivity(Bitmap bitmap, Object object, MLGcrCapture.Callback callback) {
        if (bitmap == null) {
            this.mTextView.setText("No card image to recognition.");
            return;
        }
        MLGcrCaptureConfig config = new MLGcrCaptureConfig.Factory().create();
        MLGcrCapture ocrManager = MLGcrCaptureFactory.getInstance().getGcrCapture(config);
        ocrManager.captureImage(bitmap, object, callback);
    }

    /**
     * Set the recognition parameters, call the recognizer capture interface for recognition, and the recognition result will be returned through the callback function.
     *
     * @param callBack The callback of cards analyse.
     */
    private void startCaptureActivity(Object object, MLGcrCapture.Callback callBack) {
        MLGcrCaptureConfig cardConfig = new MLGcrCaptureConfig.Factory().create();
        MLGcrCaptureUIConfig uiConfig = new MLGcrCaptureUIConfig.Factory()
                .setScanBoxCornerColor(Color.GREEN)
                .setTipText("Recognizing, align edges")
                .setOrientation(MLGcrCaptureUIConfig.ORIENTATION_AUTO).create();
        MLGcrCapture ocrManager = MLGcrCaptureFactory.getInstance().getGcrCapture(cardConfig, uiConfig);
        ocrManager.capturePreview(this, object, callBack);
    }
}