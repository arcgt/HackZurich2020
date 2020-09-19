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

package com.huawei.mlkit.example.productvisionsearch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.MLApplication;
import com.huawei.hms.mlsdk.common.MLException;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.productvisionsearch.MLProductVisionSearch;
import com.huawei.hms.mlsdk.productvisionsearch.MLVisionSearchProduct;
import com.huawei.hms.mlsdk.productvisionsearch.MLVisionSearchProductImage;
import com.huawei.hms.mlsdk.productvisionsearch.cloud.MLRemoteProductVisionSearchAnalyzer;
import com.huawei.hms.mlsdk.productvisionsearch.cloud.MLRemoteProductVisionSearchAnalyzerSetting;
import com.huawei.mlkit.example.MainActivity;
import com.huawei.hackzurich.R;

import java.util.ArrayList;
import java.util.List;

public class ProductVisionSearchAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = ProductVisionSearchAnalyseActivity.class.getSimpleName();

    private int CAMERA_PERMISSION_CODE = 1;

    private static final int MAX_RESULTS = 1;

    private TextView mTextView;

    private ImageView productResult;

    private Bitmap bitmap;

    private MLRemoteProductVisionSearchAnalyzer analyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_product_vision_search_analyse);
        this.mTextView = this.findViewById(R.id.result);
        this.productResult = this.findViewById(R.id.image_product);
        this.bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.product_image);
        this.productResult.setImageBitmap(this.bitmap);
        this.findViewById(R.id.product_detect).setOnClickListener(this);
        // Checking Camera Permissions
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

    /**
     * Product search analyzer on the cloud. If you want to use product search analyzer,
     * you need to apply for an agconnect-services.json file in the developer
     * alliance(https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/ml-add-agc),
     * replacing the sample-agconnect-services.json in the project.
     */
    private void remoteAnalyzer() {
        // Use customized parameter settings for cloud-based recognition.
        MLRemoteProductVisionSearchAnalyzerSetting setting =
                new MLRemoteProductVisionSearchAnalyzerSetting.Factory()
                        // Set the maximum number of products that can be returned.
                        .setLargestNumOfReturns(MAX_RESULTS)
                        .setProductSetId("vmall")
                        .setRegion(MLRemoteProductVisionSearchAnalyzerSetting.REGION_DR_CHINA)
                        .create();
        this.analyzer = MLAnalyzerFactory.getInstance().getRemoteProductVisionSearchAnalyzer(setting);
        // Create an MLFrame by using the bitmap.
        MLFrame frame = MLFrame.fromBitmap(bitmap);
        // Set ApiKey.
        MLApplication.getInstance().setApiKey(MainActivity.apiKey);
        Task<List<MLProductVisionSearch>> task = this.analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<List<MLProductVisionSearch>>() {
            @Override
            public void onSuccess(List<MLProductVisionSearch> productVisionSearchList) {
                // Recognition success.
                ProductVisionSearchAnalyseActivity.this.displaySuccess(productVisionSearchList);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Recognition failure.
                ProductVisionSearchAnalyseActivity.this.displayFailure(e);
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

    private void drawBitmap(ImageView imageView, Rect rect, String product) {
        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.WHITE);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4.0f);
        Paint textPaint = new Paint();
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(100.0f);

        imageView.setDrawingCacheEnabled(true);
        Bitmap bitmapDraw = Bitmap.createBitmap(this.bitmap.copy(Bitmap.Config.ARGB_8888, true));
        Canvas canvas = new Canvas(bitmapDraw);
        canvas.drawRect(rect, boxPaint);
        canvas.drawText("product type: " + product, rect.left, rect.top, textPaint);
        this.productResult.setImageBitmap(bitmapDraw);
    }

    private void displaySuccess(List<MLProductVisionSearch> productVisionSearchList) {
        List<MLVisionSearchProductImage> productImageList = new ArrayList<MLVisionSearchProductImage>();
        for (MLProductVisionSearch productVisionSearch : productVisionSearchList) {
            this.drawBitmap(this.productResult, productVisionSearch.getBorder(), productVisionSearch.getType());
            for (MLVisionSearchProduct product : productVisionSearch.getProductList()) {
                productImageList.addAll(product.getImageList());
            }
        }
        StringBuffer buffer = new StringBuffer();
        for (MLVisionSearchProductImage productImage : productImageList) {
            String str = "ProductID: " + productImage.getProductId() + "\nImageID: " + productImage.getImageId() + "\nPossibility: " + productImage.getPossibility();
            buffer.append(str);
            buffer.append("\n");
        }

        this.mTextView.setText(buffer.toString());
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.product_detect:
                this.remoteAnalyzer();
                break;
            default:
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.analyzer == null) {
            return;
        }
        this.analyzer.stop();
    }
}