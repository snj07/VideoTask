package com.snj.furlencotaskjava.storage;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * provides meta-data and access to a stream for resources on mobile.
 */
public class ExternalDataSource {

    private final File vFileResource;
    long contentLength;
    private FileInputStream inputStream;
    private String TAG = getClass().getSimpleName();

    public ExternalDataSource(File resource) {
        vFileResource = resource;
        Log.i(TAG, "File Path is: " + resource.getPath());
    }

    /**
     * Returns a MIME-compatible content type
     */
    public String getContentType() {
        return "video/mp4";
    }

    /**
     * Creates and opens an input stream that returns the contents of the resource.
     */
    public InputStream createInputStream() throws IOException {
        getInputStream();
        return inputStream;
    }

    /**
     * Returns the length of resource in bytes.
     * else return -1 => for unknown size stream
     */
    public long getContentLength(boolean ignoreSimulation) {
        if (!ignoreSimulation) {
            return -1;
        }
        return contentLength;
    }

    public void getInputStream() {
        try {
            inputStream = new FileInputStream(vFileResource);
            Log.e(TAG, "find found : " + vFileResource.exists());
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Cannot find file : " + vFileResource.exists());
            e.printStackTrace();
        }
        contentLength = vFileResource.length();
        Log.i(TAG, "content length is: " + contentLength);
    }

}