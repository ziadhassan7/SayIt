package com.ziad.sayit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;

import com.sergivonavi.materialbanner.Banner;

public class WifiConnection extends BroadcastReceiver {
    Banner mBanner;

    public WifiConnection(Banner banner){
        mBanner = banner;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                new Handler().postDelayed(() -> {
                    if(Helper.isNetworkAvailable(context)){
                        mBanner.dismiss();
                    }

                }, 4000);
            }
        }
    }

    public static void turnWifiOn(Context context){
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager != null){
            wifiManager.setWifiEnabled(true);
        }
    }
}
