package com.snj.furlencotaskjava.network;

import android.app.Activity;

import java.io.File;

public class VideoDownloadAndPlayService {
    private static VideoStreamingServer server;

    private VideoDownloadAndPlayService(VideoStreamingServer server) {
        this.server = server;
    }

    public static VideoDownloadAndPlayService startServer(final Activity activity, String videoUrl, String pathToSaveVideo, final String ipOfServer, final VideoStreamInterface callback) {
//        Thread t  =new Thread();
//        t.setPriority(Thread.MAX_PRIORITY);
//        t.start();
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
