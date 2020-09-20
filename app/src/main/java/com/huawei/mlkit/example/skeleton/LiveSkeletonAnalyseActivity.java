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

package com.huawei.mlkit.example.skeleton;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hms.mlsdk.common.LensEngine;
import com.huawei.hms.mlsdk.common.MLAnalyzer;
import com.huawei.hms.mlsdk.skeleton.MLJoint;
import com.huawei.hms.mlsdk.skeleton.MLSkeleton;
import com.huawei.hms.mlsdk.skeleton.MLSkeletonAnalyzer;
import com.huawei.hms.mlsdk.skeleton.MLSkeletonAnalyzerFactory;
import com.huawei.hackzurich.R;
import com.huawei.mlkit.example.camera.GraphicOverlay;
import com.huawei.mlkit.example.camera.LensEnginePreview;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static java.lang.String.valueOf;

public class LiveSkeletonAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = LiveSkeletonAnalyseActivity.class.getSimpleName();

    public static final int UPDATE_VIEW = 101;

    private static final int CAMERA_PERMISSION_CODE = 0;

    private Handler mHandler = new MsgHandler(this);

    private MLSkeletonAnalyzer analyzer;

    private LensEngine mLensEngine;

    private LensEnginePreview mPreview;

    //our beautiful added content
    private GraphicOverlay graphicOverlay;
    private ImageView templateImgView;
    private TextView similarityTxt;
    public TextView timerTxt;
    public TextView wordTxt;
    public TextView translationTxt;

    private TextView redBox;
    private TextView greenBox;
    private TextView halfGreenBox;

    private Runnable gameRunner;
    private Thread gameThread;

    private static long startTime;
    private static long elapsed = 0;
    private static int health = 3;
    private static int points = 0;
    private static boolean isCorrectPosture = false;
    private static boolean isCorrectTranslation = false;
    private static double meanSimilarity = 0;
    private static String currentWord = "你好";
    private static String currentTranslation = "Hello";
    private static String[] words = {"你好","快乐","强"};
    private static String[] translations = {"hello", "happy", "strong"};



    public static float[][] current_skeletons = {{416.6629f, 312.46442f, 101, 0.8042025f}, {382.3348f, 519.43396f, 102, 0.86383355f}, {381.0387f, 692.09515f, 103, 0.7551306f}
            , {659.49194f, 312.24445f, 104, 0.8305682f}, {693.5356f, 519.4844f, 105, 0.8932837f}, {694.0054f, 692.4169f, 106, 0.8742422f}
            , {485.08786f, 726.8787f, 107, 0.6004682f}, {485.02808f, 935.4897f, 108, 0.7334503f}, {485.09384f, 1177.127f, 109, 0.67240065f}
            , {623.7807f, 726.7474f, 110, 0.5483011f}, {624.5828f, 936.3222f, 111, 0.730425f}, {625.81915f, 1212.2491f, 112, 0.72417295f}
            , {521.47363f, 103.95903f, 113, 0.7780853f}, {521.6231f, 277.2533f, 114, 0.7745689f}};

    private int lensType = LensEngine.BACK_LENS;

    private boolean isFront = false;

    private List<MLSkeleton> templateList;

    private boolean isPermissionRequested;


    private static final String[] ALL_PERMISSION =
            new String[]{
                    Manifest.permission.CAMERA,
            };


    // coordinates for the bones of the image template
    public static float[][] TMP_SKELETONS = {{416.6629f, 312.46442f, 101, 0.8042025f}, {382.3348f, 519.43396f, 102, 0.86383355f}, {381.0387f, 692.09515f, 103, 0.7551306f}
            , {659.49194f, 312.24445f, 104, 0.8305682f}, {693.5356f, 519.4844f, 105, 0.8932837f}, {694.0054f, 692.4169f, 106, 0.8742422f}
            , {485.08786f, 726.8787f, 107, 0.6004682f}, {485.02808f, 935.4897f, 108, 0.7334503f}, {485.09384f, 1177.127f, 109, 0.67240065f}
            , {623.7807f, 726.7474f, 110, 0.5483011f}, {624.5828f, 936.3222f, 111, 0.730425f}, {625.81915f, 1212.2491f, 112, 0.72417295f}
            , {521.47363f, 103.95903f, 113, 0.7780853f}, {521.6231f, 277.2533f, 114, 0.7745689f}};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        startTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_live_skeleton_analyse);
        if (savedInstanceState != null) {
            this.lensType = savedInstanceState.getInt("lensType");
        }
        this.mPreview = this.findViewById(R.id.skeleton_preview);
        this.graphicOverlay = this.findViewById(R.id.skeleton_overlay);
        templateImgView = this.findViewById(R.id.template_imgView);
        templateImgView.setImageResource(R.drawable.skeleton_template);
        similarityTxt = this.findViewById(R.id.similarity_txt);
        timerTxt = this.findViewById(R.id.timer_txt);
        wordTxt = this.findViewById(R.id.word_txt);
        translationTxt = this.findViewById(R.id.translation_txt);
        redBox = this.findViewById(R.id.red);
        greenBox = this.findViewById(R.id.green);

        //halfGreenBox = this.findViewById(R.id.half_green);

        this.createSkeletonAnalyzer();
        Button facingSwitchBtn = this.findViewById(R.id.skeleton_facingSwitch);
        if (Camera.getNumberOfCameras() == 1) {
            facingSwitchBtn.setVisibility(View.GONE);
        }
        facingSwitchBtn.setOnClickListener(this);

        initTemplateData();

        // Checking Camera Permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            this.createLensEngine();
        } else {
            this.checkPermission();
        }
    }

    @Override
    protected void onResume() { 
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            this.createLensEngine();
            this.startLensEngine();
        } else {
            this.checkPermission();
        }
    }

    private void createSkeletonAnalyzer() {
        this.analyzer = MLSkeletonAnalyzerFactory.getInstance().getSkeletonAnalyzer();
        this.analyzer.setTransactor(new SkeletonAnalyzerTransactor(this, this.graphicOverlay));
    }

    private void createLensEngine() {
        Context context = this.getApplicationContext();
        // Create LensEngine.
        this.mLensEngine = new LensEngine.Creator(context, this.analyzer)
                .setLensType(this.lensType)
                .applyDisplayDimension(1280, 720)
                .applyFps(20.0f)
                .enableAutomaticFocus(true)
                .create();
    }

    private void startLensEngine() {
        if (this.mLensEngine != null) {
            try {
                this.mPreview.start(this.mLensEngine, this.graphicOverlay);
            } catch (IOException e) {
                Log.e(LiveSkeletonAnalyseActivity.TAG, "Failed to start lens engine.", e);
                this.mLensEngine.release();
                this.mLensEngine = null;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mPreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mLensEngine != null) {
            this.mLensEngine.release();
        }
        if (this.analyzer != null) {
            try {
                this.analyzer.stop();
            } catch (IOException e) {
                Log.e(LiveSkeletonAnalyseActivity.TAG, "Stop failed: " + e.getMessage());
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
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("lensType", this.lensType);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        this.isFront = !this.isFront;
        if (this.isFront) {
            this.lensType = LensEngine.FRONT_LENS;
        } else {
            this.lensType = LensEngine.BACK_LENS;
        }
        if (this.mLensEngine != null) {
            this.mLensEngine.close();
        }
        this.createLensEngine();
        this.startLensEngine();
    }

    private void initTemplateData() {
        if (templateList != null) {
            return;
        }
        List<MLJoint> mlJointList = new ArrayList<>();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.skeleton_template);
        for (int i = 0; i < TMP_SKELETONS.length; i++) {
            MLJoint mlJoint = new MLJoint(bitmap.getWidth() * TMP_SKELETONS[i][0],
                    bitmap.getHeight() * TMP_SKELETONS[i][1], (int)TMP_SKELETONS[i][2], TMP_SKELETONS[i][3]);
            mlJointList.add(mlJoint);
        }

        templateList = new ArrayList<>();
        templateList.add(new MLSkeleton(mlJointList));
    }

    /**
     * Compute Similarity
     *
     * @param skeletons
     */
    private void compareSimilarity(List<MLSkeleton> skeletons) {
        if (templateList == null) {
            return;
        }

        float similarity = 0f;
        float result = analyzer.caluteSimilarity(skeletons, templateList);
        if (result > similarity) {
            similarity = result;
        }

        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putFloat("similarity", similarity);
        msg.setData(bundle);
        msg.what = this.UPDATE_VIEW;
        mHandler.sendMessage(msg);
        //        Log.d("skeleton", skeletons[0].)

    }

    private static class MsgHandler extends Handler {
        WeakReference<LiveSkeletonAnalyseActivity> mMainActivityWeakReference;

        MsgHandler(LiveSkeletonAnalyseActivity mainActivity) {
            mMainActivityWeakReference = new WeakReference<>(mainActivity);
        }


        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LiveSkeletonAnalyseActivity mainActivity = mMainActivityWeakReference.get();
            if (mainActivity == null || mainActivity.isFinishing()) {
                return;
            }
            if (msg.what == UPDATE_VIEW) {
                Bundle bundle = msg.getData();
                float result = bundle.getFloat("similarity");
                mainActivity.similarityTxt.setVisibility(View.VISIBLE);
//                mainActivity.similarityTxt.setText("similarity:" + (int) (result * 100) + "%");

                meanSimilarity = 0.9 * meanSimilarity + 0.1 * (result);
                mainActivity.similarityTxt.setText("" + (int) (meanSimilarity * 100) + "%");

                long currentTime = System.currentTimeMillis();
                long newElapsed = (System.currentTimeMillis() - startTime) / 1000;
                if (newElapsed != elapsed) {
                    elapsed = newElapsed;
                    Log.d("michael", "elapsed" + valueOf(elapsed));
                    int remaining = 9 - (int) (elapsed % 10);
                    mainActivity.timerTxt.setText((remaining) + "");

                    if (remaining == 0) {

                        //TODO add correct sound
                        //calculate and display results
                        if (meanSimilarity >= 0.5) {
                            //correct posture
                            mainActivity.greenBox.setVisibility(View.VISIBLE);
                        } else {
                            //incorrect
                            mainActivity.redBox.setVisibility(View.VISIBLE);
                        }
                        mainActivity.translationTxt.setVisibility(View.VISIBLE);
                        mainActivity.timerTxt.setVisibility(View.INVISIBLE);
                    } else if (remaining == 8) {
                        //display next round
                        mainActivity.timerTxt.setVisibility(View.VISIBLE);
                        Random Dice = new Random();
                        int n = Dice.nextInt(words.length);
                        currentWord = words[n];
                        currentTranslation = translations[n];
                        mainActivity.wordTxt.setText(currentWord);
                        mainActivity.translationTxt.setText("= "+currentTranslation);

                        //clear backgrounds
                        mainActivity.redBox.setVisibility(View.GONE);
                        mainActivity.greenBox.setVisibility(View.GONE);
                        mainActivity.translationTxt.setVisibility(View.GONE);


                    }
                }
//              mainActivity.timerTxt.setText(remaining)
            }
        }
    }


    private class SkeletonAnalyzerTransactor implements MLAnalyzer.MLTransactor<MLSkeleton> {
        private GraphicOverlay mGraphicOverlay;

        WeakReference<LiveSkeletonAnalyseActivity> mMainActivityWeakReference;

        SkeletonAnalyzerTransactor(LiveSkeletonAnalyseActivity mainActivity, GraphicOverlay ocrGraphicOverlay) {
            mMainActivityWeakReference = new WeakReference<>(mainActivity);
            this.mGraphicOverlay = ocrGraphicOverlay;
        }

        @Override
        public void transactResult(MLAnalyzer.Result<MLSkeleton> result) {
            this.mGraphicOverlay.clear();

            SparseArray<MLSkeleton> sparseArray = result.getAnalyseList();
            List<MLSkeleton> list = new ArrayList<>();
            for (int i = 0; i < sparseArray.size(); i++) {
                list.add(sparseArray.valueAt(i));
                printJoints(sparseArray.valueAt(i).getJoints());
               // Log.d("skelly", "JP"+i+sparseArray.valueAt(i).getJoints().get(0).getType());
            }
            // Remove invalid point.
            List<MLSkeleton> skeletons = SkeletonUtils.getValidSkeletons(list);
//            Log.d("skelly", "skeleton");
//          Log.d("skeleton", Arrays.toString(list.toArray(list)));

            SkeletonGraphic graphic = new SkeletonGraphic(this.mGraphicOverlay, skeletons);
            this.mGraphicOverlay.add(graphic);

            LiveSkeletonAnalyseActivity mainActivity = mMainActivityWeakReference.get();
            if (mainActivity != null && !mainActivity.isFinishing()) {
                mainActivity.compareSimilarity(skeletons);
            }
        }

        @Override
        public void destroy() {
            this.mGraphicOverlay.clear();
        }

    }

    public static float[][] printJoints(List<MLJoint> joints) {
        float[][] arr = new float[14][];
        int i = 0;
        for (MLJoint joint : joints) {
            float x = joint.getPointX();
            float y = joint.getPointY();
            float score = joint.getScore();
            int type = joint.getType();
            float[] jointArr = {x, y, score, type};
            arr[i] = jointArr;
            i++;
        }
        Log.d("jointy", Arrays.deepToString(arr));
        return arr;
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

    private void showWaringDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(R.string.Information_permission)
                .setPositiveButton(R.string.go_authorization, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Guide the user to the setting page for manual authorization.
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Instruct the user to perform manual authorization. The permission request fails.
                        finish();
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //Instruct the user to perform manual authorization. The permission request fails.
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private boolean boundCheck(float x, float top, float bot) {
        return x > top && x < bot;
    }

    private boolean collisionCheck(MLSkeleton skeleton, float top, float bot) {
        boolean collide = false;
        for (MLJoint joint : skeleton.getJoints()) {
            if (!boundCheck(joint.getPointX(), top, bot)) {
                collide = true;
            }
        }
        return collide;
    }

}
