package com.snj.furlencotaskjava.utils;

import android.content.Context;
import android.net.ConnectivityManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class NetworkUtils {

    public static int getFileSize(String urlStr) {
        URL url = null;
        URLConnection urlConnection = null;
        try {
            url = new URL(urlStr);
            urlConnection = url.openConnection();
            urlConnection.connect();
            return urlConnection.getContentLength();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {

        }
        return -1;
    }

    public static Boolean isOnline(Context mContext) {

        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isConnected();
    }
}
