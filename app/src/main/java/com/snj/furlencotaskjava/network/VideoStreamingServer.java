package com.snj.furlencotaskjava.network;


import android.util.Log;

import com.snj.furlencotaskjava.storage.ExternalDataSource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Local HTTP server to send the file from local server
 */
public class VideoStreamingServer implements Runnable {
    private static final String TAG = VideoStreamingServer.class.getName();
    private int port = 0;
    private boolean isRunning = false;
    private ServerSocket socket;
    private Thread thread;
    private long cbSkip;
    private boolean seekRequest;
    private File vFile;

    private boolean supportPlayWhileDownloading = false;

    public VideoStreamingServer(File file) {
        vFile = file;
    }

    public int getPort() {
        return port;
    }

    /**
     * Prepare the server to start.
     * This only needs to be called once per instance. Once initialized, the
     * server can be started and stopped as needed.
     */
    public String init(String ip) {
        String url = null;
        try {
            InetAddress inet = InetAddress.getByName(ip);

            byte[] bytes = inet.getAddress();
            socket = new ServerSocket(port, 0, InetAddress.getByAddress(bytes));
            socket.setSoTimeout(0);
            port = socket.getLocalPort();
            url = "http://" + socket.getInetAddress().getHostAddress() + ":"
                    + port;
            Log.e(TAG, "Server started at " + url);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Error UnknownHostException server", e);
        } catch (IOException e) {
            Log.e(TAG, "Error IOException server", e);
        }
        return url;
    }

    public String getFileUrl() {
        return "http://" + socket.getInetAddress().getHostAddress() + ":"
                + port + "/" + vFile.getName();
    }

    /**
     * Start the server.
     */
    public void start() {
        thread = new Thread(this);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        isRunning = true;
    }

    /**
     * Stop stops the thread listening to the port. It may take up to five
     * seconds to close the service and this call blocks until that occurs.
     */
    public void stop() {
        isRunning = false;
        if (thread == null) {
            Log.e(TAG, "Server was stopped without being started.");
            return;
        }
        Log.e(TAG, "Stopping server.");
        thread.interrupt();
    }

    /**
     * Checks if the server is running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * This is used internally by the server and should not be called directly.
     */
    @Override
    public void run() {
        Log.e(TAG, "running");
        while (isRunning) {
            try {
                Socket client = socket.accept();
                if (client == null) {
                    continue;
                }
                Log.e(TAG, "client connected at " + port);
                ExternalDataSource data = new ExternalDataSource(
                        vFile);
                Log.e(TAG, "processing request...");
                processRequest(data, client);
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "No client connected, waiting for client...", e);
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to client", e);
            }
        }
        Log.e(TAG, "Server interrupted or stopped. Shutting down.");
    }

    /**
     * Find byte index separating header from body.
     * It must be the last byte of the first two sequential new lines.
     **/
    private int findHeaderEnd(final byte[] buf, int rLen) {
        int splitByte = 0;
        while (splitByte + 3 < rLen) {
            if (buf[splitByte] == '\r' && buf[splitByte + 1] == '\n'
                    && buf[splitByte + 2] == '\r' && buf[splitByte + 3] == '\n')
                return splitByte + 4;
            splitByte++;
        }
        return 0;
    }

    /*
     * Sends the HTTP response to the client, including headers (as applicable)
     * and content.
     */
    private void processRequest(ExternalDataSource dataSource,
                                Socket client) throws IllegalStateException, IOException {
        if (dataSource == null) {
            Log.e(TAG, "Invalid (null) resource.");
            client.close();
            return;
        }
        InputStream is = client.getInputStream();
        final int buffSize = 8192;
        byte[] buf = new byte[buffSize];
        int splitByte;
        int rLen = 0;
        {
            int read = is.read(buf, 0, buffSize);
            while (read > 0) {
                rLen += read;
                splitByte = findHeaderEnd(buf, rLen);
                if (splitByte > 0)
                    break;
                read = is.read(buf, rLen, buffSize - rLen);
            }
        }

        // Create a BufferedReader for parsing the header.
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf, 0, rLen);
        BufferedReader hin = new BufferedReader(new InputStreamReader(byteArrayInputStream));
        Properties pre = new Properties();
        Properties params = new Properties();
        Properties header = new Properties();

        try {
            decodeHeader(hin, pre, params, header);
        } catch (InterruptedException e1) {
            Log.e(TAG, "Exception: " + e1.getMessage());
            e1.printStackTrace();
        }
        for (Entry<Object, Object> e : header.entrySet()) {
            Log.e(TAG, "Header: " + e.getKey() + " : " + e.getValue());
        }
        String range = header.getProperty("range");
        cbSkip = 0;
        seekRequest = false;
        if (range != null) {
            Log.i(TAG, "range is: " + range);
            seekRequest = true;
            range = range.substring(6);
            int charPos = range.indexOf('-');
            if (charPos > 0) {
                range = range.substring(0, charPos);
            }
            cbSkip = Long.parseLong(range);
            Log.e(TAG, "range found!! " + cbSkip);
        }
        String headers = "";
        // Log.e(TAG, "is seek request: " + seekRequest);
        if (seekRequest) {// It is a seek or skip request if there's a Range
            // header
            headers += "HTTP/1.1 206 Partial Content\r\n";
            headers += "Content-Type: " + dataSource.getContentType() + "\r\n";
            headers += "Accept-Ranges: bytes\r\n";
            headers += "Content-Length: " + dataSource.getContentLength(false)
                    + "\r\n";
            headers += "Content-Range: bytes " + cbSkip + "-"
                    + dataSource.getContentLength(true) + "/*\r\n";
            headers += "\r\n";
        } else {
            headers += "HTTP/1.1 200 OK\r\n";
            headers += "Content-Type: " + dataSource.getContentType() + "\r\n";
            headers += "Accept-Ranges: bytes\r\n";
            headers += "Content-Length: " + dataSource.getContentLength(false)
                    + "\r\n";
            headers += "\r\n";
        }

        InputStream data = null;
        try {
            data = dataSource.createInputStream();
            byte[] buffer = headers.getBytes();
            Log.e(TAG, "writing to client");
            client.getOutputStream().write(buffer, 0, buffer.length);

            // Start sending content.

            byte[] buff = new byte[1024 * 50];
            Log.e(TAG, "No of bytes skipped: " + data.skip(cbSkip));
            int cbSentThisBatch = 0;
            while (isRunning) {
                if (supportPlayWhileDownloading) {
                    // Check if data is ready
                    while (!VideoDownloader.isDataReady() && isRunning) {
                        if (VideoDownloader.dataStatus == VideoDownloader.DATA_READY) {
                            Log.i(TAG, "start reading bytes : state : Data ready");
                            break;
                        } else if (VideoDownloader.dataStatus == VideoDownloader.DATA_CONSUMED) {
                            Log.i(TAG, "reading bytes end : state : All Data consumed");
                            break;
                        } else if (VideoDownloader.dataStatus == VideoDownloader.DATA_NOT_READY) {
                            Log.e(TAG, "error in reading bytes : state : Data not ready");
                        } else if (VideoDownloader.dataStatus == VideoDownloader.DATA_NOT_AVAILABLE) {
                            Log.e(TAG, "error in reading bytes : state : Data not available");
                        }
                    }
                    Log.i(TAG, "reading bytes : Data ready");
                }

                int cbRead = data.read(buff, 0, buff.length);
                if (cbRead == -1) {
                    Log.e(TAG, "ready bytes are -1 and this is simulate streaming, close the ips and create another  ");
                    data.close();
                    data = dataSource.createInputStream();
                    cbRead = data.read(buff, 0, buff.length);
                    if (cbRead == -1) {
                        Log.e(TAG, "error in reading bytes");
//                        throw new IOException(
//                                "Error re-opening data source for looping.");
                        break;
                    }
                }
                client.getOutputStream().write(buff, 0, cbRead);
                client.getOutputStream().flush();
                cbSkip += cbRead;
                cbSentThisBatch += cbRead;

                if (supportPlayWhileDownloading)
                    VideoDownloader.consumedBytes += cbRead;
            }
            Log.e(TAG, "cbSentThisBatch: " + cbSentThisBatch);
            // If we did nothing this batch, block for a second
            if (cbSentThisBatch == 0) {
                Log.e(TAG, "Blocking until more data appears");
                Thread.sleep(1000);
            }
        } catch (SocketException e) {
            // Ignore when the client breaks connection
            Log.e(TAG, "Ignoring " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error getting content stream.", e);
        } catch (Exception e) {
            Log.e(TAG, "Error streaming file content.", e);
        } finally {
            if (data != null) {
                data.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }

    /**
     * Decodes the sent headers and loads the data into java Properties' key -
     * value pairs
     **/
    private void decodeHeader(BufferedReader in, Properties pre,
                              Properties params, Properties header) throws InterruptedException {
        try {
            // Read the request line
            String inLine = in.readLine();
            if (inLine == null)
                return;
            StringTokenizer st = new StringTokenizer(inLine);
            if (!st.hasMoreTokens())
                Log.e(TAG, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");

            String method = st.nextToken();
            pre.put("method", method);

            if (!st.hasMoreTokens())
                Log.e(TAG, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");

            String uri = st.nextToken();

            // Decode parameters from the URI
            int qmi = uri.indexOf('?');
            if (qmi >= 0) {
                decodeParms(uri.substring(qmi + 1), params);
                uri = decodePercent(uri.substring(0, qmi));
            } else
                uri = decodePercent(uri);

            // If there's another token, it's protocol version,
            // followed by HTTP headers. Ignore version but parse headers.
            // NOTE: this now forces header names lowercase since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens()) {
                String line = in.readLine();
                while (line != null && line.trim().length() > 0) {
                    int p = line.indexOf(':');
                    if (p >= 0)
                        header.put(line.substring(0, p).trim().toLowerCase(),
                                line.substring(p + 1).trim());
                    line = in.readLine();
                }
            }

            pre.put("uri", uri);
        } catch (IOException ioe) {
            Log.e(TAG,
                    "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
        }
    }

    /**
     * Decodes parameters in percent-encoded URI-format ( e.g.
     * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given
     * Properties. NOTE: this doesn't support multiple identical keys due to the
     * simplicity of Properties -- if you need multiples, you might want to
     * replace the Properties with a Hashtable of Vectors or such.
     */
    private void decodeParms(String params, Properties p)
            throws InterruptedException {
        if (params == null)
            return;

        StringTokenizer st = new StringTokenizer(params, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            if (sep >= 0)
                p.put(decodePercent(e.substring(0, sep)).trim(),
                        decodePercent(e.substring(sep + 1)));
        }
    }

    /**
     * Decodes the percent encoding scheme.
     * For example: "an+example%20string" -> "an example string"
     */
    private String decodePercent(String str) {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                switch (c) {
                    case '+':
                        sb.append(' ');
                        break;
                    case '%':
                        sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
                        i += 2;
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            return sb.toString();
        } catch (RuntimeException e) {
            Log.e(TAG, "BAD REQUEST: bad request encoding.");
            return null;
        }
    }

    public boolean isSupportPlayWhileDownloading() {
        return supportPlayWhileDownloading;
    }

    public void setSupportPlayWhileDownloading(boolean supportPlayWhileDownloading) {
        this.supportPlayWhileDownloading = supportPlayWhileDownloading;
    }

}