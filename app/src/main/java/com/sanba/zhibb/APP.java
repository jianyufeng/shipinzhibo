package com.sanba.zhibb;

import android.app.Application;
import android.util.Log;

import com.tencent.rtmp.TXLivePusher;

/**
 * 作者:Created by 简玉锋 on 2017/6/12 13:39
 * 邮箱: jianyufeng@38.hn
 */

public class APP extends Application {

    private static APP instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        //getSDKVersion接口获取版本号
        int[] sdkVer = TXLivePusher.getSDKVersion(); //这里调用TXLivePlayer.getSDKVersion()也是可以的
        if (sdkVer != null && sdkVer.length >= 4) {
            Log.d("rtmpsdk","rtmp sdk version is:" + sdkVer[0] + "." + sdkVer[1] + "." + sdkVer[2]);
        }
    }
    public static APP getApplication() {
        return instance;
    }
}
