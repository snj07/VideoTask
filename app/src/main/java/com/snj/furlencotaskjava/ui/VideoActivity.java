package com.snj.furlencotaskjava.ui;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.snj.furlencotaskjava.R;
import com.snj.furlencotaskjava.network.VideoDownloadAndPlayService;
import com.snj.furlencotaskjava.utils.Constants;
import com.snj.furlencotaskjava.utils.NetworkUtils;

import java.io.File;

public class VideoActivity extends AppCompatActivity {


    public static final String TAG = VideoActivity.class.getSimpleName();
    private static final int DELAY = 2000;
    private MediaController mediaController = null;
    private VideoView videoView;
    private String fileName;
    private Uri videoPath;
    private VideoDownloadAndPlayService videoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_view_activity);

        videoView = findViewById(R.id.videoView);
        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);


        fileName = Constants.VIDEO_URL.substring(Constants.VIDEO_URL.lastIndexOf('/') + 1);
        Log.i(TAG, "Video file name: " + fileName);
        final String path = getFilesDir() + "/" + fileName;
        Log.i(TAG, "Path: " + path);
        File file = new File(path);
        Log.i(TAG, "File exists: " + file.exists());

        if (file.exists()) {
            videoPath = Uri.parse(path);
            videoView.setVideoURI(videoPath);
            videoView.requestFocus();
            videoView.start();

        } else {
            if (NetworkUtils.isOnline(getApplicationContext())) {
                startServer(path);
            } else {
                Toast.makeText(VideoActivity.this, getResources().getText(R.string.no_internet), Toast.LENGTH_LONG).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, VideoActivity.DELAY);
            }

        }


    }

    public void startServer(final String path) {
        videoService = new VideoDownloadAndPlayService().startServer(this, Constants.VIDEO_URL, path, Constants.LOCAL_IP, new VideoDownloadAndPlayService.VideoStreamInterface() {
            @Override
            public void onServerStart(String videoStreamUrl) {
                videoPath = Uri.parse(videoStreamUrl);
                videoView.setVideoURI(videoPath);
                videoView.requestFocus();
                videoView.start();

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (videoService != null) {
            videoService.stop();
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }


}
