package com.huawei.mlkit.example.imagesuperresolution;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.imagesuperresolution.MLImageSuperResolutionAnalyzer;
import com.huawei.hms.mlsdk.imagesuperresolution.MLImageSuperResolutionAnalyzerFactory;
import com.huawei.hms.mlsdk.imagesuperresolution.MLImageSuperResolutionAnalyzerSetting;
import com.huawei.hms.mlsdk.imagesuperresolution.MLImageSuperResolutionResult;
import com.huawei.hackzurich.R;

import androidx.appcompat.app.AppCompatActivity;

public class ImageSuperResolutionActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SuperResolutionActivity";
    private MLImageSuperResolutionAnalyzer analyzer;
    private static final int INDEX_1X = 0;
    private static final int INDEX_ORIGINAL = 1;
    private ImageView imageView;
    private Bitmap srcBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_super_resolution);
        imageView = findViewById(R.id.image);
        srcBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.superresolution_image);
        findViewById(R.id.button_1x).setOnClickListener(this);
        findViewById(R.id.button_original).setOnClickListener(this);
        createAnalyzer();
        detectImage(INDEX_1X);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_1x) {
            detectImage(INDEX_1X);
        } else if (view.getId() == R.id.button_original) {
            detectImage(INDEX_ORIGINAL);
        }
    }

    private void detectImage(int type) {
        if(type == INDEX_ORIGINAL){
            imageView.setImageBitmap(srcBitmap);
            return;
        }

        if (analyzer == null) {
            return;
        }

        // Create an MLFrame by using the bitmap.
        MLFrame frame = MLFrame.fromBitmap(srcBitmap);
        Task<MLImageSuperResolutionResult> task = analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<MLImageSuperResolutionResult>() {
            public void onSuccess(MLImageSuperResolutionResult result) {
                // Recognition success.
                Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                setImage(result.getBitmap());
            }
        }).addOnFailureListener(new OnFailureListener() {
            public void onFailure(Exception e) {
                // Recognition failure.
                Toast.makeText(getApplicationContext(), "Failed：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setImage(final Bitmap bitmap) {
        ImageSuperResolutionActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(bitmap);
            }
        });
    }

    private void createAnalyzer() {
        // Method 1: use the default setting, that is, 1x image super resulotion.
        // analyzer = MLImageSuperResolutionAnalyzerFactory.getInstance().getImageSuperResolutionAnalyzer();
        // Method 2: using the custom setting, currently only supports 1x image super resulotion, and can be expanded later.
        MLImageSuperResolutionAnalyzerSetting settings = new MLImageSuperResolutionAnalyzerSetting.Factory()
                // Set the scale of image super resolution to 1x.
                .setScale(MLImageSuperResolutionAnalyzerSetting.ISR_SCALE_1X)
                .create();
        analyzer = MLImageSuperResolutionAnalyzerFactory.getInstance().getImageSuperResolutionAnalyzer(settings);
    }

    private void release() {
        if (analyzer == null) {
            return;
        }
        analyzer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (srcBitmap != null) {
            srcBitmap.recycle();
        }
        release();
    }
}