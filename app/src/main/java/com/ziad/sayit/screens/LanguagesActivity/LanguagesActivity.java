package com.ziad.sayit.screens.LanguagesActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;

import com.ziad.sayit.Helper;
import com.ziad.sayit.Interfaces.LanguageListItemListener;
import com.ziad.sayit.R;
import com.ziad.sayit.Utils.LanguageUtils.LanguageManagerUtil;

import static com.ziad.sayit.Helper.LANGUAGE_PREFERENCE_KEY;

public class LanguagesActivity extends AppCompatActivity implements LanguageListItemListener {
    LanguageManagerUtil mLanguageManagerUtil;
    SharedPreferences mSharedPreferences;

    RecyclerView mRecyclerView;
    RecyclerView.Adapter mLanguagesAdapter;

    WifiConnectionReceiver mWifiConnection;
    IntentFilter mIntentFilter;


    @Override
    public void onLanguageItemSelected(int clickedItemPosition) {

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt("language-Index", clickedItemPosition);
        editor.apply(); //Use apply instead of commit to write in background

        LanguageManagerUtil.chosenLanguageIndex = mSharedPreferences.getInt("language-Index", 2); //2 is default value (English)
        mLanguageManagerUtil.setLanguage(LanguageManagerUtil.chosenLanguageIndex);

        finish();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_languages);
        Helper.colorStatusBarForApiLowerThanM(this);

        mLanguageManagerUtil = new LanguageManagerUtil(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Languages"); //string is custom name you want

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(v -> {
            //What to do on back clicked
            finish();
        });

        mRecyclerView = findViewById(R.id.recyclerview);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        mLanguagesAdapter = new LanguagesAdapter(this, this);
        mRecyclerView.setAdapter(mLanguagesAdapter);

        Button ttsSettings = findViewById(R.id.tts_settings);
        ttsSettings.setOnClickListener(view -> {
            // launch Google TTS voice settings
            Intent intent = new Intent();
            intent.setAction("com.android.settings.TTS_SETTINGS");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        });

        mWifiConnection = new WifiConnectionReceiver(); //Initializing Broadcast receiver
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);

        mSharedPreferences = getApplicationContext().getSharedPreferences(LANGUAGE_PREFERENCE_KEY, Context.MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLanguagesAdapter.notifyDataSetChanged();

        registerReceiver(mWifiConnection, mIntentFilter); // Register Broadcast receiver
    }

    @Override
    public void onPause(){
        super.onPause();

        unregisterReceiver(mWifiConnection); // Unregister Broadcast receiver
    }



    class WifiConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    new Handler().postDelayed(() -> {
                        if(Helper.isNetworkAvailable(context)){
                            // Update RecyclerView
                            mLanguagesAdapter.notifyDataSetChanged();
                        }

                    }, 4000);
                } else {
                    mLanguagesAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
