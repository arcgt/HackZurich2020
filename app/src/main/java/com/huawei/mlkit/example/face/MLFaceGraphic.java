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

package com.huawei.mlkit.example.face;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.huawei.hms.mlsdk.common.MLPosition;
import com.huawei.hms.mlsdk.face.MLFace;
import com.huawei.hms.mlsdk.face.MLFaceKeyPoint;
import com.huawei.hms.mlsdk.face.MLFaceShape;
import com.huawei.mlkit.example.camera.GraphicOverlay;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MLFaceGraphic extends GraphicOverlay.Graphic {
    private static final float BOX_STROKE_WIDTH = 8.0f;

    private static final float LINE_WIDTH = 5.0f;

    private final GraphicOverlay overlay;

    private final Paint facePositionPaint;

    private final Paint landmarkPaint;

    private final Paint boxPaint;

    private final Paint facePaint;

    private final Paint eyePaint;

    private final Paint eyebrowPaint;

    private final Paint lipPaint;

    private final Paint nosePaint;

    private final Paint noseBasePaint;

    private final Paint textPaint;

    private final Paint probilityPaint;

    private volatile MLFace mFace;

    public MLFaceGraphic(GraphicOverlay overlay, MLFace face) {
        super(overlay);

        this.mFace = face;
        this.overlay = overlay;
        final int selectedColor = Color.WHITE;

        this.facePositionPaint = new Paint();
        this.facePositionPaint.setColor(selectedColor);

        this.textPaint = new Paint();
        this.textPaint.setColor(Color.WHITE);
        this.textPaint.setTextSize(24);
        this.textPaint.setTypeface(Typeface.DEFAULT);

        this.probilityPaint = new Paint();
        this.probilityPaint.setColor(Color.WHITE);
        this.probilityPaint.setTextSize(35);
        this.probilityPaint.setTypeface(Typeface.DEFAULT);

        this.landmarkPaint = new Paint();
        this.landmarkPaint.setColor(Color.RED);
        this.landmarkPaint.setStyle(Paint.Style.FILL);
        this.landmarkPaint.setStrokeWidth(10f);

        this.boxPaint = new Paint();
        this.boxPaint.setColor(Color.WHITE);
        this.boxPaint.setStyle(Paint.Style.STROKE);
        this.boxPaint.setStrokeWidth(MLFaceGraphic.BOX_STROKE_WIDTH);

        this.facePaint = new Paint();
        this.facePaint.setColor(Color.parseColor("#ffcc66"));
        this.facePaint.setStyle(Paint.Style.STROKE);
        this.facePaint.setStrokeWidth(MLFaceGraphic.LINE_WIDTH);

        this.eyePaint = new Paint();
        this.eyePaint.setColor(Color.parseColor("#00ccff"));
        this.eyePaint.setStyle(Paint.Style.STROKE);
        this.eyePaint.setStrokeWidth(MLFaceGraphic.LINE_WIDTH);

        this.eyebrowPaint = new Paint();
        this.eyebrowPaint.setColor(Color.parseColor("#006666"));
        this.eyebrowPaint.setStyle(Paint.Style.STROKE);
        this.eyebrowPaint.setStrokeWidth(MLFaceGraphic.LINE_WIDTH);

        this.nosePaint = new Paint();
        this.nosePaint.setColor(Color.parseColor("#ffff00"));
        this.nosePaint.setStyle(Paint.Style.STROKE);
        this.nosePaint.setStrokeWidth(MLFaceGraphic.LINE_WIDTH);

        this.noseBasePaint = new Paint();
        this.noseBasePaint.setColor(Color.parseColor("#ff6699"));
        this.noseBasePaint.setStyle(Paint.Style.STROKE);
        this.noseBasePaint.setStrokeWidth(MLFaceGraphic.LINE_WIDTH);

        this.lipPaint = new Paint();
        this.lipPaint.setColor(Color.parseColor("#990000"));
        this.lipPaint.setStyle(Paint.Style.STROKE);
        this.lipPaint.setStrokeWidth(MLFaceGraphic.LINE_WIDTH);
    }

    public List<String> sortHashMap(HashMap<String, Float> map) {

        Set<Map.Entry<String, Float>> entey = map.entrySet();
        List<Map.Entry<String, Float>> list = new ArrayList<Map.Entry<String, Float>>(entey);
        Collections.sort(list, new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                if (o2.getValue() - o1.getValue() >= 0) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        List<String> emotions = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            emotions.add(list.get(i).getKey());
        }
        return emotions;
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mFace == null) {
            return;
        }
        float start = 350f;
        float x = start;
        float width = 500f;
        float y = this.overlay.getHeight() - 300.0f;
        HashMap<String, Float> emotions = new HashMap<>();
        emotions.put("Smiling", this.mFace.getEmotions().getSmilingProbability());
        emotions.put("Neutral", this.mFace.getEmotions().getNeutralProbability());
        emotions.put("Angry", this.mFace.getEmotions().getAngryProbability());
        emotions.put("Fear", this.mFace.getEmotions().getFearProbability());
        emotions.put("Sad", this.mFace.getEmotions().getSadProbability());
        emotions.put("Disgust", this.mFace.getEmotions().getDisgustProbability());
        emotions.put("Surprise", this.mFace.getEmotions().getSurpriseProbability());
        List<String> result = this.sortHashMap(emotions);

        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        // Draw the facial feature value.
        canvas.drawText("Left eye: " + decimalFormat.format(this.mFace.getFeatures().getLeftEyeOpenProbability()), x, y,
                this.probilityPaint);
        x = x + width;
        canvas.drawText("Right eye: " + decimalFormat.format(this.mFace.getFeatures().getRightEyeOpenProbability()), x, y,
                this.probilityPaint);
        y = y - 40.0f;
        x = start;
        canvas.drawText("Moustache Probability: " + decimalFormat.format(this.mFace.getFeatures().getMoustacheProbability()),
            x, y, this.probilityPaint);
        x = x + width;
        canvas.drawText("Glass Probability: " + decimalFormat.format(this.mFace.getFeatures().getSunGlassProbability()), x,
            y, this.probilityPaint);
        y = y - 40.0f;
        x = start;
        canvas.drawText("Hat Probability: " + decimalFormat.format(this.mFace.getFeatures().getHatProbability()), x, y,
                this.probilityPaint);
        x = x + width;
        canvas.drawText("Age: " + this.mFace.getFeatures().getAge(), x, y, this.probilityPaint);
        y = y - 40.0f;
        x = start;
        String sex = (this.mFace.getFeatures().getSexProbability() > 0.5f) ? "Female" : "Male";
        canvas.drawText("Gender: " + sex, x, y, this.probilityPaint);
        x = x + width;
        canvas.drawText("RotationAngleY: " + decimalFormat.format(this.mFace.getRotationAngleY()), x, y, this.probilityPaint);
        y = y - 40.0f;
        x = start;
        canvas.drawText("RotationAngleZ: " + decimalFormat.format(this.mFace.getRotationAngleZ()), x, y, this.probilityPaint);
        x = x + width;
        canvas.drawText("RotationAngleX: " + decimalFormat.format(this.mFace.getRotationAngleX()), x, y, this.probilityPaint);
        y = y - 40.0f;
        x = start;
        canvas.drawText(result.get(0), x, y, this.probilityPaint);

        // Draw a face contour.
        if (this.mFace.getFaceShapeList() != null) {
            for (MLFaceShape faceShape : this.mFace.getFaceShapeList()) {
                if (faceShape == null) {
                    continue;
                }
                List<MLPosition> points = faceShape.getPoints();
                for (int i = 0; i < points.size(); i++) {
                    MLPosition point = points.get(i);
                    canvas.drawPoint(this.translateX(point.getX().floatValue()), this.translateY(point.getY().floatValue()),
                            this.boxPaint);
                    if (i != (points.size() - 1)) {
                        MLPosition next = points.get(i + 1);
                        if (point != null && point.getX() != null && point.getY() != null) {
                            if (i % 3 == 0) {
                                canvas.drawText(i + 1 + "", this.translateX(point.getX().floatValue()),
                                        this.translateY(point.getY().floatValue()), this.textPaint);
                            }
                            canvas.drawLines(new float[] {this.translateX(point.getX().floatValue()),
                                    this.translateY(point.getY().floatValue()), this.translateX(next.getX().floatValue()),
                                    this.translateY(next.getY().floatValue())}, this.getPaint(faceShape));
                        }
                    }
                }
            }
        }
        // Face Key Points
        for (MLFaceKeyPoint keyPoint : this.mFace.getFaceKeyPoints()) {
            if (keyPoint != null) {
                MLPosition point = keyPoint.getPoint();
                canvas.drawCircle(this.translateX(point.getX()), this.translateY(point.getY()), 10f, this.landmarkPaint);
            }
        }
    }

    private Paint getPaint(MLFaceShape faceShape) {
        switch (faceShape.getFaceShapeType()) {
            case MLFaceShape.TYPE_LEFT_EYE:
            case MLFaceShape.TYPE_RIGHT_EYE:
                return this.eyePaint;
            case MLFaceShape.TYPE_BOTTOM_OF_LEFT_EYEBROW:

            case MLFaceShape.TYPE_BOTTOM_OF_RIGHT_EYEBROW:
            case MLFaceShape.TYPE_TOP_OF_LEFT_EYEBROW:
            case MLFaceShape.TYPE_TOP_OF_RIGHT_EYEBROW:
                return this.eyebrowPaint;
            case MLFaceShape.TYPE_BOTTOM_OF_LOWER_LIP:
            case MLFaceShape.TYPE_TOP_OF_LOWER_LIP:
            case MLFaceShape.TYPE_BOTTOM_OF_UPPER_LIP:
            case MLFaceShape.TYPE_TOP_OF_UPPER_LIP:
                return this.lipPaint;
            case MLFaceShape.TYPE_BOTTOM_OF_NOSE:
                return this.noseBasePaint;
            case MLFaceShape.TYPE_BRIDGE_OF_NOSE:
                return this.nosePaint;
            default:
                return this.facePaint;
        }
    }
}