package com.ziad.sayit;

import android.content.Context;
import android.graphics.Bitmap;

import android.graphics.Rect;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.ziad.sayit.Interfaces.UpdateGraphicViews;

import java.util.List;


public class TextRecognitionProcessor {
    private UpdateGraphicViews mUpdateGraphicViews;
    Context mContext;
    private Toast mToast;


    public TextRecognitionProcessor(Context context, UpdateGraphicViews updateGraphicViews) {
        mUpdateGraphicViews = updateGraphicViews;
        mContext = context;
        mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
    }


    /////////////////////////////////////////////////////////////////////////////////
    void recognizeTextFromImage(Bitmap scaledBitmapImage){
        mToast.setText("Reading Text..."); mToast.setDuration(Toast.LENGTH_LONG);
        mToast.show();

        InputImage image = InputImage.fromBitmap(scaledBitmapImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(

        );


        // Task failed with an exception
        recognizer.process(image)
                .addOnSuccessListener(
                        this::processTextRecognitionResult)
                .addOnFailureListener(
                        Throwable::printStackTrace);
    }
    ///////////////////////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////////////////////
     private void processTextRecognitionResult(Text texts) {
        mToast.cancel(); //cancel the "Reading Text.." toast

        //if there is no text found, show a Toast message
         List<Text.TextBlock> blocks = texts.getTextBlocks();
         if (blocks.size() == 0) {
             Toast.makeText(mContext, "No text found", Toast.LENGTH_LONG).show();
             mUpdateGraphicViews.onClear();
             return;
         }

         //clear the old boxes when scanning a new image
         mUpdateGraphicViews.onClear();

         //looping throw each word on the image
         for (Text.TextBlock block: blocks) {
             for (Text.Line line: block.getLines()) {
                 for (Text.Element element: line.getElements()) {

                     Rect elementRect = element.getBoundingBox();

                     String wordText = element.getText();

                     mUpdateGraphicViews.onAdd(element, elementRect, wordText);
                 }
             }
         }
     }
    ///////////////////////////////////////////////////////////////////////////////////
}
