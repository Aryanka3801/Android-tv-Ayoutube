package com.example.myapp;

import android.os.Bundle;
import android.graphics.Color;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainFragment extends BrowseSupportFragment {

    private static final String[] CATEGORIES = {
        "Trending Now",
        "Music",
        "Gaming",
        "News",
        "Sports",
        "Comedy",
        "Education"
    };

    private static final String[][] CARD_TITLES = {
        {"Top Video 1", "Top Video 2", "Top Video 3", "Top Video 4", "Top Video 5"},
        {"Music Hit 1", "Music Hit 2", "Music Hit 3", "Music Hit 4", "Music Hit 5"},
        {"Game Play 1", "Game Play 2", "Game Play 3", "Game Play 4", "Game Play 5"},
        {"News Story 1", "News Story 2", "News Story 3", "News Story 4", "News Story 5"},
        {"Match 1",     "Match 2",     "Match 3",     "Match 4",     "Match 5"},
        {"Funny 1",     "Funny 2",     "Funny 3",     "Funny 4",     "Funny 5"},
        {"Lesson 1",    "Lesson 2",    "Lesson 3",    "Lesson 4",    "Lesson 5"}
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setTitle("AYouTube TV");
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(Color.parseColor("#E53935"));
        setSearchAffordanceColor(Color.parseColor("#FF6D00"));

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        for (int i = 0; i < CATEGORIES.length; i++) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
            for (int j = 0; j < CARD_TITLES[i].length; j++) {
                listRowAdapter.add(CARD_TITLES[i][j]);
            }
            HeaderItem header = new HeaderItem(i, CATEGORIES[i]);
            rowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        setAdapter(rowsAdapter);
    }

    // -------------------------------------------------------
    // Simple card presenter — shows a coloured card + label
    // -------------------------------------------------------
    private static class CardPresenter extends Presenter {

        private static final int[] CARD_COLORS = {
            0xFFB71C1C, 0xFF880E4F, 0xFF4A148C,
            0xFF1A237E, 0xFF006064, 0xFF1B5E20, 0xFFE65100
        };

        private int mColorIndex = 0;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            ImageCardView cardView = new ImageCardView(parent.getContext());
            cardView.setFocusable(true);
            cardView.setFocusableInTouchMode(true);
            cardView.setMainImageDimensions(320, 180);
            return new ViewHolder(cardView);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            String title = (String) item;
            ImageCardView cardView = (ImageCardView) viewHolder.view;
            cardView.setTitleText(title);
            cardView.setContentText("AYouTube");
            // Use a solid colour as placeholder (no Glide/network needed)
            int color = CARD_COLORS[mColorIndex % CARD_COLORS.length];
            mColorIndex++;
            cardView.setMainImageDimensions(320, 180);
            android.graphics.drawable.ColorDrawable drawable =
                new android.graphics.drawable.ColorDrawable(color);
            cardView.setMainImage(drawable);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
            ImageCardView cardView = (ImageCardView) viewHolder.view;
            cardView.setMainImage(null);
        }
    }
}
