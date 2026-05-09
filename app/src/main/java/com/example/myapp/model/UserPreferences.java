package com.example.myapp.model;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Persists the user's interest categories and watch history IDs
 * so the Home tab can show personalised recommendations.
 */
public class UserPreferences {

    private static final String PREF_FILE    = "ayoutube_prefs";
    private static final String KEY_INTERESTS = "interests";
    private static final String KEY_HISTORY   = "watch_history";
    private static final int    MAX_HISTORY   = 50;

    // Default interests shown on first launch
    private static final Set<String> DEFAULT_INTERESTS = new HashSet<>(
        Arrays.asList("Trending", "Music", "Gaming")
    );

    private final SharedPreferences prefs;

    public UserPreferences(Context ctx) {
        prefs = ctx.getApplicationContext()
                   .getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public Set<String> getInterests() {
        return prefs.getStringSet(KEY_INTERESTS, DEFAULT_INTERESTS);
    }

    public void setInterests(Set<String> interests) {
        prefs.edit().putStringSet(KEY_INTERESTS, interests).apply();
    }

    public void addInterest(String category) {
        Set<String> cur = new HashSet<>(getInterests());
        cur.add(category);
        setInterests(cur);
    }

    /** Record a watched video so future recommendations adapt */
    public void recordWatched(String videoId) {
        Set<String> history = new HashSet<>(getWatchHistory());
        history.add(videoId);
        // Trim to max — SharedPreferences Set has no order, so just cap size
        if (history.size() > MAX_HISTORY) {
            history.remove(history.iterator().next());
        }
        prefs.edit().putStringSet(KEY_HISTORY, history).apply();
    }

    public Set<String> getWatchHistory() {
        return prefs.getStringSet(KEY_HISTORY, new HashSet<>());
    }

    /**
     * Map an interest label to a YouTube search query used for
     * the Home tab recommendation rows.
     */
    public static String interestToQuery(String interest) {
        switch (interest) {
            case "Music":     return "music 2024";
            case "Gaming":    return "gaming highlights 2024";
            case "News":      return "world news today";
            case "Sports":    return "sports highlights 2024";
            case "Comedy":    return "comedy videos";
            case "Education": return "educational videos";
            case "Tech":      return "technology news 2024";
            case "Cooking":   return "cooking recipes";
            default:          return "trending videos";
        }
    }
}
