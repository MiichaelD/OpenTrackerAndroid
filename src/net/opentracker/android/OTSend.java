/*
 *  Copyright (C) 2011 Opentracker.net 
 * 
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 * 
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 * 
 *  The full license is located at the root of this distribution
 *  in the LICENSE file.
 *
 *  Please report bugs to support@opentracker.net
 *
 */
package net.opentracker.android;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

/**
 * OTSend provides methods for sending key value pairs to Opentracker's logging/
 * analytics engines via HTTP POST requests, and uploading (compressed) files.
 * 
 * For the HTTP POST requests to work on android you must declare Internet
 * permissions in your apps manifest by adding the following line to
 * AndroidManifest.xml. This allows your application to use any Internet
 * connections.
 * 
 * The application will need the following android permissions defined:
 * <uses-permission android:name="android.permission.INTERNET" />
 * 
 * @author $Author: eddie $ (latest svn author)
 * @version $Id: OTSend.java 14171 2012-03-08 15:32:37Z eddie $
 */
public class OTSend {

    private static final String boundary = "*****";
    private static final String lineEnd = "\r\n";
    private static final String twoHyphens = "--";
    private static final String TAG = OTSend.class.getName().intern();

    private static byte[] buffer;

    private static int bytesRead, bytesAvailable, bufferSize;
    private static final int maxBufferSize = 1 * 1024 * 1024; // 1MB

    private static HttpURLConnection conn = null;

    /*
     * The default socket timeout (SO_TIMEOUT) in milliseconds which is the
     * timeout for waiting for data. A timeout value of zero is interpreted as
     * an infinite timeout. This value is used when no socket timeout is set in
     * the HTTP method parameters.
     */
    private static final int HTTP_SOCKET_TIMEOUT = 3000;
    
    public static final String DEFAULT_LOG_URL = "http://log.opentracker.net/";
    private static final String DEFAULT_UPLOAD_SERVER = "http://upload.opentracker.net/upload/upload.jsp";

    private static DataOutputStream dos = null;
    private static DataInputStream inStream = null;

    /**
     * getResponseBody function gives out the HTTP POST data from the given
     * httpResponse output: data from the http as string input : httpEntity type
     * 
     * based on:
     * http://thinkandroid.wordpress.com/2009/12/30/getting-response-body-of-httpresponse/
     */
    private static String getResponseBody(final HttpEntity entity) throws IOException, ParseException {
        LogWrapper.v(TAG, "getResponseBody(final HttpEntity entity)");

        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        InputStream instream = entity.getContent();
        if (instream == null) {
            return "";
        }
        if (entity.getContentLength() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
        }
        String charset = EntityUtils.getContentCharSet(entity);
        if (charset == null) {
            charset = HTTP.DEFAULT_CONTENT_CHARSET;
        }
        Reader reader = new InputStreamReader(instream, charset);
        StringBuilder buffer = new StringBuilder();
        try {
            char[] tmp = new char[1024];
            int l;
            while ((l = reader.read(tmp)) != -1) {
                buffer.append(tmp, 0, l);
            }
        } finally {
            reader.close();
        }
        return buffer.toString();
    }

    /**
     * Sends the key value pairs to Opentracker's logging/ analytics engines via
     * HTTP POST requests.
     * 
     * Based on sending key value pairs documentated at:
     * http://api.opentracker.net/api/inserts/insert_event.jsp
     * 
     * @param keyValuePairs
     *            the key value pairs (plain text utf-8 strings) to send to the
     *            logging service.
     * 
     * @return the response as string, null if an exception is caught
     */
    protected static String send(HashMap<String, String> keyValuePairs) {
        LogWrapper.v(TAG, "send(HashMap<String, String> keyValuePairs)");

        // http://www.wikihow.com/Execute-HTTP-POST-Requests-in-Android
        // http://hc.apache.org/httpclient-3.x/tutorial.html
        HttpClient client = new DefaultHttpClient();

        // time to wait before throwing timeout exception
        client.getParams().setParameter("http.socket.timeout", HTTP_SOCKET_TIMEOUT);

        HttpPost post = new HttpPost(DEFAULT_LOG_URL);
        post.getParams().setParameter("http.socket.timeout", HTTP_SOCKET_TIMEOUT);

        Iterator<Entry<String, String>> it = keyValuePairs.entrySet().iterator();

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            pairs.add(new BasicNameValuePair(pair.getKey(), pair.getValue()));
            LogWrapper.v(TAG, pair.getKey() + " = " + pair.getValue());
        }

        String responseText = null;
        try {

            post.setEntity(new UrlEncodedFormEntity(pairs));
            HttpResponse response = client.execute(post);
            HttpEntity entity = response.getEntity();

            // http://hc.apache.org/httpclient-3.x/tutorial.html
            // It is vital that the response body is always read regardless of
            // the status returned by the server.
            responseText = getResponseBody(entity);

            LogWrapper.v(TAG, "Success url:" + post.getURI());
            LogWrapper.v(TAG, "Success url:" + pairs);
            return responseText;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            LogWrapper.w(TAG, "Failed:" + e);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            LogWrapper.w(TAG, "Failed:" + e);
        } catch (IOException e) {
            e.printStackTrace();
            LogWrapper.w(TAG, "Failed:" + e);
        } catch (ParseException e) {
            e.printStackTrace();
            LogWrapper.w(TAG, "Failed:" + e);
        }
        LogWrapper.e(TAG, "Got response " + responseText);
        return responseText;
    }

    /**
     * Method used for uploading a file to the default upload server
     * 
     * 
     * @param pathName
     *            The path to use taking the apps context into account
     * @param fileName
     *            The file name to append to
     */
    protected static boolean uploadFile(String pathName, String fileName) {
        LogWrapper.v(TAG, "uploadFile(String pathName, String fileName)");
        return uploadFile(DEFAULT_UPLOAD_SERVER, pathName, fileName);
    }

    /**
     * Method used for uploading a file to a server;
     * 
     * @param uploadServer
     *            The server to upload the file to
     * @param internalPathName
     *            The path to use taking the apps context into account
     * @param fileName
     *            The file name to append to
     */            
    @SuppressWarnings("deprecation")
	private static boolean uploadFile(String uploadServer, String internalPathName, String fileName) {
     
        LogWrapper.v(TAG, "uploadFile(uploadServer, pathName, fileName)");

        String randomFileName = UUID.randomUUID() + ".gz";

        try {
            // ------------------ CLIENT REQUEST
            FileInputStream fileInputStream = new FileInputStream(new File(internalPathName + fileName));

            // Open a URL connection to the Servlet
            URL url = new URL(uploadServer);

            // Open a HTTP connection to the URLs
            conn = (HttpURLConnection) url.openConnection();

            // Allow Inputs
            conn.setDoInput(true);

            // Allow Outputs
            conn.setDoOutput(true);

            // Don't use a cached copy.
            conn.setUseCaches(false);

            // Use a post method.
            conn.setRequestMethod("POST");

            conn.setRequestProperty("Connection", "Keep-Alive");

            conn.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);

            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"upload\";"
                    + " filename=\"" + randomFileName + "\"" + lineEnd);
            dos.writeBytes(lineEnd);

            // create a buffer of maximum size
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            while ( (bytesRead = fileInputStream.read(buffer, 0, bufferSize)) > 0) {
                dos.write(buffer, 0, bytesRead);
            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // close streams
            fileInputStream.close();
            dos.flush();
            dos.close();

        } catch (MalformedURLException ex) {
            LogWrapper.w(TAG, "From ServletCom CLIENT REQUEST: " + ex);
            return false;
        } catch (IOException ioe) {
            LogWrapper.w(TAG, "From ServletCom CLIENT REQUEST: " + ioe);
            return false;
        }

        // ------------------ read the SERVER RESPONSE
        try {
            inStream = new DataInputStream(conn.getInputStream());
            String str;
            while ((str = inStream.readLine()) != null) {
                LogWrapper.v(TAG, "Server response: " + str);
            }
            inStream.close();
            return true;

        } catch (IOException ioex) {
            LogWrapper.w(TAG, "Server response: " + ioex);
            return false;
        }

    }

    private OTSend() {
    }

}
