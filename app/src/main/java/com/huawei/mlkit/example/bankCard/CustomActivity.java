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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.huawei.hms.mlplugin.card.bcr.CustomView;
import com.huawei.hms.mlplugin.card.bcr.MLBcrCaptureConfig;
import com.huawei.hms.mlplugin.card.bcr.MLBcrCaptureResult;
import com.huawei.hackzurich.R;

import java.lang.reflect.InvocationTargetException;

/**
 * Custom scan interface activity
 */
public class CustomActivity extends Activity {
    private static final String TAG = "CustomActivity";
    private static final double TOP_OFFSET_RATIO = 0.4;
    private FrameLayout linearLayout;
    private CustomView remoteView;
    private ViewfinderView viewfinderView;
    private View light_layout;
    private ImageView img;
    boolean isLight = false;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_defined);
        linearLayout = findViewById(R.id.rim);
        light_layout = findViewById(R.id.light_layout);
        img = findViewById(R.id.imageButton2);

        // Calculate the coordinate information of the custom interface
        Rect mScanRect = createScanRectFromCamera();

        remoteView = new CustomView.Builder()
                .setContext(this)
                // Set the rectangular coordinate setting of the scan frame, required, otherwise it will not be recognized.
                .setBoundingBox(mScanRect)
                // Set the type of result that the bank card identification expects to return.
                // MLBcrCaptureConfig.RESULT_SIMPLE：Only identify the card number and validity period information.
                // MLBcrCaptureConfig.RESULT_ALL：Identify information such as card number, expiration date, issuing bank, issuing organization, and card type.
                .setResultType(MLBcrCaptureConfig.RESULT_SIMPLE)
                // Set result monitoring
                .setOnBcrResultCallback(callback).build();

        // External calls need to be made explicitly, depending on the life cycle of the current container Activity or ViewGroup
        remoteView.onCreate(savedInstanceState);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        linearLayout.addView(remoteView, params);
        // Draw custom interface according to coordinates
        // In this step, you can also draw other such as scan lines, masks, and draw prompts or other buttons according to your needs.
        addMainView(mScanRect);

        // Flash setting click event
        light_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remoteView.switchLight();
                isLight = !isLight;
                if (isLight) {
                    img.setBackgroundResource(R.drawable.rn_eid_ic_hivision_light_act);
                } else {
                    img.setBackgroundResource(R.drawable.rn_eid_ic_hivision_light);
                }
            }
        });

        if (!checkPermissions(this)) {
            Toast.makeText(this, com.huawei.hms.mlplugin.card.bcr.R.string.mlkit_bcr_permission_tip, Toast.LENGTH_LONG).show();
        }
    }

    private CustomView.OnBcrResultCallback callback = new CustomView.OnBcrResultCallback() {
        @Override
        public void onBcrResult(MLBcrCaptureResult idCardResult) {
            Intent intent = new Intent();
            Bitmap bitmap = idCardResult.getOriginalBitmap();
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true);
            // Because the set mode is MLBcrCaptureConfig.RESULT_SIMPLE, only the corresponding data is returned
            intent.putExtra("bitmap", bitmap);
            intent.putExtra("number", idCardResult.getNumber());
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        View decorView = window.getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(option);
        if (Build.VERSION.SDK_INT >= 21) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        remoteView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        remoteView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        remoteView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        remoteView.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        remoteView.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void addMainView(Rect frameRect) {
        this.viewfinderView = new ViewfinderView(this, frameRect);
        this.viewfinderView.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.linearLayout.addView(this.viewfinderView);
    }

    /**
     * Get real screen size information
     *
     * @return Point
     */
    private Point getRealScreenSize() {
        int heightPixels = 0;
        int widthPixels = 0;
        Point point = null;
        WindowManager manager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);

        if (manager != null) {
            Display d = manager.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            d.getMetrics(metrics);
            heightPixels = metrics.heightPixels;
            widthPixels = metrics.widthPixels;
            Log.i(TAG, "heightPixels=" + heightPixels + " widthPixels=" + widthPixels);

            if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
                try {
                    heightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
                    widthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
                    Log.i(TAG, "2 heightPixels=" + heightPixels + " widthPixels=" + widthPixels);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "getRealScreenSize exception");
                } catch (IllegalAccessException e) {
                    Log.w(TAG, "getRealScreenSize exception");
                } catch (InvocationTargetException e) {
                    Log.w(TAG, "getRealScreenSize exception");
                } catch (NoSuchMethodException e) {
                    Log.w(TAG, "getRealScreenSize exception");
                }
            } else if (Build.VERSION.SDK_INT >= 17) {
                Point realSize = new Point();
                try {
                    Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
                    heightPixels = realSize.y;
                    widthPixels = realSize.x;
                    Log.i(TAG, "3 heightPixels=" + heightPixels + " widthPixels=" + widthPixels);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "getRealScreenSize exception");
                } catch (IllegalAccessException e) {
                    Log.w(TAG, "getRealScreenSize exception");
                } catch (InvocationTargetException e) {
                    Log.w(TAG, "getRealScreenSize exception");
                } catch (NoSuchMethodException e) {
                    Log.w(TAG, "getRealScreenSize exception");
                }
            }
        }
        Log.i(TAG, "getRealScreenSize widthPixels=" + widthPixels + " heightPixels=" + heightPixels);
        point = new Point(widthPixels, heightPixels);
        return point;
    }

    private Rect createScanRect(int screenWidth, int screenHeight) {
        final float heightFactor = 0.8f;
        final float CARD_SCALE = 0.63084F;
        int width = Math.round(screenWidth * heightFactor);
        int height = Math.round((float) width * CARD_SCALE);
        int leftOffset = (screenWidth - width) / 2;
        int topOffset = (int) (screenHeight * TOP_OFFSET_RATIO) - height / 2;
        Log.i(TAG, "screenWidth=" + screenWidth + " screenHeight=" + screenHeight + "  rect width=" + width
                + " leftOffset " + leftOffset + " topOffset " + topOffset);
        Rect rect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
        return rect;
    }

    private Rect createScanRectFromCamera() {
        Point point = getRealScreenSize();
        int screenWidth = point.x;
        int screenHeight = point.y;
        Rect rect = createScanRect(screenWidth, screenHeight);
        return rect;
    }

    private boolean checkPermissions(Context context) {
        final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();

        for (String permission : permissions) {
            int check = packageManager.checkPermission(permission, packageName);
            if (check == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }
}
