package com.ziad.sayit.Interfaces;


import android.graphics.Rect;
import com.google.mlkit.vision.text.Text;


public interface UpdateGraphicViews {
    void onAdd(Text.Element element, Rect elementRect, String wordText);
    void onClear();
}
