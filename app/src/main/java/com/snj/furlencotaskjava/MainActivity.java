package com.snj.furlencotaskjava;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.snj.furlencotaskjava.ui.VideoActivity;
import com.snj.furlencotaskjava.utils.Constants;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, VideoActivity.class));
            }
        });

        Button delete = findViewById(R.id.delete_button);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fileName = Constants.VIDEO_URL.substring(Constants.VIDEO_URL.lastIndexOf('/') + 1);
                final String path = getFilesDir().getAbsolutePath() + "/" + fileName;
                File file = new File(path);
                if (file.exists()) {
                    Log.d("MainActivity", "deleted");
                    file.delete();
                    getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

                }
            }
        });

    }
}
