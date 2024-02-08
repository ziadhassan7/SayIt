// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.ziad.sayit.Utils.GraphicUtils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;


import com.google.mlkit.vision.text.Text;
import com.ziad.sayit.Utils.GraphicUtils.GraphicOverlay.Graphic;

import static com.ziad.sayit.ImageHandler.centerHeightValue;
import static com.ziad.sayit.ImageHandler.centerWidthValue;


/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
public class TextGraphic extends Graphic {

    private static final int COLOR_DEFAULT = Color.GRAY;
    private static final int COLOR_HIGHLIGHTED = Color.rgb(0,59,111);
    private static final float STROKE_WIDTH = 4f;
    private static final int RECT_OPACITY = 95;
    private static final int RECT_OPACITY_HIGHLIGHTED = 120;

    private final Paint rectPaint;
    private final Text.Element element;



    public TextGraphic(GraphicOverlay overlay, Text.Element element) {
        super(overlay);

        this.element = element;

        rectPaint = new Paint();
        rectPaint.setColor(COLOR_DEFAULT);
        rectPaint.setAlpha(RECT_OPACITY);//set the opacity of the rect box
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }


    public void changeColor(boolean isHighlighted) {
        //set opacity of rect
        if(isHighlighted) {
            rectPaint.setColor(COLOR_HIGHLIGHTED);
            rectPaint.setAlpha(RECT_OPACITY_HIGHLIGHTED); //set opacity of rect
        }else{
            rectPaint.setColor(COLOR_DEFAULT);
            rectPaint.setAlpha(RECT_OPACITY); //set opacity of rect
        }

        postInvalidate();
    }


    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        if (element == null) {
            throw new IllegalStateException("Attempting to draw a null text.");
        }


        //---    Draws the bounding box around the TextBlock. -----------------------------------------------------------------------

        Rect rect = new Rect(element.getBoundingBox()); //get the original values of the BoundingBox Rect

        //rescaling the box to be smaller or 'narrower' , and make things on the center
        /*
         *if the image is tall, we add (half of the imageView width) minus (half of the image width)   > to center things out
         *if the image is wide, we add (half of the imageView height) minus (half of the image height) > to center things out
         */
        Rect rescaledRect = new Rect(rect.left + 1 + centerWidthValue,
                rect.top + centerHeightValue,
                rect.right - 2 + centerWidthValue,
                rect.bottom + centerHeightValue);


        //draw the boxes
        canvas.drawRect(rescaledRect, rectPaint);
        //----------------------------------------------------------------------------------------------------------------------

    }
}
