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

package com.huawei.mlkit.example.imgseg;

import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentation;
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentationAnalyzer;
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentationClassification;
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentationScene;
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentationSetting;
import com.huawei.hackzurich.R;

public class ImageSegmentationStillAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = ImageSegmentationStillAnalyseActivity.class.getSimpleName();

    private MLImageSegmentationAnalyzer analyzer;

    private ImageView mImageView;

    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_segmentation_analyse);
        this.findViewById(R.id.segment_detect).setOnClickListener(this);
        this.mImageView = this.findViewById(R.id.image_result);
    }

    @Override
    public void onClick(View v) {
        this.analyzer();
    }

    private void analyzer() {
        /**
         * Configure image segmentation analyzer with custom parameter MLImageSegmentationSetting.
         *
         * setExact(): Set the segmentation fine mode, true is the fine segmentation mode,
         *     and false is the speed priority segmentation mode.
         * setAnalyzerType(): Set the segmentation mode. When segmenting a static image, support setting
         *     MLImageSegmentationSetting.BODY_SEG (only segment human body and background)
         *     and MLImageSegmentationSetting.IMAGE_SEG (segment 10 categories of scenes, including human bodies)
         * setScene(): Set the type of the returned results. This configuration takes effect only in
         *     MLImageSegmentationSetting.BODY_SEG mode. In MLImageSegmentationSetting.IMAGE_SEG mode,
         *     only pixel-level tagging information is returned.
         *     Supports setting MLImageSegmentationScene.ALL (returns all segmentation results,
         *     including: pixel-level tag information, portrait images with transparent backgrounds
         *     and portraits are white, gray background with black background),
         *     MLImageSegmentationScene.MASK_ONLY (returns only pixel-level tag information),
         *     MLImageSegmentationScene .FOREGROUND_ONLY (returns only portrait images with transparent background),
         *     MLImageSegmentationScene.GRAYSCALE_ONLY (returns only grayscale images with white portrait and black background).
         */
        MLImageSegmentationSetting setting = new MLImageSegmentationSetting.Factory()
                .setExact(false)
                .setAnalyzerType(MLImageSegmentationSetting.BODY_SEG)
                .setScene(MLImageSegmentationScene.ALL)
                .create();
        this.analyzer = MLAnalyzerFactory.getInstance().getImageSegmentationAnalyzer(setting);
        // Create an MLFrame by using android.graphics.Bitmap. Recommended image size: large than 224*224.
        this.bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.imgseg_foreground);
        MLFrame mlFrame = new MLFrame.Creator().setBitmap(this.bitmap).create();
        Task<MLImageSegmentation> task = this.analyzer.asyncAnalyseFrame(mlFrame);
        task.addOnSuccessListener(new OnSuccessListener<MLImageSegmentation>() {
            @Override
            public void onSuccess(MLImageSegmentation imageSegmentationResult) {
                // Processing logic for recognition success.
                if (imageSegmentationResult != null) {
                    ImageSegmentationStillAnalyseActivity.this.displaySuccess(imageSegmentationResult);
                } else {
                    ImageSegmentationStillAnalyseActivity.this.displayFailure();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Processing logic for recognition failure.
                ImageSegmentationStillAnalyseActivity.this.displayFailure();
            }
        });
    }

    private void displaySuccess(MLImageSegmentation imageSegmentationResult) {
        if (this.bitmap == null) {
            this.displayFailure();
            return;
        }
        // Draw the portrait with a transparent background.
        Bitmap bitmapFore = imageSegmentationResult.getForeground();
        if (bitmapFore != null) {
            this.mImageView.setImageBitmap(bitmapFore);
        } else {
            this.displayFailure();
        }

        /**
        // Draw a segmentation result map based on the returned pixel-level marker information.
        byte[] result = imageSegmentationResult.getMasks();
        if (result != null) {
            int[] pixels = this.cutOutHuman(result);
            Bitmap processBitmap = Bitmap.createBitmap(pixels, 0, this.bitmap.getWidth(), this.bitmap.getWidth(),
                    this.bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            this.mImageView.setImageBitmap(processBitmap);
        } else {
            this.displayFailure();
        }*/

        /**
        // Draw the gray image of the returned portrait as white and background as black.
        Bitmap bitmapGray = imageSegmentationResult.getGrayscale();
        if (bitmapGray != null) {
            this.mImageView.setImageBitmap(bitmapGray);
        } else {
            this.displayFailure();
        }*/
    }

    private void displayFailure() {
        Toast.makeText(this.getApplicationContext(), "Fail", Toast.LENGTH_SHORT).show();
    }

    private int[] cutOutHuman(byte[] masks) {
        int[] results = new int[this.bitmap.getWidth() * this.bitmap.getHeight()];
        this.bitmap.getPixels(results, 0, this.bitmap.getWidth(), 0, 0, this.bitmap.getWidth(),
                this.bitmap.getHeight());
        for (int i = 0; i < masks.length; i++) {
            if (masks[i] != MLImageSegmentationClassification.TYPE_HUMAN) {
                results[i] = Color.WHITE;
            }
        }
        return results;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.analyzer != null) {
            try {
                this.analyzer.stop();
            } catch (IOException e) {
                Log.e(ImageSegmentationStillAnalyseActivity.TAG, "Stop failed: " + e.getMessage());
            }
        }
    }
}
