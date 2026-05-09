package com.example.myapp;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.OptIn;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import com.example.myapp.extractor.YouTubeExtractorService;
import com.example.myapp.model.VideoItem;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import okhttp3.OkHttpClient;

/**
 * Full-screen video player using ExoPlayer (Media3).
 *
 * Flow:
 *  1. Receives {@link VideoItem} via Intent extra.
 *  2. Calls {@link YouTubeExtractorService#getStreamUrl(String)} on IO thread.
 *  3. Feeds the resolved HTTPS URL into ExoPlayer.
 *  4. Shows loading overlay while extracting / buffering.
 *  5. Shows error overlay with Retry on failure.
 *
 * Supports Android 9 (API 28) and above.
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayerActivity extends FragmentActivity {

    public static final String EXTRA_VIDEO = "extra_video";
    private static final String TAG        = "PlayerActivity";

    private PlayerView       mPlayerView;
    private View             mLoadingOverlay;
    private View             mErrorOverlay;
    private TextView         mErrorText;
    private Button           mRetryButton;

    private ExoPlayer        mPlayer;
    private VideoItem        mVideo;
    private final CompositeDisposable mDisposables = new CompositeDisposable();

    // ─────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mPlayerView     = findViewById(R.id.player_view);
        mLoadingOverlay = findViewById(R.id.loading_overlay);
        mErrorOverlay   = findViewById(R.id.error_overlay);
        mErrorText      = findViewById(R.id.error_text);
        mRetryButton    = findViewById(R.id.retry_button);

        Serializable extra = getIntent().getSerializableExtra(EXTRA_VIDEO);
        if (!(extra instanceof VideoItem)) {
            finish();
            return;
        }
        mVideo = (VideoItem) extra;

        mRetryButton.setOnClickListener(v -> {
            mErrorOverlay.setVisibility(View.GONE);
            loadAndPlay();
        });

        initPlayer();
        loadAndPlay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPlayer != null) mPlayer.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPlayer != null) mPlayer.play();
    }

    @Override
    protected void onDestroy() {
        mDisposables.clear();
        releasePlayer();
        super.onDestroy();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Player setup
    // ─────────────────────────────────────────────────────────────────────

    private void initPlayer() {
        OkHttpClient okHttp = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

        OkHttpDataSource.Factory httpFactory =
            new OkHttpDataSource.Factory(okHttp)
                .setUserAgent(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Safari/537.36");

        mPlayer = new ExoPlayer.Builder(this)
            .setMediaSourceFactory(new DefaultMediaSourceFactory(httpFactory))
            .build();

        mPlayerView.setPlayer(mPlayer);
        mPlayerView.setUseController(true);
        mPlayerView.setControllerAutoShow(true);
        mPlayerView.setControllerShowTimeoutMs(4000);
        mPlayerView.setControllerHideOnTouch(false);

        mPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    mLoadingOverlay.setVisibility(View.VISIBLE);
                } else {
                    mLoadingOverlay.setVisibility(View.GONE);
                }
                if (state == Player.STATE_READY) {
                    mErrorOverlay.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Player error: " + error.getMessage(), error);
                mLoadingOverlay.setVisibility(View.GONE);
                mErrorText.setText(getString(R.string.error_load) +
                    "\n" + error.getMessage());
                mErrorOverlay.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadAndPlay() {
        mLoadingOverlay.setVisibility(View.VISIBLE);

        mDisposables.add(
            YouTubeExtractorService.getInstance()
                .getStreamUrl(mVideo.getVideoId())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    this::playUrl,
                    err -> {
                        Log.e(TAG, "Stream extraction failed", err);
                        mLoadingOverlay.setVisibility(View.GONE);
                        mErrorText.setText(getString(R.string.error_load) +
                            "\n" + err.getMessage());
                        mErrorOverlay.setVisibility(View.VISIBLE);
                    }
                )
        );
    }

    private void playUrl(String url) {
        if (mPlayer == null) return;

        MediaItem mediaItem = MediaItem.fromUri(url);
        mPlayer.setMediaItem(mediaItem);
        mPlayer.prepare();
        mPlayer.setPlayWhenReady(true);
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  TV D-pad: back key exits player
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK ||
            keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (mPlayer != null && mPlayer.isPlaying()) {
                mPlayer.pause();
            }
            finish();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
            keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (mPlayer != null) {
                if (mPlayer.isPlaying()) mPlayer.pause();
                else mPlayer.play();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
