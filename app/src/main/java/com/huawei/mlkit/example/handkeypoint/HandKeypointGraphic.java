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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.huawei.hms.mlsdk.handkeypoint.MLHandKeypoint;
import com.huawei.hms.mlsdk.handkeypoint.MLHandKeypoints;
import com.huawei.mlkit.example.camera.GraphicOverlay;

import java.util.List;

/**
 * Graphic instance for rendering hand position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class HandKeypointGraphic extends GraphicOverlay.Graphic {

    private static final float BOX_STROKE_WIDTH = 5.0f;

    private final Paint rectPaint;

    private final Paint idPaintnew;

    private List<MLHandKeypoints> handKeypoints;

    public HandKeypointGraphic(GraphicOverlay overlay, List<MLHandKeypoints> handKeypoints) {
        super(overlay);
        this.handKeypoints = handKeypoints;

        final int selectedColor = Color.WHITE;

        idPaintnew = new Paint();
        idPaintnew.setColor(Color.GREEN);
        idPaintnew.setTextSize(32);

        rectPaint = new Paint();
        rectPaint.setColor(selectedColor);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    @Override
    public void draw(Canvas canvas) {
        for(int i=0;i<handKeypoints.size();i++){
            MLHandKeypoints mHandKeypoints = handKeypoints.get(i);
            if(mHandKeypoints.getHandKeypoints()==null){
                continue;
            }

            Rect rect = translateRect(handKeypoints.get(i).getRect());
            canvas.drawRect(rect, rectPaint);
            for (MLHandKeypoint handKeypoint : mHandKeypoints.getHandKeypoints()) {
                if (!(Math.abs(handKeypoint.getPointX() - 0f) == 0 && Math.abs(handKeypoint.getPointY() - 0f) == 0)) {
                    canvas.drawCircle(translateX(handKeypoint.getPointX()),
                            translateY(handKeypoint.getPointY()), 24f, idPaintnew);
                }
            }
        }
    }

    public Rect translateRect(Rect rect) {
        float left = translateX(rect.left);
        float right = translateX(rect.right);
        float bottom = translateY(rect.bottom);
        float top = translateY(rect.top);
        return new Rect((int) left, (int) top, (int) right, (int) bottom);
    }
}
