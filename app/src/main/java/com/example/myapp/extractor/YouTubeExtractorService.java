package com.example.myapp.extractor;

import android.util.Log;

import com.example.myapp.model.VideoItem;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeSearchExtractor;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.ListExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Thin service layer around NewPipe Extractor.
 * All public methods return RxJava3 Singles — subscribe on IO, observe on main.
 *
 * Key operations:
 *  - search(query)          → list of VideoItems
 *  - getTrending()          → trending VideoItems
 *  - getByCategory(query)   → category VideoItems (used for Home recommendations)
 *  - getStreamUrl(videoId)  → best HTTPS stream URL for ExoPlayer
 */
public class YouTubeExtractorService {

    private static final String TAG = "YTExtractor";
    private static volatile YouTubeExtractorService instance;
    private final StreamingService youtubeService;

    private YouTubeExtractorService() {
        // NewPipe.init must already have been called in App.onCreate()
        youtubeService = ServiceList.YouTube;
    }

    public static YouTubeExtractorService getInstance() {
        if (instance == null) {
            synchronized (YouTubeExtractorService.class) {
                if (instance == null) instance = new YouTubeExtractorService();
            }
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Search
    // ─────────────────────────────────────────────────────────────────────

    public Single<List<VideoItem>> search(String query) {
        return Single.fromCallable(() -> doSearch(query))
                     .subscribeOn(Schedulers.io());
    }

    private List<VideoItem> doSearch(String query) throws Exception {
        SearchExtractor extractor = youtubeService.getSearchExtractor(query);
        extractor.fetchPage();
        return parseInfoItems(extractor.getInitialPage().getItems());
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Trending
    // ─────────────────────────────────────────────────────────────────────

    public Single<List<VideoItem>> getTrending() {
        return Single.fromCallable(this::doGetTrending)
                     .subscribeOn(Schedulers.io());
    }

    private List<VideoItem> doGetTrending() throws Exception {
        // YouTube kiosk "Trending"
        org.schabi.newpipe.extractor.kiosk.KioskExtractor kiosk =
            youtubeService.getKioskList().getDefaultKioskExtractor();
        kiosk.fetchPage();
        return parseInfoItems(kiosk.getInitialPage().getItems());
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Category / Recommendation rows (for Home tab)
    // ─────────────────────────────────────────────────────────────────────

    public Single<List<VideoItem>> getByCategory(String searchQuery) {
        return search(searchQuery);    // reuses search with category keyword
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Stream URL extraction  (called just before playback)
    // ─────────────────────────────────────────────────────────────────────

    public Single<String> getStreamUrl(String videoId) {
        return Single.fromCallable(() -> doGetStreamUrl(videoId))
                     .subscribeOn(Schedulers.io());
    }

    private String doGetStreamUrl(String videoId) throws Exception {
        String watchUrl = "https://www.youtube.com/watch?v=" + videoId;
        StreamInfo info = StreamInfo.getInfo(youtubeService, watchUrl);

        // Prefer a progressive HTTPS video+audio stream (no DASH merge needed)
        List<VideoStream> videoStreams = info.getVideoStreams();

        String bestUrl = null;
        int    bestRes = 0;

        for (VideoStream vs : videoStreams) {
            if (vs.getUrl() == null) continue;
            if (!vs.getUrl().startsWith("https")) continue;
            // getResolution() returns e.g. "720p"
            String resStr = vs.getResolution().replace("p", "");
            int res = 0;
            try { res = Integer.parseInt(resStr); } catch (NumberFormatException ignored) {}
            if (res > bestRes && res <= 1080) {
                bestRes = res;
                bestUrl = vs.getUrl();
            }
        }

        if (bestUrl == null && !videoStreams.isEmpty()) {
            bestUrl = videoStreams.get(0).getUrl();
        }

        // Fall back to HLS manifest if no progressive stream found
        if (bestUrl == null) {
            String hlsUrl = info.getHlsUrl();
            if (hlsUrl != null && !hlsUrl.isEmpty()) return hlsUrl;
            throw new Exception("No playable stream found for " + videoId);
        }

        return bestUrl;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────

    private List<VideoItem> parseInfoItems(List<InfoItem> items) {
        List<VideoItem> result = new ArrayList<>();
        for (InfoItem item : items) {
            if (item instanceof StreamInfoItem) {
                StreamInfoItem si = (StreamInfoItem) item;
                String thumbnail = si.getThumbnails().isEmpty()
                    ? "" : si.getThumbnails().get(0).getUrl();
                String duration = formatDuration(si.getDuration());
                String videoId  = extractVideoId(si.getUrl());
                if (videoId == null) continue;
                result.add(new VideoItem(
                    videoId,
                    si.getName(),
                    si.getUploaderName(),
                    thumbnail,
                    duration,
                    si.getViewCount(),
                    si.getTextualUploadDate() != null ? si.getTextualUploadDate() : ""
                ));
            }
        }
        return result;
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%d:%02d", m, s);
    }

    private String extractVideoId(String url) {
        // handles https://www.youtube.com/watch?v=XXXXXXXXXXX
        if (url == null) return null;
        int idx = url.indexOf("v=");
        if (idx < 0) return null;
        String id = url.substring(idx + 2);
        int amp = id.indexOf('&');
        return amp < 0 ? id : id.substring(0, amp);
    }
}
