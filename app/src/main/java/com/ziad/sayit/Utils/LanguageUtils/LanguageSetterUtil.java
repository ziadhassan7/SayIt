package com.ziad.sayit.Utils.LanguageUtils;

import java.util.Locale;

public class LanguageSetterUtil {
    public static Locale setTTSLanguage(int language){
        Locale localeLanguage;

        switch(language){
            case 0:
                localeLanguage = LanguagesConstants.DANISH_TTS;
                break;
            case 1:
                localeLanguage = LanguagesConstants.DUTCH_TTS;
                break;
            case 2:
                localeLanguage = LanguagesConstants.ENGLISH_TTS;
                break;
            case 3:
                localeLanguage = LanguagesConstants.FINNISH_TTS;
                break;
            case 4:
                localeLanguage = LanguagesConstants.FRENCH_TTS;
                break;
            case 5:
                localeLanguage = LanguagesConstants.GERMAN_TTS;
                break;
            case 6:
                localeLanguage = LanguagesConstants.ITALIAN_TTS;
                break;
            case 7:
                localeLanguage = LanguagesConstants.POLISH_TTS;
                break;
            case 8:
                localeLanguage = LanguagesConstants.PORTUGUESE_TTS;
                break;
            case 9:
                localeLanguage = LanguagesConstants.SPANISH_TTS;
                break;
            case 10:
                localeLanguage = LanguagesConstants.SWEDISH_TTS;
                break;
            case 11:
                localeLanguage = LanguagesConstants.TURKISH_TTS;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + language);
                //localeLanguage = Languages.ENGLISH_TTS; - default language
        }

        return localeLanguage;
    }
}
