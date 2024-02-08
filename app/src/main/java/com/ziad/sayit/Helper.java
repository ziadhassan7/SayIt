package com.ziad.sayit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.ziad.sayit.screens.PreviewActivity;


public class Helper {
    public static final int PICK_IMAGE_REQUEST = 301;                              ///*
    public static final int LAUNCH_ADJUST_IMAGE_ACTIVITY = 500;                    //
    public static final int LAUNCH_CAMERA_ACTIVITY = 600;                          //Some Constants
    public static final String LANGUAGE_PREFERENCE_KEY = "language-preference";    //
    public static final String LANGUAGE_INDEX_PREFERENCE_KEY = "language-Index";   //*/

    public static boolean firstOpen = true;

    private Context mContext;
    private Toast mToast;
    //~ Variables End

    @SuppressLint("ShowToast")
    public Helper(Context context){

        mContext = context;
        mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
    }




    //Method for hiding Keyboard
    public void dismissKeyboard() {
        TextInputLayout editTextLayout = ((PreviewActivity)mContext).findViewById(R.id.textInputLayout);

            // Check if no view has focus:
            InputMethodManager inputManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(editTextLayout.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public boolean isVoiceAudible(){
        AudioManager audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int musicVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

        return musicVolume > 1;
    }

    public void copyText(String touchedText, boolean doVibration){
        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("mTouchedText", touchedText);
        clipboard.setPrimaryClip(clip);

        mToast.setText("\"" + touchedText + "\" Copied to Clipboard");
        mToast.setDuration(Toast.LENGTH_SHORT); mToast.show();


        // Vibrate for 10 milliseconds
        if(doVibration) {
            Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(40);
            }
        }
    }

    public void translateIntent(String touchedText){
        mToast.setText("translating " + touchedText); mToast.setDuration(Toast.LENGTH_SHORT);
        mToast.show();

        Intent intent = new Intent();
        intent.setType("text/plain");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.setAction(Intent.ACTION_PROCESS_TEXT);
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, touchedText);
            intent.putExtra("key_language_from", "en");
            intent.putExtra("key_language_to", "ar");

        }else{
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, touchedText);

        }

        //This code is essential for api less than android.M
        for (ResolveInfo resolveInfo : mContext.getPackageManager().queryIntentActivities(intent, 0)) {

            if( resolveInfo.activityInfo.packageName.contains("com.google.android.apps.translate")){
                intent.setComponent(new ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name));

                mContext.startActivity(intent);
            }

        }
    }

    public static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public static void colorStatusBarForApiLowerThanM(Activity activity){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.colorStatus_oldApi));
        }
    }

}
