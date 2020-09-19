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

package com.huawei.mlkit.example.generalCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.huawei.hms.mlsdk.text.MLText;

/**
 * Post-processing plug-in for Exit-Entry Permit for Travelling to and from Hong Kong and Macao.
 *
 */
public class PassCardProcess {
    private static final String TAG = PassCardProcess.class.getSimpleName();

    private final MLText text;

    public PassCardProcess(MLText text) {
        this.text = text;
    }

    // Re-encapsulate the return result of OCR in Block.
    class BlockItem {
        public final String text;
        public final Rect rect;

        public BlockItem(String text, Rect rect) {
            this.text = text;
            this.rect = rect;
        }
    }

    public UniversalCardResult getResult() {
        List<MLText.Block> blocks = this.text.getBlocks();
        if (blocks.isEmpty()) {
            Log.i(PassCardProcess.TAG, "PassCardProcess::getResult blocks is empty");
            return null;
        }
        ArrayList<BlockItem> originItems = this.getOriginItems(blocks);
        String valid = "";
        String number = "";
        boolean validFlag = false;
        boolean numberFlag = false;

        for (BlockItem item : originItems) {
            String tempStr = item.text;
            if (!validFlag) {
                String result = this.tryGetValidDate(tempStr);
                if (!result.isEmpty()) {
                    valid = result;
                    validFlag = true;
                }
            }
            if (!numberFlag) {
                String result = this.tryGetCardNumber(tempStr);
                if (!result.isEmpty()) {
                    number = result;
                    numberFlag = true;
                }
            }
        }
        return new UniversalCardResult(valid, number);
    }

    private String tryGetValidDate(String originStr) {
        return Utils.getCorrectValidDate(originStr);
    }

    private String tryGetCardNumber(String originStr) {
        String result = Utils.getPassCardNumber(originStr);
        if (!result.isEmpty()) {
            result = result.toUpperCase(Locale.ENGLISH);
            result = Utils.filterString(result, "[^0-9A-Z<]");
        }
        return result;
    }

    /**
     * Transform the detected text into BlockItem structure.
     * @param blocks
     * @return
     */
    private ArrayList<BlockItem> getOriginItems(List<MLText.Block> blocks) {
        ArrayList<BlockItem> originItems = new ArrayList<>();

        for (MLText.Block block : blocks) {
            List<MLText.TextLine> lines = block.getContents();
            for (MLText.TextLine line : lines) {
                String text = line.getStringValue();
                text = Utils.filterString(text, "[^a-zA-Z0-9\\.\\-,<\\(\\)\\s]");
                Log.d(PassCardProcess.TAG, "PassCardProcess text: " + text);
                Point[] points = line.getVertexes();
                Rect rect = new Rect(points[0].x, points[0].y, points[2].x, points[2].y);
                BlockItem item = new BlockItem(text, rect);
                originItems.add(item);
            }
        }
        return originItems;
    }
}
