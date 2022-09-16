package cn.yinxm.media.ms;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DemoActivity extends AppCompatActivity {
    private static final String TAG = "DemoActivity";

    View mViewController;
    private CheckBox btnPlay;
    private TextView textTitle;

    private RecyclerView recyclerView;
    private List<MediaBrowserCompat.MediaItem> list;
    private DemoAdapter demoAdapter;
    private LinearLayoutManager layoutManager;

    private MediaBrowserCompat mBrowser;
    private MediaControllerCompat mController;
    private String mediaId;

    private MediaButtonIntentReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        Log.e(TAG, "onCreate: broadcastReceiverRegistered");
        registerMyReceiver();

        startService(new Intent(this, MusicService.class)); // 避免ui unbind后，后台播放音乐停止
        mBrowser = new MediaBrowserCompat(this, new ComponentName(this, MusicService.class),// 绑定浏览器服务
                BrowserConnectionCallback,// 设置连接回调
                null);

        mViewController = findViewById(R.id.view_controller);
        btnPlay = findViewById(R.id.btn_play);
        textTitle = (TextView) findViewById(R.id.text_title);

        list = new ArrayList<>();
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        demoAdapter = new DemoAdapter(this, list);
        demoAdapter.setOnItemClickListener(new DemoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Log.d(TAG, "ItemClicked");
                Bundle bundle = new Bundle();
                bundle.putInt("playPosition", position);
                mController.getTransportControls().playFromUri(
                        rawToUri(Integer.valueOf(list.get(position).getMediaId())),
                        bundle
                );
            }

            @Override
            public void onItemLongClick(View view, int position) {
                Log.d(TAG, "ItemLongClicked");
            }
        });
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(demoAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Browser发送连接请求
        mBrowser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mController != null) {
            mController.unregisterCallback(mControllerCallback);
        }
        if (mBrowser != null) {
            mBrowser.unsubscribe(mediaId);
            mBrowser.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void clickEvent(View view) {
        switch (view.getId()) {
            case R.id.btn_play:
                if (mController != null) {
                    handlerPlayEvent();
                }
                break;
        }
    }

    /**
     * 处理播放按钮事件
     */
    private void handlerPlayEvent() {
        switch (mController.getPlaybackState().getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                mController.getTransportControls().pause();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                mController.getTransportControls().play();
                break;
            default:
                mController.getTransportControls().playFromSearch("", null);
                break;
        }
    }

    private void updatePlayState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_NONE://无任何状态
                textTitle.setText("");
                btnPlay.setChecked(true);
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                btnPlay.setChecked(true);
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                btnPlay.setChecked(false);
                break;
        }
    }

    private void updatePlayMetadata(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        mViewController.setVisibility(View.VISIBLE);
        textTitle.setText(metadata.getDescription().getTitle() + " - " + metadata.getDescription().getSubtitle());
    }

    /**
     * 连接状态的回调接口，连接成功时会调用onConnected()方法
     */
    private MediaBrowserCompat.ConnectionCallback BrowserConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    Log.e(TAG, "onConnected------");
                    //mediaId即为MediaBrowserService.onGetRoot的返回值
                    //若Service允许客户端连接，则返回结果不为null，其值为数据内容层次结构的根ID
                    //若拒绝连接，则返回null
                    mediaId = mBrowser.getRoot();

                    //Browser通过订阅的方式向Service请求数据，发起订阅请求需要两个参数，其一为mediaId
                    //而如果该mediaId已经被其他Browser实例订阅，则需要在订阅之前取消mediaId的订阅者
                    //虽然订阅一个 已被订阅的mediaId 时会取代原Browser的订阅回调，但却无法触发onChildrenLoaded回调

                    //发起订阅请求之前都需要先取消订阅
                    mBrowser.unsubscribe(mediaId);
                    //之前说到订阅的方法还需要一个参数，即设置订阅回调SubscriptionCallback
                    //当Service获取数据后会将数据发送回来，此时会触发SubscriptionCallback.onChildrenLoaded回调
                    mBrowser.subscribe(mediaId, mBrowserSubscriptionCallback);

                    try {
                        mController = new MediaControllerCompat(DemoActivity.this, mBrowser.getSessionToken());
                        mController.registerCallback(mControllerCallback);
                        if (mController.getMetadata() != null) {
                            updatePlayMetadata(mController.getMetadata());
                            updatePlayState(mController.getPlaybackState());
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConnectionFailed() {
                }

            };
    /**
     * 向媒体浏览器服务(MediaBrowserService)发起数据订阅请求的回调接口
     */
    private final MediaBrowserCompat.SubscriptionCallback mBrowserSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    Log.e(TAG, "onChildrenLoaded------" + children);
                    list.clear();
                    //children 即为Service发送回来的媒体数据集合
                    for (MediaBrowserCompat.MediaItem item : children) {
                        Log.e("ListAdded", item.getDescription().getTitle().toString());
                        list.add(item);
                    }
                    demoAdapter.notifyDataSetChanged();
                }
            };

    /**
     * 媒体控制器控制播放过程中的回调接口，可以用来根据播放状态更新UI
     */
    private final MediaControllerCompat.Callback mControllerCallback =
            new MediaControllerCompat.Callback() {
                /**
                 * 音乐播放状态改变的回调
                 * @param state
                 */
                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    Log.e(TAG, "onCreate: 播放音乐状态改变");
                    updatePlayState(state);
                }

                /**
                 * 播放音乐改变的回调
                 * @param metadata
                 */
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    Log.e(TAG, "onCreate: 播放音乐改变");
                    updatePlayMetadata(metadata);
                }
            };

    private Uri rawToUri(int id) {
        String uriStr = "android.resource://" + getPackageName() + "/" + id;
        return Uri.parse(uriStr);
    }

    private void registerMyReceiver() {
        receiver = new MediaButtonIntentReceiver();
        IntentFilter filter = new IntentFilter();//创建IntentFilter对象
        filter.addAction(Intent.ACTION_SCREEN_OFF);//IntentFilter对象中添加要接收的关屏广播
        filter.addAction(Intent.ACTION_SCREEN_ON);//添加点亮屏幕广播
        registerReceiver(receiver, filter);
    }

}
