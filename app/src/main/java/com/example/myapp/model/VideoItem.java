package com.example.myapp.model;

import java.io.Serializable;

/**
 * Lightweight data holder for a YouTube video card.
 * Passed between activities via Intent extras.
 */
public class VideoItem implements Serializable {

    private final String videoId;
    private final String title;
    private final String channelName;
    private final String thumbnailUrl;
    private final String duration;     // e.g. "12:34"
    private final long viewCount;
    private final String uploadDate;

    public VideoItem(String videoId, String title, String channelName,
                     String thumbnailUrl, String duration,
                     long viewCount, String uploadDate) {
        this.videoId     = videoId;
        this.title       = title;
        this.channelName = channelName;
        this.thumbnailUrl= thumbnailUrl;
        this.duration    = duration;
        this.viewCount   = viewCount;
        this.uploadDate  = uploadDate;
    }

    public String getVideoId()     { return videoId; }
    public String getTitle()       { return title; }
    public String getChannelName() { return channelName; }
    public String getThumbnailUrl(){ return thumbnailUrl; }
    public String getDuration()    { return duration; }
    public long   getViewCount()   { return viewCount; }
    public String getUploadDate()  { return uploadDate; }

    /** YouTube watch URL — what NewPipe Extractor understands */
    public String getWatchUrl() {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    @Override
    public String toString() {
        return "VideoItem{id='" + videoId + "', title='" + title + "'}";
    }
}
