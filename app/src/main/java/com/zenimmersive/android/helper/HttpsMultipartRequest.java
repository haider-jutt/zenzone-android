package com.zenimmersive.android.helper;

import com.zenimmersive.android.BuildConfig;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;


/*
 * Volley and Retrofit They manage all network related task on separate thread.
 * So i have created this class for managing operations over my thread.
 * */

public class HttpsMultipartRequest {
    public static String multipartRequest(String urlTo, Map<String, String> parmas, String filepath, String filefield, String fileMimeType, String className) throws Exception {
        try {
            LogManager.getLogManager().writeLog(className + " URL : " + urlTo);
        } catch (Exception ignore) {
        }
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        String twoHyphens = "--";
        String boundary = "*****" + System.currentTimeMillis() + "*****";
        String lineEnd = "\r\n";

        String result = "";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        String[] q = filepath.split("/");
        int idx = q.length - 1;


        File file = new File(filepath);
        FileInputStream fileInputStream = new FileInputStream(file);

        URL url = new URL(urlTo);
        connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(60 * 1000 * 10);
        connection.setReadTimeout(60 * 1000 * 10);

        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + q[idx] + "\"" + lineEnd);
        outputStream.writeBytes("Content-Type: " + fileMimeType + lineEnd);
        outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);

        outputStream.writeBytes(lineEnd);

        bytesAvailable = fileInputStream.available();
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];

        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        while (bytesRead > 0) {
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        outputStream.writeBytes(lineEnd);

        // Upload POST Data
        Iterator<String> keys = parmas.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = parmas.get(key);

            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
            outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(value);
            outputStream.writeBytes(lineEnd);
        }

        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


        if (200 != connection.getResponseCode()) {
            LogSystem.e("ApiService", "-------------------------------------");
            LogSystem.e("ApiService", "Exe. Request : " + urlTo);
            LogSystem.e("ApiService", "Exe. Method Post : " + "POST");
            LogSystem.e("ApiService", "File : " + filepath);
            LogSystem.e("ApiService", "Caller Class : " + className);
            LogSystem.e("ApiService", "Exe. Response : " + result);
            LogSystem.e("ApiService", "-------------------------------------");
            throw new RuntimeException("Failed to upload code:" + connection.getResponseCode() + " " + connection.getResponseMessage());
        }

        inputStream = connection.getInputStream();

        result = convertStreamToString(inputStream);

        try {
            LogManager.getLogManager().writeLog(className + " URL : " + urlTo + "\nResult : " + result);
        } catch (Exception ignore) {
        }
        if (BuildConfig.DEBUG) {
            LogSystem.e("ApiService", "-------------------------------------");
            LogSystem.e("ApiService", "Request : " + urlTo);
            LogSystem.e("ApiService", "Method Post : " + "POST");
            LogSystem.e("ApiService", "File : " + filepath);
            LogSystem.e("ApiService", "Caller Class : " + className);
            LogSystem.e("ApiService", "Response : " + result);
            LogSystem.e("ApiService", "-------------------------------------");
        }

        fileInputStream.close();
        inputStream.close();
        outputStream.flush();
        outputStream.close();

        return result;
    }

    public static String request(String urlTo, String method, String className) throws Exception {
        try {
            LogManager.getLogManager().writeLog(className + " URL : " + urlTo);
        } catch (Exception ignore) {
        }
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        URL url = new URL(urlTo);
        connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(60 * 1000 * 10);
        connection.setReadTimeout(60 * 1000 * 10);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);

        connection.setRequestMethod(method);

        inputStream = connection.getInputStream();

        String result = convertStreamToString(inputStream);

        if (BuildConfig.DEBUG) {
            LogSystem.e("ApiService", "-------------------------------------");
            LogSystem.e("ApiService", "Request : " + urlTo);
            LogSystem.e("ApiService", "Method Post : " + "POST");
            LogSystem.e("ApiService", "Response : " + result);
            LogSystem.e("ApiService", "Caller Class : " + className);
            LogSystem.e("ApiService", "-------------------------------------");
        }

        inputStream.close();

        return result;
    }

    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw e;
            }
        }
        return sb.toString();
    }
}
