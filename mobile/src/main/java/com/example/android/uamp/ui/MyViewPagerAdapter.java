package com.example.android.uamp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.uamp.AlbumArtCache;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.List;

//taked here https://medium.com/@cdmunoz/the-easiest-way-to-work-with-viewpager-and-without-fragments-c62ec3e8b9f3
public class MyViewPagerAdapter extends PagerAdapter {

    private Context context;
    /**
     * Contains the list of objects that represent the data of this ArrayAdapter.
     * The content of this list is referred to as "the array" in the documentation.
     */
    private List<MediaBrowserCompat.MediaItem> mObjects;

    /**
     * Indicates whether or not {@link #notifyDataSetChanged()} must be called whenever
     * {@link #mObjects} is modified.
     */
    private boolean mNotifyOnChange = true;

    /**
     * Lock used to modify the content of {@link #mObjects}. Any write operation
     * performed on the array should be synchronized on this lock.
     */
    private final Object mLock = new Object();

    private static ColorStateList sColorStatePlaying;
    private static ColorStateList sColorStateNotPlaying;


    public MyViewPagerAdapter(Context context) {
        this.context = context;
        mObjects = new ArrayList<>();
    }

    @Override
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {
        if (sColorStateNotPlaying == null || sColorStatePlaying == null) {
            initializeColorStateLists(context);
        }
        Integer cachedState = STATE_INVALID;

        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup convertView = (ViewGroup) inflater.inflate(R.layout.music_fragment, collection, false);

        ImageView mBackgroundImage = convertView.findViewById(R.id.image);
        TextView textViewName = convertView.findViewById(R.id.name);
        TextView textViewGenre = convertView.findViewById(R.id.genre);

        MediaBrowserCompat.MediaItem mediaItem = mObjects.get(position);
        MediaDescriptionCompat description = mediaItem.getDescription();
        fetchImageAsync(description, mBackgroundImage);

        CharSequence name = description.getTitle();
        CharSequence category = description.getSubtitle();
        textViewName.setText(name);
        textViewGenre.setText(category);

        // If the state of convertView is different, we need to adapt the view to the
        // new state.
        int state = getMediaItemState((Activity) context, mediaItem); //todo if boxing works wromg - take activity
        if (cachedState == null || cachedState != state) {
            //next code is an example - don't remove
//            Drawable drawable = getDrawableByState(container.getContext(), state);
//            if (drawable != null) {
//                //ignore result here, this code is just a placeholder
//                //set visibility to View.VISIBLE and use drawable
//            } else {
//                //set visibility to View.GONE and use drawable
//            }
            convertView.setTag(R.id.tag_mediaitem_state_cache, state);
        }

        collection.addView(convertView);
        return convertView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object view) {
        container.removeView((View) view);
    }

    @Override
    public int getCount() {
        return this.mObjects.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    public void add(MediaBrowserCompat.MediaItem item) {
        synchronized (mLock) {
            mObjects.add(item);
        }
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    /**
     * Remove all elements from the list.
     */
    public void clear() {
        synchronized (mLock) {
            mObjects.clear();
        }
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    public MediaBrowserCompat.MediaItem getItem(int position) {
        return mObjects.get(position);
    }

    public static final int STATE_INVALID = -1;
    public static final int STATE_NONE = 0;
    public static final int STATE_PLAYABLE = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_PLAYING = 3;

    private static void initializeColorStateLists(Context ctx) {
        sColorStateNotPlaying = ColorStateList.valueOf(ctx.getResources().getColor(
                R.color.media_item_icon_not_playing));
        sColorStatePlaying = ColorStateList.valueOf(ctx.getResources().getColor(
                R.color.media_item_icon_playing));
    }

    public static Drawable getDrawableByState(Context context, int state) {
        if (sColorStateNotPlaying == null || sColorStatePlaying == null) {
            initializeColorStateLists(context);
        }

        switch (state) {
            case STATE_PLAYABLE:
                Drawable pauseDrawable = ContextCompat.getDrawable(context,
                        R.drawable.ic_play_arrow_black_36dp);
                DrawableCompat.setTintList(pauseDrawable, sColorStateNotPlaying);
                return pauseDrawable;
            case STATE_PLAYING:
                AnimationDrawable animation = (AnimationDrawable)
                        ContextCompat.getDrawable(context, R.drawable.ic_equalizer_white_36dp);
                DrawableCompat.setTintList(animation, sColorStatePlaying);
                animation.start();
                return animation;
            case STATE_PAUSED:
                Drawable playDrawable = ContextCompat.getDrawable(context,
                        R.drawable.ic_equalizer1_white_36dp);
                DrawableCompat.setTintList(playDrawable, sColorStatePlaying);
                return playDrawable;
            default:
                return null;
        }
    }

    public static int getMediaItemState(Activity context, MediaBrowserCompat.MediaItem mediaItem) {
        int state = STATE_NONE;
        // Set state to playable first, then override to playing or paused state if needed
        if (mediaItem.isPlayable()) {
            state = STATE_PLAYABLE;
            if (MediaIDHelper.isMediaItemPlaying(context, mediaItem)) {
                state = getStateFromController(context);
            }
        }

        return state;
    }

    public static int getStateFromController(Activity context) {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(context);
        PlaybackStateCompat pbState = controller.getPlaybackState();
        if (pbState == null ||
                pbState.getState() == PlaybackStateCompat.STATE_ERROR) {
            return MediaItemViewHolder.STATE_NONE;
        } else if (pbState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            return MediaItemViewHolder.STATE_PLAYING;
        } else {
            return MediaItemViewHolder.STATE_PAUSED;
        }
    }

    private void fetchImageAsync(
            @NonNull MediaDescriptionCompat description, ImageView mBackgroundImage) {
        if (description.getIconUri() == null) {
            return;
        }
        String artUrl = description.getIconUri().toString();
        AlbumArtCache cache = AlbumArtCache.getInstance();
        Bitmap art = cache.getBigImage(artUrl);
        if (art == null) {
            art = description.getIconBitmap();
        }
        if (art != null) {
            // if we have the art cached or from the MediaDescription, use it:
            mBackgroundImage.setImageBitmap(art);
        } else {
            // otherwise, fetch a high res version and update:
            cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                    mBackgroundImage.setImageBitmap(bitmap);
                }
            });
        }
    }
}