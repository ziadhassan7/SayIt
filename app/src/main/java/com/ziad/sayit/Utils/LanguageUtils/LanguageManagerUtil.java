package com.ziad.sayit.Utils.LanguageUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.widget.Toast;

import com.ziad.sayit.Helper;

import java.util.Set;

public class LanguageManagerUtil {

    Context mContext;
    private static TextToSpeech mTTS;
    private static Toast mToast;

    public static int chosenLanguageIndex = 2; //default is English

    @SuppressLint("ShowToast")
    public LanguageManagerUtil(Context context, TextToSpeech textToSpeech){

        mContext = context;
        mTTS = textToSpeech;
        mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
    }

    public LanguageManagerUtil(Context context){
        mContext = context;
    }



    public void setLanguage(int language) {

        setAndCheckLanguageAvailability(language);
    }


    //                             -- Set & Check if language is downloaded --
    public void setAndCheckLanguageAvailability(int chosenLanguageIndex) {
        switch (mTTS.isLanguageAvailable(LanguageSetterUtil.setTTSLanguage(chosenLanguageIndex))) {
            case TextToSpeech.LANG_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:

                mTTS.setLanguage(LanguageSetterUtil.setTTSLanguage(chosenLanguageIndex));

                if(isLanguageInstalled()){
                    mToast.setText("Language is set to " + LanguagesConstants.LANGUAGES_LIST[chosenLanguageIndex]);
                } else {
                    if(Helper.isNetworkAvailable(mContext)){
                        mToast.setText("Downloading Language...");

                    }else{
                        mToast.setText("Not Downloaded.. Open Wifi");
                    }
                }
                mToast.setDuration(Toast.LENGTH_SHORT);
                mToast.show();

                break;

            case TextToSpeech.LANG_MISSING_DATA:
                mToast.setText("Language Missing Data"); mToast.setDuration(Toast.LENGTH_SHORT);
                mToast.show();
            case TextToSpeech.LANG_NOT_SUPPORTED:
                mToast.setText("Language is not supported"); mToast.setDuration(Toast.LENGTH_SHORT);
                mToast.show();

            default:
                break;
        }
    }



    public static boolean isLanguageInstalled(){
        Voice voice = mTTS.getVoice();
        if(voice != null){
            Set<String> features = voice.getFeatures();

            //Simplified if statement that returns true if the condition is met
            return features != null && !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
        }

        return false;
    }
}