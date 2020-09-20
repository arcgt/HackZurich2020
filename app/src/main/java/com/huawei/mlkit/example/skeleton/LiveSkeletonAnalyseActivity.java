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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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


    private int lensType = LensEngine.BACK_LENS;

    private boolean isFront = false;

    private static List<MLSkeleton> currentSkeletons;

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

    public static float[][] STAR = {{267.93414f, 430.5351f, 101, 0.71240234f}, {194.80865f, 362.83948f, 102, 0.75878906f}, {133.88184f, 321.79745f, 103, 0.6669922f}, {412.56152f, 430.62805f, 104, 0.6621094f}, {476.8604f, 375.45895f, 105, 0.6743164f}, {523.6869f, 326.43134f, 106, 0.8339844f}, {304.07794f, 648.6727f, 107, 0.8046875f}, {267.49435f, 789.0001f, 108, 0.8574219f}, {230.68094f, 908.1598f, 109, 0.77685547f}, {386.41693f, 652.2744f, 110, 0.6879883f}, {449.47095f, 772.23486f, 111, 0.7841797f}, {487.23895f, 942.85864f, 112, 0.74316406f}, {339.87183f, 347.94504f, 113, 0.82910156f}, {339.92133f, 434.43668f, 114, 0.7841797f}};
    public static float [][] STRETCH = {{375.86508f, 398.63742f, 101, 0.6821289f}, {340.63196f, 326.695f, 102, 0.8129883f}, {377.36285f, 253.75702f, 103, 0.83447266f}, {450.59836f, 399.2226f, 104, 0.6953125f}, {486.68945f, 327.2224f, 105, 0.6640625f}, {449.75684f, 254.70863f, 106, 0.7338867f}, {340.04782f, 688.4221f, 107, 0.6982422f}, {193.58167f, 761.15967f, 108, 0.72998047f}, {340.75992f, 834.72485f, 109, 0.8105469f}, {449.2434f, 689.87177f, 110, 0.6816406f}, {414.25717f, 892.3572f, 111, 0.69873047f}, {413.52863f, 1087.6544f, 112, 0.79345703f}, {413.2946f, 290.76166f, 113, 0.8276367f}, {412.85678f, 398.986f, 114, 0.8232422f}};
    public static float [][] TILT = {{267.0674f, 616.7778f, 101, 0.31152344f}, {266.86255f, 725.74677f, 102, 0.32861328f}, {267.0623f, 871.03f, 103, 0.43286133f}, {267.98602f, 616.84106f, 104, 0.54296875f}, {266.6856f, 726.05194f, 105, 0.2861328f}, {266.56757f, 835.08636f, 106, 0.25610352f}, {377.59976f, 688.79f, 107, 0.5683594f}, {340.1589f, 762.7306f, 108, 0.52441406f}, {266.84937f, 907.17236f, 109, 0.29882812f}, {450.69153f, 617.6101f, 110, 0.5839844f}, {558.3581f, 797.3463f, 111, 0.6977539f}, {595.7693f, 907.8295f, 112, 0.7949219f}, {157.47375f, 617.67816f, 113, 0.47705078f}, {230.66676f, 616.56104f, 114, 0.68115234f}};
    public static float [][] LEG = {{375.98724f, 436.19708f, 101, 0.69140625f}, {275.08008f, 471.16504f, 102, 0.62060547f}, {212.21176f, 471.15863f, 103, 0.6279297f}, {448.88525f, 471.62115f, 104, 0.7260742f}, {487.09628f, 435.8158f, 105, 0.67529297f}, {559.29065f, 398.97745f, 106, 0.72802734f}, {267.96436f, 579.9413f, 107, 0.6748047f}, {165.25621f, 544.99615f, 108, 0.53808594f}, {0.0f, 0.0f, 109, 0.0f}, {339.69293f, 617.01965f, 110, 0.78515625f}, {304.60822f, 726.1819f, 111, 0.7495117f}, {304.4248f, 852.19824f, 112, 0.7050781f}, {414.32184f, 398.63525f, 113, 0.75439453f}, {412.8252f, 436.2071f, 114, 0.7792969f}};
    public static ArrayList<List<MLSkeleton>> skeletonTemplates;
    public static float[][][] skeleton_data = {STAR, STRETCH, TILT, LEG};

   // public static float[][][] skeleton_data = {STAR, STRETCH, TILT, LEG, DOWN};


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
        templateImgView.setImageResource(R.drawable.star);
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
        if (skeletonTemplates != null) {
            return;
        }

        skeletonTemplates = new ArrayList<>();
        for (int j = 0; j < skeleton_data.length; j++)
        {
            List<MLJoint> mlJointList = new ArrayList<>();
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.skeleton_template);

            for (int i = 0; i < skeleton_data[j].length; i++) {
                MLJoint mlJoint = new MLJoint(bitmap.getWidth() * skeleton_data[j][i][0],
                        bitmap.getHeight() * skeleton_data[j][i][1], (int)skeleton_data[j][i][2], skeleton_data[j][i][3]);
                mlJointList.add(mlJoint);
            }

            ArrayList<MLSkeleton> tempList = new ArrayList<>();
            tempList.add(new MLSkeleton(mlJointList));
            skeletonTemplates.add(tempList);
        }
        currentSkeletons = skeletonTemplates.get(0);
    }

//    private void initTemplateData() {
//        if (templateList != null) {
//            return;
//        }
//
//        List<MLJoint> mlJointList = new ArrayList<>();
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.skeleton_template);
//        for (int i = 0; i < TMP_SKELETONS.length; i++) {
//            MLJoint mlJoint = new MLJoint(bitmap.getWidth() * TMP_SKELETONS[i][0],
//                    bitmap.getHeight() * TMP_SKELETONS[i][1], (int)TMP_SKELETONS[i][2], TMP_SKELETONS[i][3]);
//            mlJointList.add(mlJoint);
//        }
//
//        templateList = new ArrayList<>();
//        templateList.add(new MLSkeleton(mlJointList));
//        //
//    }
//
//    public void setSkeletonsTemplate(float[][] skeletons)
//    {
//        List<MLJoint> mlJointList = new ArrayList<>();
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.skeleton_template);
//        for (int i = 0; i < skeletons.length; i++) {
//            MLJoint mlJoint = new MLJoint(bitmap.getWidth() * skeletons[i][0],
//                    bitmap.getHeight() * skeletons[i][1], (int)skeletons[i][2], skeletons[i][3]);
//            mlJointList.add(mlJoint);
//        }
//
//        templateList = new ArrayList<>();
//        templateList.add(new MLSkeleton(mlJointList));
//    }

    /**
     * Compute Similarity
     *
     * @param skeletons
     */
    private void compareSimilarity(List<MLSkeleton> skeletons) {
        if (currentSkeletons == null) {
            return;
        }

        float similarity = 0f;
        float result = analyzer.caluteSimilarity(skeletons, currentSkeletons);
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

                        //change current skeleton template
                        int q = Dice.nextInt(5);
                        if (q == 0) {
                            currentSkeletons = skeletonTemplates.get(0);
                            mainActivity.templateImgView.setImageResource(R.drawable.star);
                        }
                        else if (q == 1)
                        {
                            currentSkeletons = skeletonTemplates.get(1);
                            mainActivity.templateImgView.setImageResource(R.drawable.stretch);
                        }
                        else if (q == 2)
                        {
                            currentSkeletons = skeletonTemplates.get(2);
                            mainActivity.templateImgView.setImageResource(R.drawable.tilt);
                        }
                        else if (q == 3)
                        {
                            currentSkeletons = skeletonTemplates.get(3);
                            mainActivity.templateImgView.setImageResource(R.drawable.leg);
                        }
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

    public static String[][] printJoints(List<MLJoint> joints) {
        String[][] arr = new String[14][];
        int i = 0;
        for (MLJoint joint : joints) {
            float x = joint.getPointX();
            float y = joint.getPointY();
            float score = joint.getScore();
            int type = joint.getType();
            String[] jointArr = {String.valueOf(x) + 'f', String.valueOf(y) + 'f', String.valueOf(type), String.valueOf(score) + 'f'};
            arr[i] = jointArr;
            i++;
        }
        Log.d("ttt", Arrays.deepToString(arr).replace('[', '{').replace(']' ,'}'));
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
