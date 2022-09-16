package cn.yinxm.media.ms;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();//获取到收到的广播的名称
        Log.e(TAG,"接收到广播的Action是"+action);
    }
}
