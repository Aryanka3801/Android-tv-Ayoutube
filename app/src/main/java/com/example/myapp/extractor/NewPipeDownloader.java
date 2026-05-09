package com.example.myapp.extractor;

import androidx.annotation.Nullable;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Bridges NewPipe Extractor's {@link Downloader} interface to OkHttp.
 * NewPipe calls this for every HTTP request it needs to make.
 */
public class NewPipeDownloader extends Downloader {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Safari/537.36";

    private static NewPipeDownloader instance;
    private final OkHttpClient client;

    private NewPipeDownloader(OkHttpClient client) {
        this.client = client;
    }

    public static synchronized NewPipeDownloader getInstance() {
        if (instance == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
            instance = new NewPipeDownloader(okHttpClient);
        }
        return instance;
    }

    @Override
    public Response execute(Request request) throws IOException, ReCaptchaException {
        String httpMethod = request.httpMethod();
        String url        = request.url();
        Map<String, List<String>> headers = request.headers();
        byte[] dataToSend = request.dataToSend();

        RequestBody requestBody = null;
        if (dataToSend != null) {
            requestBody = RequestBody.create(dataToSend);
        }

        okhttp3.Request.Builder reqBuilder = new okhttp3.Request.Builder()
            .url(url)
            .addHeader("User-Agent", USER_AGENT);

        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                for (String val : entry.getValue()) {
                    reqBuilder.addHeader(entry.getKey(), val);
                }
            }
        }

        switch (httpMethod) {
            case "GET":    reqBuilder.get(); break;
            case "POST":   reqBuilder.post(requestBody != null ? requestBody
                               : RequestBody.create(new byte[0])); break;
            case "DELETE": reqBuilder.delete(); break;
            default:       reqBuilder.method(httpMethod, requestBody);
        }

        okhttp3.Response response = client.newCall(reqBuilder.build()).execute();

        if (response.code() == 429) {
            throw new ReCaptchaException("Rate limited (429)", url);
        }

        ResponseBody body = response.body();
        String responseBody = body != null ? body.string() : "";

        Map<String, List<String>> responseHeaders = new HashMap<>(response.headers().toMultimap());

        return new Response(response.code(), response.message(),
                            responseHeaders, responseBody, response.request().url().toString());
    }
}
