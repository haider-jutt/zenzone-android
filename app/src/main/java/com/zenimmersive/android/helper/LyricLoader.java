package com.zenimmersive.android.helper;

import static com.zenimmersive.android.helper.CustomLyricView.LyricLine;

import android.content.Context;
import android.text.TextUtils;

import com.zenimmersive.android.apiresponsemodel.AlbumMusic;
import com.zenimmersive.android.helper.MusicPackDownloader;
import com.zenimmersive.android.ui.player.PlayerManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricLoader {

    private static final String TAG = "LyricLoader";

    public static void loadLyricsFromUrl(
            final String urlString,
            CustomLyricView customLyricView,
            Context context,
            AlbumMusic albumMusic
    ) {
        BackgroundTask.execute("TAG", new BackgroundTask.Listener<List<LyricLine>>() {
            @Override
            public List<LyricLine> doWork() {
                ArrayList<LyricLine> lyricLines = new ArrayList<>();
                File folder = new File(context.getFilesDir(), Configrations.INSTANCE.getMusicPackFolderName()+"/" + albumMusic.getSongId());
                if (!folder.exists() && !folder.mkdirs()) {
                    LogSystem.e("TAG", "Failed to create folder: " + folder.getAbsolutePath());
                    return lyricLines;
                }

                String fileName = CommonUtils.extractFileName(urlString);
                if (TextUtils.isEmpty(fileName))
                    fileName = "lyricFile_" + System.currentTimeMillis();

                File targetFile = new File(folder, fileName);
                // Check if MusicPackDownloader has the file cached
                String localFilePath = com.zenimmersive.android.helper.MusicPackDownloader.INSTANCE.findLocalFile(context, albumMusic, urlString);
                File cachedFile = new File(localFilePath);
                
                // If file exists locally (downloaded by MusicPackDownloader), use it
                if (cachedFile.exists() && !localFilePath.equals(urlString)) {
                    targetFile = cachedFile;
                }
                
                File cacheFile = new File(folder, "cache_" + fileName);
                LogSystem.e("TAG", "Using folder: " + folder.getAbsolutePath());

                try {
                    if (targetFile.exists()) {
                        LogSystem.e("TAG", "Reading From Local File: " + targetFile.getAbsolutePath());
                        try (BufferedReader reader = new BufferedReader(new FileReader(targetFile))) {
                            readStream(reader, lyricLines);
                        }
                    } else {
                        LogSystem.e("TAG", "Reading From URL : " + urlString);
                        URL url = new URL(urlString);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(10000);
                        connection.setReadTimeout(10000);

                        try (
                                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile))
                        ) {
                            //Copy Reader Bytes to Writter
                            char[] buffer = new char[1024];
                            int bytesRead;
                            while ((bytesRead = reader.read(buffer)) != -1) {
                                writer.write(buffer, 0, bytesRead);
                            }

                            writer.flush();
                        }

                        try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
                            readStream(reader, lyricLines);
                        }

                        if (targetFile.exists()) targetFile.delete();
                        cacheFile.renameTo(targetFile);

                        connection.disconnect();
                    }
                } catch (Exception e) {
                    LogSystem.e("TAG", "Error loading lyrics: " + e.getMessage(), e);
                }

                return lyricLines;
            }

            @Override
            public void onTaskDone(List<LyricLine> result) {
                if(PlayerManager.Companion.getInstance()!=null) PlayerManager.Companion.getInstance().setLyricLines(result != null ? result : new ArrayList<>());
                customLyricView.setLyricLines(result != null ? result : new ArrayList<>());
            }
        });
    }

    private static void readStream(BufferedReader reader, ArrayList<LyricLine> lyricLines) throws Exception {
        String line;


        LogSystem.e(TAG, "Parsing file");


        // Check if it's a VTT file by reading the first line
        String firstLine = reader.readLine();
        if (firstLine != null && firstLine.contains("WEBVTT")) {
            List<LyricLine> tempLines = parseVttFile(reader);
            lyricLines.addAll(tempLines);
        } else {
            // Reset reader if it's not a VTT file
            {
                LyricLine lyricLine = parseLyricLine(firstLine);
                if (lyricLine != null) {
                    lyricLines.add(lyricLine);
                }
            }
            while ((line = reader.readLine()) != null) {
                LyricLine lyricLine = parseLyricLine(line);
                if (lyricLine != null) {
                    lyricLines.add(lyricLine);
                }
            }
        }


    }

    private static LyricLine parseLyricLine(String line) {
        try {
            if (line.startsWith("[")) {
                int endIndex = line.indexOf("]");
                if (endIndex > 0) {
                    String timeString = line.substring(1, endIndex);
                    String text = line.substring(endIndex + 1).trim();
                    String[] minSec = timeString.split(":");
                    long minutes = Long.parseLong(minSec[0]);
                    float seconds = Float.parseFloat(minSec[1]);
                    long timestamp = (minutes * 60 + (long) seconds) * 1000;
                    return new LyricLine(timestamp, text);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<LyricLine> parseVttFile(BufferedReader reader) {
        List<LyricLine> lyricLines = new ArrayList<>();
        String line;
        try {
            // Updated pattern to support optional hours (hh:)?mm:ss.SSS format
            Pattern timePattern = Pattern.compile("(\\d{2}):?(\\d{2}):(\\d{2}\\.\\d{3}) --> (\\d{2}):?(\\d{2}):(\\d{2}\\.\\d{3})");

            while ((line = reader.readLine()) != null) {
                // Skip empty lines or non-timestamp lines
                if (line.trim().isEmpty() || !timePattern.matcher(line).find()) {
                    continue;
                }

                Matcher matcher = timePattern.matcher(line);
                if (matcher.find()) {
                    // Check if the hours part is present or not
                    String startTime = matcher.group(1) + ":" + matcher.group(2) + ":" + matcher.group(3);
                    long timestamp = parseVttTimestamp(startTime);

                    // Capture the lyrics/text line after the timestamp line
                    StringBuilder text = new StringBuilder();
                    while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
                        text.append(line).append(" ");
                    }

                    // Create a new LyricLine
                    if (text.length() > 0) {
                        lyricLines.add(new LyricLine(timestamp, text.toString().trim()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lyricLines;
    }

    private static long parseVttTimestamp(String timeString) {

        try {
            String[] parts = timeString.split(":");
            long hours = 0;
            long minutes = 0;
            float seconds = 0f;

            if (parts.length == 3) {
                // Format: hh:mm:ss.SSS
                hours = Long.parseLong(parts[0]);
                minutes = Long.parseLong(parts[1]);
                seconds = Float.parseFloat(parts[2]);
            } else if (parts.length == 2) {
                // Format: mm:ss.SSS
                minutes = Long.parseLong(parts[0]);
                seconds = Float.parseFloat(parts[1]);
            } else {
                throw new IllegalArgumentException("Invalid timestamp format: " + timeString);
            }
            long totalMillis = (long) ((hours * 3600 + minutes * 60) * 1000 + (seconds * 1000));
            LogSystem.e(TAG, "parseVttTimestamp Time : " + timeString + " Parsed : " + totalMillis);
            return totalMillis;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

}
