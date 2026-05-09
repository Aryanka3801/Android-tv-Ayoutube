package com.example.myapp.presenter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;

import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.myapp.model.VideoItem;

/**
 * Leanback Presenter that renders {@link VideoItem} objects as TV card tiles.
 * Uses Glide to async-load the YouTube thumbnail.
 */
public class VideoCardPresenter extends Presenter {

    private static final int CARD_WIDTH  = 320;
    private static final int CARD_HEIGHT = 180;

    private static final int[] PLACEHOLDER_COLORS = {
        0xFF1A1A2E, 0xFF16213E, 0xFF0F3460,
        0xFF533483, 0xFF2B2D42, 0xFF1B262C
    };

    private int colorIdx = 0;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageCardView card = new ImageCardView(parent.getContext());
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        // Red focus ring matching YouTube brand
        card.setInfoAreaBackgroundColor(0xFF1C1C1C);
        return new ViewHolder(card);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        VideoItem video = (VideoItem) item;
        ImageCardView card = (ImageCardView) viewHolder.view;
        Context ctx = card.getContext();

        card.setTitleText(video.getTitle());

        // Sub-title: channel • duration
        String sub = video.getChannelName();
        if (video.getDuration() != null && !video.getDuration().isEmpty()) {
            sub += "  •  " + video.getDuration();
        }
        card.setContentText(sub);
        card.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);

        // Coloured placeholder while thumbnail loads
        int placeholder = PLACEHOLDER_COLORS[colorIdx % PLACEHOLDER_COLORS.length];
        colorIdx++;

        String thumb = video.getThumbnailUrl();
        if (thumb != null && !thumb.isEmpty()) {
            Glide.with(ctx)
                 .load(thumb)
                 .placeholder(new ColorDrawable(placeholder))
                 .error(new ColorDrawable(placeholder))
                 .transition(DrawableTransitionOptions.withCrossFade(200))
                 .centerCrop()
                 .into(card.getMainImageView());
        } else {
            card.setMainImage(new ColorDrawable(placeholder));
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ImageCardView card = (ImageCardView) viewHolder.view;
        card.setBadgeImage(null);
        card.setMainImage(null);
    }
}
