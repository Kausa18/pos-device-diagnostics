package com.feitian.diagnostics;

import android.app.Application;
import android.util.Log;

import com.ftpos.library.smartpos.servicemanager.OnServiceConnectCallback;
import com.ftpos.library.smartpos.servicemanager.ServiceManager;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // OnServiceConnectCallback is a top-level interface in the same package as ServiceManager
        ServiceManager.bindPosServer(this, new OnServiceConnectCallback() {
            @Override
            public void onSuccess() {
                Log.d("FEITIAN", "POS API Service CONNECTED");
            }

            @Override
            public void onFail(int error) {
                Log.e("FEITIAN", "POS API Service BINDING FAILED: " + error);
            }
        });
    }
}
