package com.snj.furlencotaskjava.ui;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

import com.snj.furlencotaskjava.R;
import com.snj.furlencotaskjava.network.VideoDownloadAndPlayService;

import java.io.File;

public class VideoActivity extends AppCompatActivity {

    String videoUrl = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
    private MediaController mediaController = null;
    VideoView playVideo;
    String fileName;
    Uri videoPath;
    VideoDownloadAndPlayService videoService;

    //    VideoDownloade videoDownloadPlay = null
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.video_view_activity);
        fileName = videoUrl.substring(videoUrl.lastIndexOf('/') + 1);
        Log.e("--s Video name", fileName);
        String path = getFilesDir().getAbsolutePath() + "/" + fileName;
        Log.e("Video name", fileName);
        File file = new File(path);
        Log.e("--s exists", file.exists() + "");
        playVideo = findViewById(R.id.videoView);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(playVideo);
        videoService = VideoDownloadAndPlayService.startServer(this, videoUrl, path, "127.0.0.1", new VideoDownloadAndPlayService.VideoStreamInterface() {
            @Override
            public void onServerStart(String videoStreamUrl) {
                // use videoStreamUrl to play video through media player
                playVideo.setMediaController(mediaController);
                videoPath = Uri.parse(videoStreamUrl);
                playVideo.setVideoURI(videoPath);
                playVideo.requestFocus();
                playVideo.start();
            }
        });
//        if (file.exists()) {
//            playVideo.setMediaController(mediaController);
//            videoPath = Uri.parse(path);
//            playVideo.setVideoURI(videoPath);
//            playVideo.requestFocus();
//            playVideo.start();
//            Log.e("Video File found", videoPath.toString());
//        } else {
//            videoService = VideoDownloadAndPlayService.startServer(this, videoUrl, path, "127.0.0.1", new VideoDownloadAndPlayService.VideoStreamInterface() {
//                @Override
//                public void onServerStart(String videoStreamUrl) {
//                    // use videoStreamUrl to play video through media player
//                    playVideo.setMediaController(mediaController);
//                    videoPath = Uri.parse(videoStreamUrl);
//                    playVideo.setVideoURI(videoPath);
//                    playVideo.requestFocus();
//                    playVideo.start();
//                }
//            });
//
//        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (videoService != null) {
            videoService.stop();
        }
    }
}
