package cn.yinxm.media.ms;

import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MusicService extends MediaBrowserServiceCompat {
    private AudioManager mAudioManager;
    private ComponentName mMediaButtonReceive;
    boolean isHaveAudioFocus = false;
    //媒体会话，受控端
    private MediaSessionCompat mMediaSession;
    private MediaPlayer mMediaPlayer;
    private PlaybackStateCompat mPlaybackState;

    private static final String TAG = "MusicService";
    public static final String MEDIA_ID_ROOT = "__ROOT__";

    private int position = -1;
    private List<PlayBean> mPlayBeanList = PlayListHelper.getPlayList();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate-----------");

        mPlaybackState = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .setActions(getAvailableActions(PlaybackStateCompat.STATE_NONE))
                .build();
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mMediaButtonReceive = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        mMediaSession = new MediaSessionCompat(this, "MediaService");

        // 设置token后会触发MediaBrowserCompat.ConnectionCallback的回调方法，表示MediaBrowser与MediaBrowserService连接成功
        setSessionToken(mMediaSession.getSessionToken());
        //设置callback，就是客户端对服务指令到达处
        mMediaSession.setCallback(mSessionCallback);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setPlaybackState(mPlaybackState);
        mMediaSession.setActive(true);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(PreparedListener);
        mMediaPlayer.setOnCompletionListener(CompletionListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "action=" + action);
            MediaButtonReceiver.handleIntent(mMediaSession, intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mMediaSession != null) {
            mMediaSession.release();
            mMediaSession = null;
        }
    }

    //MediaBrowserService必须重写的方法，第一个参数为客户端的packageName，第二个参数为Uid
    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        //第三个参数是从客户端传递过来的Bundle。
        //通过以上参数来进行判断，若同意连接，则返回BrowserRoot对象，否则返回null;

        //构造BrowserRoot的第一个参数为rootId(自定义)，第二个参数为Bundle;
        Log.e(TAG, "onGetRoot-----------");
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.e(TAG, "onLoadChildren--------");
        // 将信息从当前线程中移除，允许后续调用sendResult方法
        result.detach();

        // 获取模拟数据，真实情况应该是异步从网络或本地读取数据
        ArrayList<MediaBrowserCompat.MediaItem> mediaItems = PlayListHelper.transformPlayList(mPlayBeanList);

        // 向Browser发送 播放列表数据
        result.sendResult(mediaItems);
    }

    private MediaBrowserCompat.MediaItem createMediaItem(MediaMetadataCompat metadata) {
        return new MediaBrowserCompat.MediaItem(
                metadata.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        );
    }

    private int requestAudioFocus() {
        int result = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        isHaveAudioFocus = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == result;
        if (isHaveAudioFocus) {
            mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceive);
        }
        Log.d(TAG, "requestAudioFocus: " + isHaveAudioFocus);
        return result;
    }

    private void abandonAudioFocus() {
        int result = mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        isHaveAudioFocus = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == result;
    }

    public long getAvailableActions(@PlaybackStateCompat.State int state) {
        long actions = PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_REWIND
                | PlaybackStateCompat.ACTION_FAST_FORWARD;
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        return actions;
    }

    private void handlePlay() {
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PAUSED
                && requestAudioFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mMediaPlayer.start();
            mPlaybackState = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .setActions(getAvailableActions(PlaybackStateCompat.STATE_PLAYING))
                    .build();
            mMediaSession.setPlaybackState(mPlaybackState);
        }
    }

    private void handlePause(boolean isAbandonFocus) {
        if (mMediaPlayer == null) {
            return;
        }
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            mMediaPlayer.pause();
            mPlaybackState = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                    .setActions(getAvailableActions(PlaybackStateCompat.STATE_PAUSED))
                    .build();
            mMediaSession.setPlaybackState(mPlaybackState);
        }
        if (isAbandonFocus) {
            Log.d(TAG, "AbandonFocus");
        }
    }

    private void handlePlayPosition(int pos) {
        PlayBean playBean = setPlayPosition(pos);
        if (playBean == null) {
            return;
        }
        handlePlayUri(PlayListHelper.rawToUri(this, playBean.mediaId));
    }

    private void handlePlayUri(Uri uri) {
        if (uri == null) {
            return;
        }
        if (requestAudioFocus() != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }
        mMediaPlayer.reset();
        mMediaPlayer.setLooping(true);
        try {
            mMediaPlayer.setDataSource(MusicService.this, uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.prepareAsync();
        mPlaybackState = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_CONNECTING, 0, 1.0f)
                .setActions(getAvailableActions(PlaybackStateCompat.STATE_CONNECTING))
                .build();
        mMediaSession.setPlaybackState(mPlaybackState);
        //我们可以保存当前播放音乐的信息，以便客户端刷新UI
        mMediaSession.setMetadata(PlayListHelper.transformPlayBean(getPlayBean()));
    }

    private PlayBean getPlayBean() {
        if (position >= 0 && position < mPlayBeanList.size()) {
            return mPlayBeanList.get(position);
        }
        return null;
    }

    private PlayBean setPlayPosition(int pos) {
        if (pos >= 0 && pos < mPlayBeanList.size()) {
            position = pos;
            return mPlayBeanList.get(position);
        }
        return null;
    }

    /**
     * 响应控制器指令的回调
     */
    private MediaSessionCompat.Callback mSessionCallback = new MediaSessionCompat.Callback() {
        /**
         * 响应MediaController.getTransportControls().play
         */
        @Override
        public void onPlay() {
            super.onPlay();
            Log.e(TAG, "onPlay");
            handlePlay();
        }

        /**
         * 响应MediaController.getTransportControls().onPause
         */
        @Override
        public void onPause() {
            super.onPause();
            Log.e(TAG, "onPause");
            handlePause(true);
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            Log.e(TAG, "onSkipToPrevious: 跳转到上一首");
            int pos = (position + mPlayBeanList.size() - 1) % mPlayBeanList.size();
            handlePlayPosition(pos);
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            Log.d(TAG, "onSkipToNext: 跳转到下一首");
            int pos = (position + 1) % mPlayBeanList.size();
            handlePlayPosition(pos);
        }

        /**
         * 响应MediaController.getTransportControls().playFromUri
         *
         * @param uri
         * @param extras
         */
        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);
            Log.e(TAG, "onPlayFromUri");
            int position = extras.getInt("playPosition");
            setPlayPosition(position);
            handlePlayUri(uri);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Log.d(TAG, "MediaSessionCallback——>onMediaButtonEvent " + mediaButtonEvent);
            return super.onMediaButtonEvent(mediaButtonEvent);
        }
    };

    /**
     * 监听MediaPlayer.prepare()
     */
    private MediaPlayer.OnPreparedListener PreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            mMediaPlayer.start();
            mPlaybackState = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .setActions(getAvailableActions(PlaybackStateCompat.STATE_PLAYING))
                    .build();
            mMediaSession.setPlaybackState(mPlaybackState);
        }
    };

    /**
     * 监听播放结束的事件
     */
    private MediaPlayer.OnCompletionListener CompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mPlaybackState = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                    .setActions(getAvailableActions(PlaybackStateCompat.STATE_NONE))
                    .build();
            mMediaSession.setPlaybackState(mPlaybackState);
            mMediaPlayer.reset();
        }
    };

    AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = focusChange -> {
        Log.d(TAG, "onAudioFocusChange  focusChange=" + focusChange + ", before isHaveAudioFocus=" + isHaveAudioFocus);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.d(TAG, "AUDIOFOCUS_LOSS");
                isHaveAudioFocus = false;
                mSessionCallback.onPause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                isHaveAudioFocus = false;
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                handlePause(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d(TAG, "AUDIOFOCUS_GAIN");
                isHaveAudioFocus = true;
                mSessionCallback.onPlay();
                break;
            case AudioManager.ADJUST_MUTE:
                Log.d(TAG, "ADJUST_MUTE");
                break;
            case AudioManager.ADJUST_UNMUTE:
                Log.d(TAG, "ADJUST_UNMUTE");
                break;
            default:
                break;
        }
    };
}