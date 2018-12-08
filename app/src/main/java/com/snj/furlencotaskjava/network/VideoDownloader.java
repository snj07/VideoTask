package com.snj.furlencotaskjava.network;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class VideoDownloader {
    public static final int DATA_READY = 1;
    public static final int DATA_NOT_READY = 2;
    public static final int DATA_CONSUMED = 3;
    public static final int DATA_NOT_AVAILABLE = 4;

    //Keeps track of read bytes while serving to video player client from server
    public static int consumedBytes = 0;

    //Keeps track of all bytes read on each while iteration
    private static int readBytes = 0;

    //Length of file being downloaded.
    public static int fileLength = -1;

    public static int dataStatus = -1;

    private String path;
    private String url;

    public VideoDownloader(final String vUrl, final String path) {
        this.path = path;
        this.url = vUrl;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                BufferedInputStream input = null;
                try {
                    final FileOutputStream out = new FileOutputStream(path);

                    try {
                        URL url = new URL(vUrl);

                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.connect();
                        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            throw new RuntimeException("response is not http_ok");
                        }
                        fileLength = connection.getContentLength();

                        input = new BufferedInputStream(connection.getInputStream());
                        byte data[] = new byte[1024 * 50];

                        int len;

                        while ((len = input.read(data)) != -1) {
                            out.write(data, 0, len);
                            out.flush();
                            readBytes += len;
                            Log.w("download", (readBytes / 1024) + "kb of " + (fileLength / 1024) + "kb");
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (out != null) {
                            out.flush();
                            out.close();
                        }
                        if (input != null)
                            input.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }


    public static boolean isDataReady() {
        dataStatus = -1;
        boolean res = false;
        if (fileLength == readBytes) {
            dataStatus = DATA_CONSUMED;
            res = false;
        } else if (readBytes > consumedBytes) {
            dataStatus = DATA_READY;
            res = true;
        } else if (readBytes <= consumedBytes) {
            dataStatus = DATA_NOT_READY;
            res = false;
        } else if (fileLength == -1) {
            dataStatus = DATA_NOT_AVAILABLE;
            res = false;
        }
        return res;
    }
}