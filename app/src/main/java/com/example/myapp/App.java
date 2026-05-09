package com.example.myapp;

import android.app.Application;

import androidx.multidex.MultiDex;

import com.example.myapp.extractor.NewPipeDownloader;

import org.schabi.newpipe.extractor.NewPipe;

/**
 * Application subclass.
 * Initialises NewPipe Extractor once at app start.
 * Also enables MultiDex for Android 9 (API 28) if needed.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // MultiDex for large dependency graph on API 28
        MultiDex.install(this);
        // Boot NewPipe Extractor with our OkHttp-backed Downloader
        NewPipe.init(NewPipeDownloader.getInstance());
    }
}
