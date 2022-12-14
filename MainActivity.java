package cn.yinxm.media.ms.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaMetadata;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Client_MediaBrowser";

    Context mContext;
    PackageManager mPackageManager;
    TextView mTvInfo;
    //媒体浏览器
    MediaBrowserCompat mMediaBrowser;
    //媒体控制器
    MediaControllerCompat mController;
    PlayInfo mPlayInfo = new PlayInfo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        mPackageManager = getPackageManager();

        mTvInfo = findViewById(R.id.tv_info);

        connectRemoteService();
    }


    private void connectRemoteService() {
        // 1.待连接的服务
        ComponentName componentName = new ComponentName("cn.yinxm.media.ms", "cn.yinxm.media.ms.MusicService");
        // 2.创建MediaBrowser
        //新建MediaBrowser,第一个参数是context
        //第二个参数是CompoentName,有多种构造方法,指向要连接的服务
        //第三个参数是连接结果的回调connectionCallback，第四个参数为Bundle
        mMediaBrowser = new MediaBrowserCompat(mContext, componentName, mConnectionCallbacks, null);
        // 3.建立连接
        mMediaBrowser.connect();
    }

    private void refreshPlayInfo() {
        mTvInfo.setText(mPlayInfo.debugInfo());
    }

    private void updatePlayState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }
        mPlayInfo.setState(state);
        refreshPlayInfo();
    }

    private void updatePlayMetadata(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        mPlayInfo.setMetadata(metadata);
        refreshPlayInfo();
    }

    //服务对客户端的信息回调
    private final MediaBrowserCompat.ConnectionCallback mConnectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                //如果服务端接受连接，就会调此方法表示连接成功，否则回调onConnectionFailed();
                @Override
                public void onConnected() {
                    Log.d(TAG, "MediaBrowser.onConnected");
                    String mediaId = mMediaBrowser.getRoot();
                    mMediaBrowser.unsubscribe(mediaId);
                    //之前说到订阅的方法还需要一个参数，即设置订阅回调SubscriptionCallback
                    //当Service获取数据后会将数据发送回来，此时会触发SubscriptionCallback.onChildrenLoaded回调
                    mMediaBrowser.subscribe(mediaId, BrowserSubscriptionCallback);
                    MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();
                    //通过token，获取MediaController,第一个参数是context，第二个参数为token
                    try {
                        mController = new MediaControllerCompat(MainActivity.this, token);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    //mController注册回调，callback就是媒体信息改变后，服务给客户端的回调
                    mController.registerCallback(mMediaControllerCallback);
                    if (mController.getMetadata() != null) {
                        updatePlayMetadata(mController.getMetadata());
                        updatePlayState(mController.getPlaybackState());
                    }
                }

                @Override
                public void onConnectionSuspended() {
                    super.onConnectionSuspended();
                    // 连接中断回调
                    Log.d(TAG, "onConnectionSuspended");
                }

                @Override
                public void onConnectionFailed() {
                    super.onConnectionFailed();
                    //连接失败回调
                    Log.d(TAG, "onConnectionFailed");
                }
            };

    /**
     * 向媒体浏览器服务(MediaBrowserService)发起数据订阅请求的回调接口
     */
    private final MediaBrowserCompat.SubscriptionCallback BrowserSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    Log.e(TAG, "onChildrenLoaded------" + children);
                    mPlayInfo.setChildren(children);
                    refreshPlayInfo();
                }
            };

    //服务对客户端的信息回调
    MediaControllerCompat.Callback mMediaControllerCallback =

            new MediaControllerCompat.Callback() {

                @Override
                public void onSessionDestroyed() {
                    // Session销毁
                    Log.d(TAG, "onSessionDestroyed");
                }

                @Override
                public void onRepeatModeChanged(int repeatMode) {
                    // 循环模式发生变化
                    Log.d(TAG, "onRepeatModeChanged");
                }

                @Override
                public void onShuffleModeChanged(int shuffleMode) {
                    // 随机模式发生变化
                    Log.d(TAG, "onShuffleModeChanged");
                }

                @Override
                public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
                    // 当前蓝牙播放列表更新回调
                    super.onQueueChanged(queue);
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    //服务端运行mediaSession.setMetadata(mediaMetadata)就会到达此处，以下类推.
                    super.onMetadataChanged(metadata);
//                    //歌曲信息回调，更新。MediaMetadata在文章后面会提及
//                    MediaDescriptionCompat description = metadata.getDescription();
//                    //获取标题
//                    String title = description.getTitle().toString();
//                    //获取作者
//                    String author = description.getSubtitle().toString();
//                    //获取专辑名
//                    String album = description.getDescription().toString();
//                    //获取总时长
//                    long allTime = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);

                    // 数据变化
                    Log.e(TAG, "onMetadataChanged ");
                    updatePlayMetadata(metadata);
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    // 播放状态变化
                    super.onPlaybackStateChanged(state);
                    Log.d(TAG, "onPlaybackStateChanged   PlaybackState:" + state.getState());
                    if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                        Log.d(TAG, "正在播放" + state);
                    }
                    //获取当前播放进度
//                    long position = state.getPosition();
                    updatePlayState(state);
                }

                @Override
                public void onExtrasChanged(Bundle extras) {
                    super.onExtrasChanged(extras);
                    //额外信息回调，可以承载播放模式等信息
                }

            };

    static class PlayInfo {
        private MediaMetadataCompat metadata;
        private PlaybackStateCompat state;
        private List<MediaBrowserCompat.MediaItem> children;

        public void setMetadata(MediaMetadataCompat metadata) {
            this.metadata = metadata;
        }

        public void setState(PlaybackStateCompat state) {
            this.state = state;
        }

        public void setChildren(List<MediaBrowserCompat.MediaItem> children) {
            this.children = children;
        }

        public String debugInfo() {
            StringBuilder builder = new StringBuilder();

            if (state != null) {
                builder.append("当前播放状态：\t" + (state.getState() == PlaybackStateCompat.STATE_PLAYING ? "播放中" : "未播放"));
                builder.append("\n\n");
            }
            if (metadata != null) {
                builder.append("当前播放信息：\t" + transform(metadata));
                builder.append("\n\n");
            }
            if (children != null && !children.isEmpty()) {
                builder.append("当前播放列表：\n");
                for (int i = 0; i < children.size(); i++) {
                    MediaBrowserCompat.MediaItem mediaItem = children.get(i);
                    builder.append((i + 1) + " " + mediaItem.getDescription().getTitle() + " - " + mediaItem.getDescription().getSubtitle()).append("\n");
                }
            }
            return builder.toString();
        }

        public static String transform(MediaMetadataCompat data) {
            if (data == null) {
                return null;
            } else {
                String title = data.getString(MediaMetadata.METADATA_KEY_TITLE);
                String artist = data.getString(MediaMetadata.METADATA_KEY_ARTIST);
                return title + " - " + artist;
            }
        }
    }
}
