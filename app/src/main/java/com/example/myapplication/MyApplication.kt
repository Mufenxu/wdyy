package com.example.myapplication

import android.app.Application
import android.util.Log
import com.alibaba.sdk.android.push.CommonCallback
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            PushServiceFactory.init(this)
            val pushService = PushServiceFactory.getCloudPushService()
            pushService.register(this, object : CommonCallback {
                override fun onSuccess(response: String?) {
                    Log.i("AliyunPush", "Push register success: $response")
                }

                override fun onFailed(errorCode: String?, errorMessage: String?) {
                    Log.e("AliyunPush", "Push register failed: $errorCode, $errorMessage")
                }
            })
        } catch (t: Throwable) {
            Log.e("AliyunPush", "Init error", t)
        }
    }
}


