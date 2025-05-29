// SPDX-License-Identifier: GPL-3.0-or-later OR Apache-2.0

package com.cardovip.dko;

import android.app.Application;
import android.content.Context;
import android.os.Build;




public class App extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        log.init(true, "DKO");
//        log.d("app is run");
        // 初始化 Toast 框架
        Toaster.init(this);


        PRNGFixes.apply();

        // 初始化LitePal
        LitePal.initialize(this);
        

        
        // 初始化网络客户端
        RetrofitClient.init();
    }
    public static Context getContext() {
        return context;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("L");
        }
    }
}
