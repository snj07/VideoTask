package com.snj.furlencotaskjava.network;

import android.app.Activity;

import java.io.File;

public class VideoDownloadAndPlayService {
    private static VideoStreamingServer server;

    public VideoDownloadAndPlayService(VideoStreamingServer server) {
        this.server = server;
    }

    public VideoDownloadAndPlayService() {

    }

    public VideoDownloadAndPlayService startServer(final Activity activity, String videoUrl, String pathToSaveVideo, final String ipOfServer, final VideoStreamInterface callback) {

        new VideoDownloader(videoUrl, pathToSaveVideo);
        server = new VideoStreamingServer(new File(pathToSaveVideo));
        server.setSupportPlayWhileDownloading(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                server.init(ipOfServer);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        server.start();
                        callback.onServerStart(server.getFileUrl());
                    }
                });
            }
        }).start();

        return new VideoDownloadAndPlayService(server);
    }

    public static boolean isServerRunning() {
        return server.isRunning();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop();
    }

    public interface VideoStreamInterface {
        void onServerStart(String videoStreamUrl);

    }
}
