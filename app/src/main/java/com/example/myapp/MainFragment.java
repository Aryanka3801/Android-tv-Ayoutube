package com.example.myapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.example.myapp.extractor.YouTubeExtractorService;
import com.example.myapp.model.UserPreferences;
import com.example.myapp.model.VideoItem;
import com.example.myapp.presenter.VideoCardPresenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Main TV browse screen.
 *
 * Left nav headers:
 *   🏠 Home           – personalised recommendations (from user interests)
 *   🔥 Trending       – YouTube trending kiosk
 *   🎵 Music          – search: "music 2024"
 *   🎮 Gaming         – search: "gaming highlights"
 *   📰 News           – search: "world news today"
 *   ⚽ Sports         – search: "sports highlights"
 *   🎭 Comedy         – search: "comedy videos"
 *   🎓 Education      – search: "educational videos"
 *
 * Tapping a card opens PlayerActivity.
 * The search affordance opens SearchActivity.
 */
public class MainFragment extends BrowseSupportFragment {

    private static final String TAG = "MainFragment";

    // Row index for "Home" — always first
    private static final int ROW_HOME     = 0;
    private static final int ROW_TRENDING = 1;

    private static final String[] CATEGORY_NAMES = {
        "Home", "Trending", "Music", "Gaming", "News", "Sports", "Comedy", "Education"
    };
    private static final String[] CATEGORY_QUERIES = {
        null,                    // Home: built from user interests
        null,                    // Trending: kiosk
        "music 2024",
        "gaming highlights 2024",
        "world news today",
        "sports highlights 2024",
        "comedy videos",
        "educational videos"
    };

    private ArrayObjectAdapter mRowsAdapter;
    private final CompositeDisposable mDisposables = new CompositeDisposable();
    private YouTubeExtractorService mExtractor;
    private UserPreferences mPrefs;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mExtractor = YouTubeExtractorService.getInstance();
        mPrefs     = new UserPreferences(requireContext());

        setupUI();
        setupEventListeners();
        loadAllRows();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  UI Setup
    // ─────────────────────────────────────────────────────────────────────

    private void setupUI() {
        setTitle("AYouTube TV");
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(Color.parseColor("#E53935"));      // YouTube red
        setSearchAffordanceColor(Color.parseColor("#FF6D00"));

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
    }

    private void setupEventListeners() {
        // Open player on card click
        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder,
                                      Object item,
                                      RowPresenter.ViewHolder rowViewHolder,
                                      Row row) {
                if (item instanceof VideoItem) {
                    openPlayer((VideoItem) item);
                }
            }
        });

        // Open search on magnifier click
        setOnSearchClickedListener(v -> {
            Intent intent = new Intent(requireActivity(), SearchActivity.class);
            startActivity(intent);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Data Loading
    // ─────────────────────────────────────────────────────────────────────

    private void loadAllRows() {
        // Pre-populate rows with empty adapters so headers appear immediately
        mRowsAdapter.clear();
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(new VideoCardPresenter());
            HeaderItem header = new HeaderItem(i, CATEGORY_NAMES[i]);
            mRowsAdapter.add(new ListRow(header, rowAdapter));
        }

        // Load Home row (personalised)
        loadHomeRow();

        // Load Trending row
        loadTrendingRow();

        // Load category rows
        for (int i = 2; i < CATEGORY_QUERIES.length; i++) {
            loadCategoryRow(i, CATEGORY_QUERIES[i]);
        }
    }

    private void loadHomeRow() {
        Set<String> interests = mPrefs.getInterests();
        // Pick first interest to seed the home row; real apps would merge multiple
        String primaryInterest = interests.isEmpty() ? "Trending" : interests.iterator().next();
        String query = UserPreferences.interestToQuery(primaryInterest);

        mDisposables.add(
            mExtractor.getByCategory(query)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    videos -> updateRow(ROW_HOME, videos),
                    err    -> logError("Home", err)
                )
        );
    }

    private void loadTrendingRow() {
        mDisposables.add(
            mExtractor.getTrending()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    videos -> updateRow(ROW_TRENDING, videos),
                    err    -> {
                        logError("Trending", err);
                        // Fallback: search "trending" if kiosk fails
                        loadCategoryRow(ROW_TRENDING, "trending videos");
                    }
                )
        );
    }

    private void loadCategoryRow(int rowIndex, String query) {
        mDisposables.add(
            mExtractor.getByCategory(query)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    videos -> updateRow(rowIndex, videos),
                    err    -> logError(CATEGORY_NAMES[rowIndex], err)
                )
        );
    }

    private void updateRow(int rowIndex, List<VideoItem> videos) {
        if (rowIndex >= mRowsAdapter.size()) return;
        ListRow row = (ListRow) mRowsAdapter.get(rowIndex);
        ArrayObjectAdapter rowAdapter = (ArrayObjectAdapter) row.getAdapter();
        rowAdapter.clear();
        rowAdapter.addAll(0, videos);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Navigation
    // ─────────────────────────────────────────────────────────────────────

    private void openPlayer(VideoItem video) {
        // Record interest based on which row the video came from
        mPrefs.recordWatched(video.getVideoId());

        Intent intent = new Intent(requireActivity(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_VIDEO, video);
        startActivity(intent);
    }

    private void logError(String tag, Throwable err) {
        Log.e(TAG, tag + " load failed: " + err.getMessage(), err);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        mDisposables.clear();
        super.onDestroyView();
    }
}
