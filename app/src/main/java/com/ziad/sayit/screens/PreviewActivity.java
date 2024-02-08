package com.ziad.sayit.screens;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.mlkit.vision.text.Text;
import com.sergivonavi.materialbanner.Banner;
import com.sergivonavi.materialbanner.BannerInterface;
import com.ziad.sayit.Helper;
import com.ziad.sayit.ImageHandler;
import com.ziad.sayit.screens.LanguagesActivity.LanguagesActivity;
import com.ziad.sayit.R;
import com.ziad.sayit.TextRecognitionProcessor;
import com.ziad.sayit.Utils.GraphicUtils.GraphicOverlay;
import com.ziad.sayit.Utils.GraphicUtils.TextGraphic;
import com.ziad.sayit.Interfaces.UpdateGraphicViews;
import com.ziad.sayit.Utils.LanguageUtils.LanguageManagerUtil;
import com.ziad.sayit.Utils.LanguageUtils.LanguageSetterUtil;
import com.ziad.sayit.WifiConnection;

import java.util.ArrayList;
import java.util.TreeSet;

import static com.ziad.sayit.Helper.LANGUAGE_INDEX_PREFERENCE_KEY;
import static com.ziad.sayit.Helper.LANGUAGE_PREFERENCE_KEY;
import static com.ziad.sayit.Helper.PICK_IMAGE_REQUEST;
import static com.ziad.sayit.ImageHandler.centerHeightValue;
import static com.ziad.sayit.ImageHandler.centerWidthValue;

public class PreviewActivity extends AppCompatActivity implements UpdateGraphicViews,
        TextToSpeech.OnInitListener, GestureDetector.OnGestureListener{

    //
    public static final int REQUEST_CODE_PERMISSIONS = 10;
    public static final String[] REQUIRED_PERMISSIONS = {android.Manifest.permission.CAMERA};

    //
    ConstraintLayout previewLayout;
    GraphicOverlay.Graphic textGraphic;
    GraphicOverlay mGraphicOverlay;
    TextToSpeech textToSpeech;

    LanguageManagerUtil languageManager;
    ImageHandler imageHandler;
    Helper helper;

    String mTouchedText;

    ArrayList<Rect> touchRects = new ArrayList<>();
    ArrayList<String> texts = new ArrayList<>();
    TreeSet<Integer> touchedRectIndexes = new TreeSet<>();

    //initialize textInput view
    TextInputLayout mTextInputCustomEndIcon;
    TextInputEditText mTextInputEditText;

    ImageView mImageView;
    ImageButton mTranslateBtnActivated;
    ImageButton mLanguageBtn;
    Banner mBanner;
    Toast mToast;

    Thread mThread;

    Uri mImageUri, mCroppedImageUri;

    private InterstitialAd mInterstitialAd;

    int touchX, touchY;
    private GestureDetectorCompat mDetector;

    boolean doubleBackToExitPressed = false;
    boolean mIsScrolling;
    boolean mIsTTSInstalled;

    AlertDialog installTTSDialog;

    SharedPreferences mSharedPreferences;

    WifiConnection mWifiConnection;
    IntentFilter mIntentFilter;
    //



    //////////////////////////////////////////////////////////////////////////////////////////////////
    //                                   -- Interface Methods --
    @Override
    public void onAdd(Text.Element element, Rect elementRect, String wordText) {
        textGraphic = new TextGraphic(mGraphicOverlay, element);
        mGraphicOverlay.add(textGraphic);

        Rect rescaledTouchRect = new Rect(elementRect.left - 5  + centerWidthValue,
                elementRect.top - 15 + centerHeightValue,
                elementRect.right + 5 + centerWidthValue,
                elementRect.bottom + 15 + centerHeightValue); //all was fives


        touchRects.add(rescaledTouchRect);
        texts.add(wordText);
    }

    @Override
    public void onClear() {
        mGraphicOverlay.clear();
        touchRects.clear();
        texts.clear();
        mTouchedText = null;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("imageUri", mImageUri);
        outState.putParcelable("croppedImageUri", mCroppedImageUri);
    }



    //                                 ________<<onCreate()>>________

    @SuppressLint("ClickableViewAccessibility")//Silence the Accessibility warning
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Helper.colorStatusBarForApiLowerThanM(this);
        //-

        mSharedPreferences = getApplicationContext().getSharedPreferences(LANGUAGE_PREFERENCE_KEY, Context.MODE_PRIVATE);
        LanguageManagerUtil.chosenLanguageIndex = mSharedPreferences.getInt(LANGUAGE_INDEX_PREFERENCE_KEY, 2); //2 is default value (English)

        // Instantiate the gesture detector with the application context and an implementation of GestureDetector.OnGestureListener
        mDetector = new GestureDetectorCompat(this, this);


        //--


        TextRecognitionProcessor FTR = new TextRecognitionProcessor(this, this);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT); //Initializing Toast

        //initialize the previewLayout
        previewLayout = findViewById(R.id.previewLayout);

        //initialize textInput view
        mTextInputCustomEndIcon = findViewById(R.id.textInputLayout);
        mTextInputEditText = findViewById(R.id.textInputEditText);

        //initialize the buttons
        mLanguageBtn = findViewById(R.id.language_btn);
        ImageButton galleryBtn = findViewById(R.id.galleryBtn);
        ImageButton cropAndRotateBtn = findViewById(R.id.adjust_btn);
        ImageButton mCopyBtn = findViewById(R.id.copy);
        mTranslateBtnActivated = findViewById(R.id.translate);
        //initialize the snapFab
        ExtendedFloatingActionButton snapFab = findViewById(R.id.cameraBtn);

        //initialize the internet connection mBanner
        mBanner = findViewById(R.id.banner);

        //initialize the imageViews
        mImageView = findViewById(R.id.imageView);

        mGraphicOverlay = findViewById(R.id.graphic_overlay);

        //initialize the ImageHelperClass
        helper = new Helper(this);
        imageHandler = new ImageHandler(this, this, FTR, mImageView);
        //--


        //Set button of mBanner
        mBanner.setLeftButtonListener(BannerInterface::dismiss);
        mBanner.setRightButtonListener(banner -> {
            WifiConnection.turnWifiOn(getApplicationContext());
            banner.dismiss();
        });


        //if there's a hardware on the device lunch the camera app on click
        if (hasCamera()) {
            //---
            snapFab.setOnClickListener(view -> {
                // Request camera permissions
                if (allPermissionsGranted()) {
                    Intent intent = new Intent(this, CameraActivity.class);
                    startActivityForResult(intent, Helper.LAUNCH_CAMERA_ACTIVITY);
                } else {
                    ActivityCompat.requestPermissions(
                            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                    );
                }
            });
            //----------

        } else {
            snapFab.setEnabled(false);

            mToast.setText("You don't have a hardware to handle camera events");
            mToast.setDuration(Toast.LENGTH_LONG);
            mToast.show();
        }

        //buttons click listeners
        galleryBtn.setOnClickListener(view -> ImageHandler.ImageRequestFromGalley(this));
        cropAndRotateBtn.setOnClickListener(view -> {
            if (mImageUri != null) {
                Intent intent = new Intent(this, AdjustImageActivity.class);
                intent.setData(mImageUri);
                startActivityForResult(intent, Helper.LAUNCH_ADJUST_IMAGE_ACTIVITY);
            } else {
                mToast.setText("You have not opened a photo");
                mToast.setDuration(Toast.LENGTH_SHORT);
                mToast.show();
            }
        });
        mCopyBtn.setOnClickListener(v -> {
            if (!mTextInputCustomEndIcon.getEditText().getText().toString().equals("")) {
                helper.copyText(mTextInputCustomEndIcon.getEditText().getText().toString(), false);
            } else {
                if (mTouchedText != null) {
                    if (textToSpeech != null) textToSpeech.stop();
                    helper.copyText(mTouchedText, false);
                } else {
                    mToast.setText("No text selected");
                    mToast.setDuration(Toast.LENGTH_SHORT);
                    mToast.show();
                }
            }
        });


        mTranslateBtnActivated.setOnClickListener(view -> {
            if (isGoogleTranslateInstalled()) {
                if (!mTextInputCustomEndIcon.getEditText().getText().toString().equals("")) {
                    helper.translateIntent(mTextInputCustomEndIcon.getEditText().getText().toString());
                } else {
                    if (mTouchedText != null) {
                        if (textToSpeech != null) textToSpeech.stop();
                        helper.translateIntent(mTouchedText);
                    } else {
                        mToast.setText("No text selected");
                        mToast.setDuration(Toast.LENGTH_SHORT);
                        mToast.show();
                    }
                }
            } else {
                openTranslateInstallDialog();
            }
        });


        mTextInputCustomEndIcon
                .setEndIconOnClickListener(view -> {
                    if (mTextInputCustomEndIcon.getEditText() != null) {
                        String text = mTextInputCustomEndIcon.getEditText().getText().toString();

                        if (!text.equals("")) { //if editText isn't empty
                            sayIt(text);
                        } else {
                            sayIt(mTouchedText);
                        }

                        helper.dismissKeyboard(); //hide keyboard on click
                    }
                });

        mLanguageBtn.setOnClickListener(v -> {
            helper.dismissKeyboard(); //hide keyboard on click

            Intent intent = new Intent(getApplicationContext(), LanguagesActivity.class);
            startActivity(intent);

        });
        //-


        //set TouchListener on previewLayout                                                        //--onTouchListener
        previewLayout.setOnTouchListener((v, event) -> {
            helper.dismissKeyboard();

            if (mDetector.onTouchEvent(event)) {
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP) { //when user left his finger up
                if (mIsScrolling) { //if it was a scroll action
                    handleScrollFinished();
                    mIsScrolling = false; //set mIsScrolling back to false
                }
            }//                                                                                 <\>

            return PreviewActivity.super.onTouchEvent(event);
        });
        //--


        String action = getIntent().getAction(); //Action: receive shared data; if user shared image to SayIt


        //--         Get image path from previous activity, or recover the instance state, or receive shared data
        if (savedInstanceState != null) {
            mImageUri = savedInstanceState.getParcelable("imageUri");
            mCroppedImageUri = savedInstanceState.getParcelable("croppedImageUri");
            imageHandler.resetAll();
        } else if (Intent.ACTION_SEND.equals(action)) {
            mImageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            imageHandler.resetAll();
        } else {
            mImageUri = getIntent().getData();
            imageHandler.resetAll();
        }

        //  Load the image
        ViewTreeObserver viewTreeObserver = mImageView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);


                    if (mCroppedImageUri != null) {
                        imageHandler.loadScaledImage(mCroppedImageUri);
                    } else {
                        imageHandler.loadScaledImage(mImageUri);
                    }
                }
            });
        }
        //--

        if (mImageUri == null){
            setWelcomingDesign(); //set the welcoming design onLaunch and set its padding
        }

        mWifiConnection = new WifiConnection(mBanner); //Initializing Broadcast receiver

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
    }//                                                                                              <<<<<<<<<\> onCreate() end bracket <<\>>>>>>>


///                                                                                                 ///
    //----------------------------------  Permissions Methods  -------------------------------------

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                //Open Camera Activity
                Intent intent = new Intent(this, CameraActivity.class);
                startActivityForResult(intent, Helper.LAUNCH_CAMERA_ACTIVITY);
            } else {
                Toast.makeText(this,
                        "Permissions are not granted by user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }



///                                                                                                 ///
    //-------------------------------  Gesture Detector Methods  -----------------------------------

    @Override                                                                                       //--onTouchDown performed (Tap on Screen!)
    public boolean onDown(MotionEvent event) {
        helper.dismissKeyboard();
        unCheckHighlightedWords();

        touchX = Math.round(event.getX());
        touchY = Math.round(event.getY());

        for(int x=0; x< touchRects.size();x++) {
            if (touchRects.get(x).contains(touchX, touchY)) { // a word has been clicked

                touchedRectIndexes.add(x); //add touched rect
                mGraphicOverlay.getGraphicElementAtIndex(x).changeColor(true); //highlight touched rect

                mTouchedText = texts.get(x);

                mTextInputEditText.getText().clear(); //or you can use editText.setText("");
                return true;

            }else{
                mTouchedText = null;
            }
        }

        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        if(mTouchedText != null){
            sayIt(mTouchedText);

        }else{
            textToSpeech.stop();
        }

        return true;
    }                                                                                               //<\>


    @Override
    public void onShowPress(MotionEvent event) {}


    @Override                                                                                       //--onLongPress performed (Hold on Screen!)
    public void onLongPress(MotionEvent event) {
        if(mTouchedText != null) {
            helper.copyText(mTouchedText, true);
        }
    }                                                                                               //<\>




    @Override                                                                                       //--onSwipe performed (Swipe on Screen!)
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {

        if (!mIsScrolling){
            mIsScrolling  = true;
        }

        touchX = Math.round(event2.getX());
        touchY = Math.round(event2.getY());


        for(int x=0; x< touchRects.size();x++) {
            if (touchRects.get(x).contains(touchX, touchY)) {
                touchedRectIndexes.add(x);
                mGraphicOverlay.getGraphicElementAtIndex(x).changeColor(true);
                return true;
            }else{
                mTouchedText = null;
            }
        }

        return true;
    }

    private void handleScrollFinished() {
        ArrayList<String> touchedRectSentence = new ArrayList<>();

        for(int i: touchedRectIndexes){
            touchedRectSentence.add(texts.get(i));
        }

        mTouchedText = TextUtils.join(" ", touchedRectSentence);
        sayIt(mTouchedText);
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        return false; //it has to return false, or I will have to write unnecessary extra code
    }                                                                                               //<\>





    void unCheckHighlightedWords(){
        for(int i: touchedRectIndexes){
            mGraphicOverlay.getGraphicElementAtIndex(i).changeColor(false);
        }
        touchedRectIndexes.clear(); //reset touched rect indexes array
    }
    ///                                                                                                 ///
    //----------------------------------------------------------------------------------------------






    //                                      ________<<onStart()>>________
    @Override
    protected void onStart() {
        super.onStart();

        //Check if Google tts is installed
        checkIfGoogleTTSIsInstalled(); //Check if Google tts is installed
        //-


        if(mIsTTSInstalled) {
            textToSpeech = new TextToSpeech(this, this); setupUtteranceProgressListener();
            languageManager = new LanguageManagerUtil(this, textToSpeech);
        }

        if(mIsTTSInstalled){
            if(LanguageManagerUtil.isLanguageInstalled()) {
                mBanner.dismiss();
            }
        }


        /// Initialize Ads
        MobileAds.initialize(this, initializationStatus -> {});
        AdRequest adRequest = new AdRequest.Builder().build();

        //load Ad
        //Test Ad: ca-app-pub-3940256099942544/1033173712
        InterstitialAd.load(this,"ca-app-pub-6295244604789756/3373473928", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        mInterstitialAd = null;
                    }
                });


    }

    //                                      ________<<onResume()>>________
    @Override
    protected void onResume() {
        registerReceiver(mWifiConnection, mIntentFilter); // Register Broadcast receiver

        super.onResume();
    }

    //                                      ________<<onPause()>>________
    @Override
    public void onPause(){
        if(textToSpeech !=null){
            textToSpeech.stop();
        }
        unregisterReceiver(mWifiConnection); // Unregister Broadcast receiver

        super.onPause();
    }

    //                                      ________<<onDestroy()>>________
    @Override
    public void onDestroy(){
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        super.onDestroy();
    }

    //                                     ______<<onBackPressed()>>_______
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressed) {
            super.onBackPressed();

        }else {
            this.doubleBackToExitPressed = true;
            mToast.setText("Click Again To Exit"); mToast.setDuration(Toast.LENGTH_SHORT);
            mToast.show();

            new Handler().postDelayed(() -> doubleBackToExitPressed = false, 2000);
        }
    }
    //                                                  <>>





    //Initialize TextToSpeech  -------------------------------------------------------------------
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(LanguageSetterUtil.setTTSLanguage(LanguageManagerUtil.chosenLanguageIndex));
            Helper.firstOpen = true;

        } else {
            mToast.setText("TTS Initialization failed"); mToast.setDuration(Toast.LENGTH_LONG);
            mToast.show();
        }
    }

    //----------------------------------------  Say A Word  ----------------------------------------
    private void sayIt(final String detectedText){
        mThread = new Thread(() -> {
            if(textToSpeech != null && detectedText != null) {
                textToSpeech.stop(); //stop current sound
                if (helper.isVoiceAudible()) {
                    textToSpeech.speak(detectedText, TextToSpeech.QUEUE_FLUSH, null, "SAYIT_UtteranceID");
                    if (Helper.firstOpen & LanguageManagerUtil.isLanguageInstalled()) {
                        this.runOnUiThread(() -> {
                            mToast.setText("Saying " + detectedText + "...");
                            mToast.setDuration(Toast.LENGTH_SHORT);
                            mToast.show();
                        });

                        Helper.firstOpen = false;
                    }

                    if (!LanguageManagerUtil.isLanguageInstalled()) {
                        this.runOnUiThread(() -> mBanner.dismiss());

                    }

                    if (!Helper.isNetworkAvailable(this)
                            && !LanguageManagerUtil.isLanguageInstalled()) {

                        this.runOnUiThread(() -> mBanner.show());
                    }
                    //--
                } else { //if speaker is muted
                    this.runOnUiThread(() -> {
                        mToast.setText("Volume is muted");
                        mToast.setDuration(Toast.LENGTH_LONG);
                        mToast.show();
                    });
                }
            }
        });
        mThread.start();
    }

    void setupUtteranceProgressListener(){
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // Speaking started.
                if(!LanguageManagerUtil.isLanguageInstalled()){
                    mToast.setText("Saying " + mTouchedText + "..."); mToast.setDuration(Toast.LENGTH_LONG);
                    mToast.show();

                }
            }

            @Override
            public void onDone(String utteranceId) {
                // Speaking stopped.
                mToast.cancel();
            }

            @Override
            public void onError(String utteranceId) {
                if(Helper.isNetworkAvailable(getApplicationContext())){
                    mToast.setText("Please wait while downloading language"); mToast.setDuration(Toast.LENGTH_LONG);
                    mToast.show();

                }
            }
        });
    }
    //----------------------------------------------------------------------------------------------






    //------------------------------ Get the results back from intents  ------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_IMAGE_REQUEST:
                if (resultCode == RESULT_OK && null != data) {
                    //reset everything
                    imageHandler.resetAll();
                    touchedRectIndexes.clear();

                    mImageUri = data.getData();
                    imageHandler.loadScaledImage(mImageUri);

                    showAnAd();

                } else {
                    mToast.setText("You haven't picked an image"); mToast.setDuration(Toast.LENGTH_SHORT);
                    mToast.show();
                }
                break;

            case Helper.LAUNCH_ADJUST_IMAGE_ACTIVITY:
                if (resultCode == RESULT_OK && null != data) {
                    //reset everything
                    imageHandler.resetAll();
                    touchedRectIndexes.clear();

                    mCroppedImageUri = data.getData();
                    imageHandler.loadScaledImage(mCroppedImageUri);

                    //showAnAd();
                }
                break;

            case Helper.LAUNCH_CAMERA_ACTIVITY:
                if (resultCode == RESULT_OK && null != data) {
                    //reset everything
                    imageHandler.resetAll();
                    touchedRectIndexes.clear();

                    mImageUri = Uri.fromFile(ImageHandler.createImageFile(this));
                    imageHandler.loadScaledImage(mImageUri);

                    //showAnAd();
                }
                break;
        }
    }

    //------------------------------------------------------------------------------------------------------

    //                                  ----------Welcoming Design----------
    void setWelcomingDesign(){
        mImageView.setImageResource(R.drawable.welcoming_logo);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        if(width > 650 && width < 900){
            mImageView.setPadding((width - 650)/2, 0, (width - 650)/2, 0);
        } else if(width > 900){
            mImageView.setPadding((width - 900)/2, 0, (width - 900)/2, 0);
        }
    }

    //                                ----------Check For Google TTS----------
    void checkIfGoogleTTSIsInstalled(){
        try {
            ApplicationInfo ai = this.getPackageManager().getApplicationInfo("com.google.android.tts",0);

            mIsTTSInstalled = ai.enabled;

            if(!mIsTTSInstalled){  // Not Installed
                openTTSInstallDialog();
            } else if (installTTSDialog != null){
                installTTSDialog.dismiss();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

            mIsTTSInstalled = Helper.isPackageInstalled("com.google.android.tts", this.getPackageManager());
            if(!mIsTTSInstalled){  // Not Installed
                openTTSInstallDialog();
            } else if (installTTSDialog != null){
                installTTSDialog.dismiss();
            }
        }
    }

    void openTTSInstallDialog(){
        installTTSDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Install Google Text-To-Speech")
                .setMessage("You need to install Google Text-To-Speech in order to use SayIt!")
                .setPositiveButton("Done", (dialog, which) -> {
                    checkIfGoogleTTSIsInstalled();
                    dialog.dismiss();
                })
                .setNegativeButton("Install", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + "com.google.android.tts")));
                    } catch (ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + "com.google.android.tts")));
                    }
                })
                .setCancelable(false)
                .show();
    }
    //----------------------------------------------------------------------------------------------

    //---------------------------------------Helper Methods-----------------------------------------
    //does the phone have a hardware to handle camera events
    public boolean hasCamera() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    //Show an AD!
    void showAnAd(){
        if (mInterstitialAd!= null) {
            mInterstitialAd.show(PreviewActivity.this);
        }
    }


    boolean isGoogleTranslateInstalled(){
        try {
            ApplicationInfo ai = this.getPackageManager().getApplicationInfo("com.google.android.apps.translate",0);

            boolean isEnabled = ai.enabled;

            if(!isEnabled){  // Not Enabled
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

            boolean isInstalled = Helper.isPackageInstalled("com.google.android.apps.translate", this.getPackageManager());
            if(!isInstalled){  // Not Installed
                return false;
            }
        }

        return true;
    }

    void openTranslateInstallDialog(){
        installTTSDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Install Google Translate")
                .setMessage("You need to install Google Translate to use this feature.")
                .setPositiveButton("Install", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + "com.google.android.apps.translate")));
                    } catch (ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + "com.google.android.apps.translate")));
                    }
                })
                .show();
    }

    //-------------------------------------------------------------------------------------------------
}