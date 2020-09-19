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

package com.huawei.mlkit.example.handkeypoint;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.huawei.hms.mlsdk.common.LensEngine;
import com.huawei.hms.mlsdk.common.MLAnalyzer;
import com.huawei.hms.mlsdk.handkeypoint.MLHandKeypointAnalyzer;
import com.huawei.hms.mlsdk.handkeypoint.MLHandKeypointAnalyzerFactory;
import com.huawei.hms.mlsdk.handkeypoint.MLHandKeypointAnalyzerSetting;
import com.huawei.hms.mlsdk.handkeypoint.MLHandKeypoints;
import com.huawei.hackzurich.R;
import com.huawei.mlkit.example.camera.GraphicOverlay;
import com.huawei.mlkit.example.camera.LensEnginePreview;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class LiveHandKeyPointAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "LiveHandKeyPointAnalyseActivity";

    private LensEnginePreview mPreview;

    private GraphicOverlay mOverlay;

    private Button mFacingSwitch;

    private MLHandKeypointAnalyzer mAnalyzer;

    private LensEngine mLensEngine;

    private int lensType = LensEngine.BACK_LENS;

    private int mLensType;

    private boolean isFront = false;

    private boolean isPermissionRequested;

    private static final int CAMERA_PERMISSION_CODE = 0;

    private static final String[] ALL_PERMISSION =
            new String[]{
                    Manifest.permission.CAMERA,
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_handkeypoint_analyse);
        if (savedInstanceState != null) {
            mLensType = savedInstanceState.getInt("lensType");
        }
        initView();
        createHandAnalyzer();
        if (Camera.getNumberOfCameras() == 1) {
            mFacingSwitch.setVisibility(View.GONE);
        }
        // Checking Camera Permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            createLensEngine();
        } else {
            checkPermission();
        }
    }

    private void initView() {
        mPreview = findViewById(R.id.hand_preview);
        mOverlay = findViewById(R.id.hand_overlay);
        mFacingSwitch = findViewById(R.id.handswitch);
        mFacingSwitch.setOnClickListener(this);
    }

    private void createHandAnalyzer() {
        // Create a  analyzer. You can create an analyzer using the provided customized face detection parameter: MLHandKeypointAnalyzerSetting
        MLHandKeypointAnalyzerSetting setting =
                new MLHandKeypointAnalyzerSetting.Factory()
                        .setMaxHandResults(2)
                        .setSceneType(MLHandKeypointAnalyzerSetting.TYPE_ALL)
                        .create();
        mAnalyzer = MLHandKeypointAnalyzerFactory.getInstance().getHandKeypointAnalyzer(setting);
        mAnalyzer.setTransactor(new HandAnalyzerTransactor(this, mOverlay));
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {
            isPermissionRequested = true;
            ArrayList<String> permissionsList = new ArrayList<>();
            for (String perm : getAllPermission()) {
                if (PackageManager.PERMISSION_GRANTED != this.checkSelfPermission(perm)) {
                    permissionsList.add(perm);
                }
            }

            if (!permissionsList.isEmpty()) {
                requestPermissions(permissionsList.toArray(new String[0]), 0);
            }
        }
    }

    public static List<String> getAllPermission() {
        return Collections.unmodifiableList(Arrays.asList(ALL_PERMISSION));
    }

    private void createLensEngine() {
        Context context = this.getApplicationContext();
        // Create LensEngine.
        mLensEngine = new LensEngine.Creator(context, mAnalyzer)
                .setLensType(this.mLensType)
                .applyDisplayDimension(640, 480)
                .applyFps(25.0f)
                .enableAutomaticFocus(true)
                .create();
    }

    @SuppressLint("LongLogTag")
    private void startLensEngine() {
        if (this.mLensEngine != null) {
            try {
                this.mPreview.start(this.mLensEngine, this.mOverlay);
            } catch (IOException e) {
                Log.d(TAG, "Failed to start lens engine.", e);
                this.mLensEngine.release();
                this.mLensEngine = null;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        boolean hasAllGranted = true;
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.createLensEngine();
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                hasAllGranted = false;
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                    showWaringDialog();
                } else {
                    Toast.makeText(this, R.string.toast, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("lensType", this.lensType);
        super.onSaveInstanceState(outState);
    }

    private class HandAnalyzerTransactor implements MLAnalyzer.MLTransactor<MLHandKeypoints> {
        private GraphicOverlay mGraphicOverlay;

        WeakReference<LiveHandKeyPointAnalyseActivity> mMainActivityWeakReference;

        HandAnalyzerTransactor(LiveHandKeyPointAnalyseActivity mainActivity, GraphicOverlay ocrGraphicOverlay) {
            mMainActivityWeakReference = new WeakReference<>(mainActivity);
            this.mGraphicOverlay = ocrGraphicOverlay;
        }

        @Override
        public void transactResult(MLAnalyzer.Result<MLHandKeypoints> result) {
            this.mGraphicOverlay.clear();

            SparseArray<MLHandKeypoints> handKeypointsSparseArray = result.getAnalyseList();
            List<MLHandKeypoints> list = new ArrayList<>();
            for (int i = 0; i < handKeypointsSparseArray.size(); i++) {
                list.add(handKeypointsSparseArray.valueAt(i));
            }
            HandKeypointGraphic graphic = new HandKeypointGraphic(this.mGraphicOverlay, list);
            this.mGraphicOverlay.add(graphic);
        }

        @Override
        public void destroy() {
            this.mGraphicOverlay.clear();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.handswitch:
                switchCamer();
                break;
        }
    }

    private void switchCamer() {
        isFront = !isFront;
        if (this.isFront) {
            mLensType = LensEngine.FRONT_LENS;
        } else {
            mLensType = LensEngine.BACK_LENS;
        }
        if (this.mLensEngine != null) {
            this.mLensEngine.close();
        }
        this.createLensEngine();
        this.startLensEngine();
    }

    private void showWaringDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(R.string.Information_permission)
                .setPositiveButton(R.string.go_authorization, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            createLensEngine();
            startLensEngine();
        } else {
            checkPermission();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mLensEngine != null) {
            this.mLensEngine.release();
        }
        if (this.mAnalyzer != null) {
            this.mAnalyzer.stop();
        }
    }
}
