package com.example.myapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.example.myapp.extractor.YouTubeExtractorService;
import com.example.myapp.model.VideoItem;
import com.example.myapp.presenter.VideoCardPresenter;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * Leanback search fragment.
 *
 * The user types with the on-screen keyboard; queries are debounced 600ms
 * before hitting NewPipe Extractor so we don't spam requests on every keypress.
 * Results appear in a single "YouTube Results" row of video cards.
 */
public class SearchFragment extends SearchSupportFragment
        implements SearchSupportFragment.SearchResultProvider {

    private static final String TAG           = "SearchFragment";
    private static final long   DEBOUNCE_MS   = 600;

    private ArrayObjectAdapter     mResultsAdapter;
    private ArrayObjectAdapter     mRowsAdapter;
    private final CompositeDisposable mDisposables = new CompositeDisposable();

    // Debounce search queries
    private final PublishSubject<String> mQuerySubject = PublishSubject.create();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);

        setupResultsRow();
        setupQueryDebounce();

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemVH, Object item,
                                      RowPresenter.ViewHolder rowVH, Row row) {
                if (item instanceof VideoItem) {
                    openPlayer((VideoItem) item);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        mDisposables.clear();
        super.onDestroyView();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SearchResultProvider
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (query == null || query.trim().isEmpty()) {
            mResultsAdapter.clear();
            return true;
        }
        mQuerySubject.onNext(query.trim());
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return onQueryTextChange(query);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Setup
    // ─────────────────────────────────────────────────────────────────────

    private void setupResultsRow() {
        mResultsAdapter = new ArrayObjectAdapter(new VideoCardPresenter());
        HeaderItem header = new HeaderItem(0, "YouTube Results");
        mRowsAdapter.add(new ListRow(header, mResultsAdapter));
    }

    private void setupQueryDebounce() {
        mDisposables.add(
            mQuerySubject
                .debounce(DEBOUNCE_MS, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .switchMapSingle(query ->
                    YouTubeExtractorService.getInstance()
                        .search(query)
                        .onErrorReturn(err -> {
                            Log.e(TAG, "Search error: " + err.getMessage(), err);
                            return new java.util.ArrayList<>();
                        })
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    this::showResults,
                    err -> Log.e(TAG, "Query stream error", err)
                )
        );
    }

    private void showResults(List<VideoItem> videos) {
        mResultsAdapter.clear();
        if (videos.isEmpty()) {
            // Could show a "no results" card here
            return;
        }
        mResultsAdapter.addAll(0, videos);
    }

    private void openPlayer(VideoItem video) {
        Intent intent = new Intent(requireActivity(), PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_VIDEO, video);
        startActivity(intent);
    }
}
