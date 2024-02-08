package com.ziad.sayit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.widget.ImageView;

import com.ziad.sayit.Interfaces.UpdateGraphicViews;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static com.ziad.sayit.Helper.PICK_IMAGE_REQUEST;

public class ImageHandler extends Activity{

    int countRotate = 0;

    private Context mContext;
    private TextRecognitionProcessor mFTR;

    private ImageView mImageView;

    public Bitmap bitmapImage;

    public static int centerHeightValue;
    public static int centerWidthValue;
    private UpdateGraphicViews mUpdateGraphicViews;
    //~ Variables End



    public ImageHandler(Context context,
                        UpdateGraphicViews updateGraphicViewsInterface,
                        TextRecognitionProcessor FTR,
                        ImageView imageView){

        mContext = context;
        mFTR = FTR;
        mImageView = imageView;
        mUpdateGraphicViews = updateGraphicViewsInterface;
    }



    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                  -- Image Helper Methods --
    //get the scaled bitmap image with the new width and height showed on the screen
    private Bitmap getScaledBitmap (Bitmap bitmapImage){

        //width and height of original image
        final int imageWidth = bitmapImage.getWidth();
        final int imageHeight = bitmapImage.getHeight();

        //width and height of the imageView
        final int imageViewWidth  = mImageView.getWidth();
        final int imageViewHeight = mImageView.getHeight();

        final int scaledWidth , scaledHeight;


        if (imageWidth*imageViewHeight <= imageHeight*imageViewWidth) {
            //don't use: imageViewWidth/imageWidth>=imageViewHeight/imageHeight; because values like (1.8)  ==  (1.3), because they are stored in integers.

            //rescaled width and height of image within ImageView
            scaledWidth = (imageWidth*imageViewHeight)/imageHeight;
            scaledHeight = imageViewHeight;
        }
        else {
            //rescaled width and height of image within ImageView
            scaledWidth = imageViewWidth;
            scaledHeight = (imageHeight*imageViewWidth)/imageWidth;
        }


        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmapImage, scaledWidth, scaledHeight, true);

        centerGraphicLayout(scaledBitmap);
        return scaledBitmap;
    }


    private void centerGraphicLayout(Bitmap bitmapImage){
        //width and height of original image
        final int imageWidth = bitmapImage.getWidth();
        final int imageHeight = bitmapImage.getHeight();

        //width and height of the imageView
        final int imageViewWidth  = mImageView.getWidth();
        final int imageViewHeight = mImageView.getHeight();




        if (imageWidth*imageViewHeight <= imageHeight*imageViewWidth){               //The image is centered vertically
            centerWidthValue = (imageViewWidth - imageWidth)/2;
        }else {                                                                      //else: image is centered horizontally
            centerHeightValue = (imageViewHeight - imageHeight)/2;
        }
    }


    public void loadScaledImage(Uri photoUri){
        if(photoUri != null){
            mUpdateGraphicViews.onClear();
            mImageView.setPadding(0,0,0,0); //reset the padding

            bitmapImage = decodeSampledBitmapFromUri(photoUri);

            //default value is 1 not 0
            float contrast = 1.25f; //from 1 to 2 (suitable from 1.2 to 1.4)
            int brightness = -40; //from 255 to 255

            ColorMatrix cm = new ColorMatrix(new float[]
                    {
                            contrast, 0, 0, 0, brightness,
                            0, contrast, 0, 0, brightness,
                            0, 0, contrast, 0, brightness,
                            0, 0, 0, 1, 0
                    });

            Bitmap ret = Bitmap.createBitmap(bitmapImage.getWidth(), bitmapImage.getHeight(), bitmapImage.getConfig());

            Canvas canvas = new Canvas(ret);

            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(cm));
            canvas.drawBitmap(bitmapImage, 0, 0, paint);

            mImageView.setImageBitmap(ret);
            mFTR.recognizeTextFromImage(getScaledBitmap(ret)); //put enhanced image here
        }

    }


    private Bitmap decodeSampledBitmapFromUri(Uri photoUri){
        InputStream input = null;
        try {
            input = mContext.getContentResolver().openInputStream(photoUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        try {
            input = mContext.getContentResolver().openInputStream(photoUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap bitmap =  BitmapFactory.decodeStream(input, null, options);
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*Note!
         *Closing and reopening the InputStream is actually necessary
         *because the first BitmapFactory.decodeStream(...) call sets the reading position of the stream to the end,
         *so the second call of the method wouldn't work anymore without reopening the stream!*/
        return bitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        final int reqHeight = mImageView.getHeight();
        final int reqWidth = mImageView.getWidth();

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////




    //reset width and height of rect boxes over text
    public void resetAll(){
        centerWidthValue = 0;
        centerHeightValue = 0;
        mUpdateGraphicViews.onClear();
    }

    public static File createImageFile(Context context){
        String fileName = context.getExternalCacheDir().getAbsolutePath();
        fileName += "/SayIt_tempImage.jpg";

        return new File(fileName);
    }

    //get an image using the gallery app
    public static void ImageRequestFromGalley(Context context) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        ((Activity) context).startActivityForResult(photoPickerIntent, PICK_IMAGE_REQUEST);
    }

}
