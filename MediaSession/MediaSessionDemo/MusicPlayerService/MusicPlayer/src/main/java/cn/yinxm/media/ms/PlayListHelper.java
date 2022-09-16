package cn.yinxm.media.ms;

import android.content.Context;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;

import java.util.ArrayList;
import java.util.List;

public class PlayListHelper {

    public static Uri rawToUri(Context context, int id) {
        String uriStr = "android.resource://" + context.getPackageName() + "/" + id;
        return Uri.parse(uriStr);
    }

    public static List<PlayBean> getPlayList() {
        List<PlayBean> list = new ArrayList<>();

        PlayBean playBean0 = new PlayBean();
        playBean0.mediaId = R.raw.music0;
        playBean0.tilte = "music0";
        playBean0.artist = "A";
        list.add(playBean0);

        PlayBean playBean1 = new PlayBean();
        playBean1.mediaId = R.raw.music1;
        playBean1.tilte = "music1";
        playBean1.artist = "B";
        list.add(playBean1);

        PlayBean playBean2 = new PlayBean();
        playBean2.mediaId = R.raw.music2;
        playBean2.tilte = "music2";
        playBean2.artist = "C";
        list.add(playBean2);

        PlayBean playBean3 = new PlayBean();
        playBean3.mediaId = R.raw.music3;
        playBean3.tilte = "music3";
        playBean3.artist = "D";
        list.add(playBean3);
        return list;
    }

    public static ArrayList<MediaBrowserCompat.MediaItem> transformPlayList(List<PlayBean> playBeanList) {
        //模拟静态数据
        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(R.raw.music0))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "music0")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "A")
                .build();
        MediaMetadataCompat metadata2 = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(R.raw.music1))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "music1")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "B")
                .build();
        MediaMetadataCompat metadata3 = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(R.raw.music2))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "music2")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "C")
                .build();
        MediaMetadataCompat metadata4 = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(R.raw.music3))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "music3")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "D")
                .build();
        ArrayList<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        mediaItems.add(createMediaItem(metadata));
        mediaItems.add(createMediaItem(metadata2));
        mediaItems.add(createMediaItem(metadata3));
        mediaItems.add(createMediaItem(metadata4));
        return mediaItems;
    }

    public static MediaMetadataCompat transformPlayBean(PlayBean bean) {
        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(bean.mediaId))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, bean.tilte)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, bean.artist)
                .build();

        return metadata;
    }

    private static MediaBrowserCompat.MediaItem createMediaItem(MediaMetadataCompat metadata) {
        return new MediaBrowserCompat.MediaItem(
                metadata.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        );
    }
}
