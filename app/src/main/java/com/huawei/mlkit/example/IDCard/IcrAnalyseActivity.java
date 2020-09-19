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

package com.huawei.mlkit.example.IDCard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlplugin.card.icr.cn.MLCnIcrCapture;
import com.huawei.hms.mlplugin.card.icr.cn.MLCnIcrCaptureConfig;
import com.huawei.hms.mlplugin.card.icr.cn.MLCnIcrCaptureFactory;
import com.huawei.hms.mlplugin.card.icr.cn.MLCnIcrCaptureResult;
import com.huawei.hms.mlsdk.card.MLCardAnalyzerFactory;
import com.huawei.hms.mlsdk.card.icr.MLIcrAnalyzer;
import com.huawei.hms.mlsdk.card.icr.MLIcrAnalyzerSetting;
import com.huawei.hms.mlsdk.card.icr.MLIdCard;
import com.huawei.hms.mlsdk.card.icr.cloud.MLRemoteIcrAnalyzer;
import com.huawei.hms.mlsdk.card.icr.cloud.MLRemoteIcrAnalyzerSetting;
import com.huawei.hms.mlsdk.common.MLApplication;
import com.huawei.hms.mlsdk.common.MLException;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.mlkit.example.MainActivity;
import com.huawei.hackzurich.R;

import java.io.IOException;

/**
 * It provides the identification function of the second-generation ID card of Chinese residents,
 * and recognizes formatted text information from the images with ID card information.
 * ID Card identification provides on-cloud and on-device API.
 */
public class IcrAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = IcrAnalyseActivity.class.getSimpleName();
    private int CAMERA_PERMISSION_CODE = 1;
    private TextView mTextView;

    private boolean isFront;

    private ImageView previewImageFront;

    private ImageView previewImageBack;

    private MLIcrAnalyzer localAnalyzer;

    private MLRemoteIcrAnalyzer remoteIcrAnalyzer;

    private Bitmap cardFront;

    private Bitmap cardBack;

    private String cardResultFront = "";

    private String cardResultBack = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_icr_analyse);
        this.mTextView = this.findViewById(R.id.text_result);
        this.previewImageFront = this.findViewById(R.id.IDCard_image_front);
        this.previewImageBack = this.findViewById(R.id.IDCard_image_back);
        this.previewImageFront.setScaleType(ImageView.ScaleType.FIT_XY);
        this.previewImageBack.setScaleType(ImageView.ScaleType.FIT_XY);
        this.findViewById(R.id.detect).setOnClickListener(this);
        this.previewImageFront.setOnClickListener(this);
        this.previewImageBack.setOnClickListener(this);
        if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
            this.requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[] {Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, this.CAMERA_PERMISSION_CODE);
            return;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.detect:
                this.mTextView.setText("");
                // Use plugins for static image detection.
                this.startCaptureImageActivity(this.idCallBack, true, true);
                // Use SDK for on-device static image detection.
                // this.localAnalyzer();
                // Use SDK for on-cloud static image detection.
                // this.remoteAnalyzer();
                break;
            case R.id.IDCard_image_front:
                this.isFront = true;
                this.mTextView.setText("");
                // Use plugins for camera stream detection.
                this.startCaptureActivity(this.idCallBack, true, false);
                break;
            case R.id.IDCard_image_back:
                this.isFront = false;
                this.mTextView.setText("");
                // Use plugins for camera stream detection.
                this.startCaptureActivity(this.idCallBack, false, false);
                break;
            default:
                break;
        }
    }

    /**
     * Icr analyse on the cloud. If you want to use product search analyzer,
     * you need to apply for an agconnect-services.json file in the developer
     * alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
     * replacing the sample-agconnect-services.json in the project.
     */
    private void remoteAnalyzer() {
        if (this.cardFront == null) {
            this.mTextView.setText("Please take the front photo of IDCard.");
            return;
        }
        // Use customized parameter settings for cloud-based recognition.
        MLRemoteIcrAnalyzerSetting setting =
            new MLRemoteIcrAnalyzerSetting.Factory()
                    .setSideType(MLRemoteIcrAnalyzerSetting.FRONT)
                    .create();
        this.remoteIcrAnalyzer = MLCardAnalyzerFactory.getInstance().getRemoteIcrAnalyzer(setting);
        // Create an MLFrame by using the bitmap. Recommended image size: large than 512*512.
        Bitmap bitmap = this.cardFront;
        MLFrame frame = MLFrame.fromBitmap(bitmap);
                // Set ApiKey.
        MLApplication.getInstance().setApiKey(MainActivity.apiKey);
        Task<MLIdCard> task = this.remoteIcrAnalyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<MLIdCard>() {
            @Override
            public void onSuccess(MLIdCard mlIdCard) {
                // Recognition success.
                IcrAnalyseActivity.this.displaySuccess(mlIdCard, true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Recognition failure.
                IcrAnalyseActivity.this.displayFailure(e);
            }
        });
    }

    private void localAnalyzer() {
        if (this.cardFront == null) {
            this.mTextView.setText("Please take the front photo of IDCard.");
            return;
        }
        // Use customized parameter settings for device-based recognition.
        MLIcrAnalyzerSetting setting = new MLIcrAnalyzerSetting.Factory()
                .setSideType(MLIcrAnalyzerSetting.FRONT)
                .create();
        this.localAnalyzer = MLCardAnalyzerFactory.getInstance().getIcrAnalyzer(setting);
        // Create an MLFrame by using the bitmap. Recommended image size: large than 512*512.
        Bitmap bitmap = this.cardFront;
        MLFrame frame = MLFrame.fromBitmap(bitmap);
        Task<MLIdCard> task = this.localAnalyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<MLIdCard>() {
            @Override
            public void onSuccess(MLIdCard mlIdCard) {
                // Recognition success.
                IcrAnalyseActivity.this.displaySuccess(mlIdCard, true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Recognition failure.
                IcrAnalyseActivity.this.displayFailure(e.getMessage());
            }
        });
    }

    private void displaySuccess(MLIdCard mlIdCard, boolean isFront) {
        StringBuilder resultBuilder = new StringBuilder();
        if (isFront) {
            resultBuilder.append("Name：" + mlIdCard.getName() + "\r\n");
            resultBuilder.append("Sex：" + mlIdCard.getSex() + "\r\n");
            resultBuilder.append("IDNum: " + mlIdCard.getIdNum() + "\r\n");
        } else {
            resultBuilder.append("ValidDate: " + mlIdCard.getValidDate() + "\r\n");
        }
        this.mTextView.setText(resultBuilder.toString());
    }

    private String formatIdCardResult(MLCnIcrCaptureResult idCardResult, boolean isFront) {
        StringBuilder resultBuilder = new StringBuilder();
        if (isFront) {
            resultBuilder.append("Name：" + idCardResult.name + "\r\n");
            resultBuilder.append("Sex：" + idCardResult.sex + "\r\n");
            resultBuilder.append("IDNum: " + idCardResult.idNum + "\r\n");
        } else {
            resultBuilder.append("ValidDate: " + idCardResult.validDate + "\r\n");
        }
        return resultBuilder.toString();
    }

    private void displayFailure(String str) {
        this.mTextView.setText("Failure. " + str);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.remoteIcrAnalyzer != null) {
            try {
                this.remoteIcrAnalyzer.stop();
            } catch (IOException e) {
                Log.d(TAG, "stop exception.");
            }
        }
        if (this.localAnalyzer != null) {
            try {
                this.localAnalyzer.stop();
            } catch (IOException e) {
                Log.d(IcrAnalyseActivity.TAG, "Stop failed:" + e.getMessage());
            }
        }
    }

    /**
     * Use the Chinese second-generation ID card pre-processing plug-in to identify video stream ID cards.
     * Create a recognition result callback function to process the identification result of the ID card.
     */
    private MLCnIcrCapture.CallBack idCallBack = new MLCnIcrCapture.CallBack() {
        // Identify successful processing.
        @Override
        public void onSuccess(MLCnIcrCaptureResult idCardResult) {
            Log.i(IcrAnalyseActivity.TAG, "IdCallBack onRecSuccess");
            if (idCardResult == null) {
                Log.i(IcrAnalyseActivity.TAG, "IdCallBack onRecSuccess idCardResult is null");
                return;
            }
            Bitmap bitmap = idCardResult.cardBitmap;
            if (IcrAnalyseActivity.this.isFront) {
                IcrAnalyseActivity.this.cardFront = bitmap;
                IcrAnalyseActivity.this.previewImageFront.setImageBitmap(bitmap);
                IcrAnalyseActivity.this.cardResultFront =
                    IcrAnalyseActivity.this.formatIdCardResult(idCardResult, true);
            } else {
                IcrAnalyseActivity.this.cardBack = bitmap;
                IcrAnalyseActivity.this.previewImageBack.setImageBitmap(bitmap);
                IcrAnalyseActivity.this.cardResultBack =
                    IcrAnalyseActivity.this.formatIdCardResult(idCardResult, false);
            }
            if (!(IcrAnalyseActivity.this.cardResultFront.equals("")
                && IcrAnalyseActivity.this.cardResultBack.equals(""))) {
                IcrAnalyseActivity.this.mTextView.setText(IcrAnalyseActivity.this.cardResultFront);
                IcrAnalyseActivity.this.mTextView.append(IcrAnalyseActivity.this.cardResultBack);
            }
        }

        // User cancellation processing.
        @Override
        public void onCanceled() {
            IcrAnalyseActivity.this.displayFailure("IdCallBackonRecCanceled");
            Log.i(IcrAnalyseActivity.TAG, "IdCallBackonRecCanceled");
        }

        // Identify failure processing.
        @Override
        public void onFailure(int retCode, Bitmap bitmap) {
            IcrAnalyseActivity.this.displayFailure("IdCallBackonRecFailed. " + "retcode is :" + retCode);
            Log.i(IcrAnalyseActivity.TAG, "IdCallBackonRecFailed");
        }

        // Camera unavailable processing, the reason that the camera is unavailable is generally that the user has not been granted camera permissions.
        @Override
        public void onDenied() {
            IcrAnalyseActivity.this.displayFailure("IdCallBackonCameraDenied");
            Log.i(IcrAnalyseActivity.TAG, "IdCallBackonCameraDenied");
        }
    };

    /**
     * Set the recognition parameters, call the recognizer capture interface for recognition, and the recognition result will be returned through the callback function.
     * @param callback The callback of ID cards analyse.
     * @param isFront Whether it is the front of the ID card.
     * @param isRemote Whether to use the on-cloud model, true: use on-cloud service. false: use on-device service.
     */
    private void startCaptureActivity(MLCnIcrCapture.CallBack callback, boolean isFront, boolean isRemote) {
        MLCnIcrCaptureConfig config = new MLCnIcrCaptureConfig.Factory()
                .setFront(isFront)
                .setRemote(isRemote)
                .create();
        MLCnIcrCapture icrCapture = MLCnIcrCaptureFactory.getInstance().getIcrCapture(config);
        icrCapture.capture(callback, this);
    }

    /**
     * Set the recognition parameters, call the recognizer captureImage interface for recognition, and the recognition result will be returned through the callback function.
     * @param callback The callback of ID cards analyse.
     * @param isFront Whether it is the front of the ID card.
     * @param isRemote Whether to use the on-cloud model, true: use on-cloud service. false: use on-device service.
     */
    private void startCaptureImageActivity(MLCnIcrCapture.CallBack callback, boolean isFront, boolean isRemote) {
        if (this.cardFront == null) {
            this.mTextView.setText("Please take the front photo of IDCard.");
            return;
        }
        this.isFront = isFront;
        MLCnIcrCaptureConfig config = new MLCnIcrCaptureConfig.Factory()
                .setFront(isFront)
                .setRemote(isRemote)
                .create();
        MLCnIcrCapture icrCapture = MLCnIcrCaptureFactory.getInstance().getIcrCapture(config);
        icrCapture.captureImage(this.cardFront, callback);
    }
}