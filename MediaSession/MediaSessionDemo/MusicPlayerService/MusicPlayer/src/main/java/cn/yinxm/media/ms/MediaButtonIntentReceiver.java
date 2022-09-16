package cn.yinxm.media.ms;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

//广播接收器
public class MediaButtonIntentReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaButtonIntentReceiver";

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        Log.d(TAG, "MediaButton onReceive action:" + action);

        if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {

            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            int keyCode = keyEvent.getKeyCode();
            int keyAction = keyEvent.getAction();
            long eventTime = keyEvent.getEventTime();

            Log.i(TAG, "keyCode:" + keyCode + ",keyAction:" + keyAction + ",eventTime:" + eventTime);

            ComponentName componentName = new ComponentName(context, MusicService.class);
            intent.setComponent(componentName);
            context.startService(intent);
        } else {
            Log.e(TAG, "onReceive: NotThisAction");
        }
    }
}
