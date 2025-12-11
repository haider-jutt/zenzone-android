package com.zenimmersive.android.helper

import android.content.Context
import android.os.StatFs
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.zenimmersive.android.apiresponsemodel.AlbumMusic
import com.zenimmersive.android.helper.KeyStorage.Companion.APP_SELECTED_LANGUAGE
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.HashMap

/**
 * Unified Music Pack Downloader
 * Downloads all necessary files for a music pack including:
 * - Audio files (EN, FR)
 * - Narrator audio files (EN, FR)
 * - Video files (EN, FR)
 * - Lyric files (EN, FR)
 * - Hue color files (EN, FR)
 * - Background images (EN, FR)
 * 
 * Memory efficient design:
 * - Single thread executor for sequential downloads
 * - Limits to max 10 concurrent downloads
 * - Uses HashMap to track download state
 * - Images downloaded first (most important)
 * - Cache-first strategy to handle app closure gracefully
 */
object MusicPackDownloader {

    // Track download progress to avoid duplicate downloads
    private val hasMap = HashMap<String, Boolean>()
    // Single thread executor to prevent resource overload on low-end devices
    private val singleThreadExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    private val musicPackStatusHashMap = HashMap<String, Boolean>()
    
    /**
     * Download complete music pack with all language files
     */
    fun downloadMusicPack(
        context: Context,
        musicPack: AlbumMusic,
        listener: DownloadListener? = null
    ) {
        val musicPackId = musicPack.songId ?: return
        val musicPackName = musicPack.songName

        if(musicPackStatusHashMap.containsKey(musicPackId.toString())) return
        musicPackStatusHashMap.put(musicPackId.toString(), true)

        
        LogSystem.e("MusicPackDownloader", "Starting download for: $musicPackName ($musicPackId)")
        
        // Preload images using Glide (most important, do first)
        preloadImages(context, musicPack)
        
        singleThreadExecutor.submit {
            try {
                val filesToDownload = collectAllFileUrls(musicPack)
                var downloadedCount = 0
                val totalFiles = filesToDownload.size
                
                LogSystem.e("MusicPackDownloader", "Total files to download: $totalFiles")
                
                filesToDownload.forEach { (url, fileType) ->
                    if (downloadFile(context, url, musicPack, fileType)) {
                        downloadedCount++
                        listener?.onProgress(musicPackId, downloadedCount, totalFiles)
                    }
                }
                
                listener?.onComplete(musicPackId, musicPackName)
                LogSystem.e("MusicPackDownloader", "Completed download for: $musicPackName ($musicPackId)")
            } catch (e: Exception) {
                e.printStackTrace()
                listener?.onError(musicPackId, e.message ?: "Unknown error")
                LogSystem.e("MusicPackDownloader", "Error downloading: $musicPackName - ${e.message}")
            }
        }
    }
    
    /**
     * Download specific music packs in sequence for next songs
     * Downloads current playing first, then remaining in order
     */
    fun setupDownloadQueue(
        context: Context,
        musicPack: AlbumMusic?,
        directMusicList: ArrayList<AlbumMusic>,
        selectMusicIndex: Int
    ) {
        val musicPackIndex = if (selectMusicIndex >= directMusicList.size) 0 else selectMusicIndex
        val size = directMusicList.size

        directMusicList?.get(musicPackIndex)?.let {
            LogSystem.e("MusicPackDownloader", "Queue Index: $musicPackIndex")
            downloadMusicPack(context, it)
        }
        // Previous Index
        var i = musicPackIndex - 1
        if (i < 0) i = size - 1
        directMusicList?.get(i)?.let {
            LogSystem.e("MusicPackDownloader", "Queue Index: $i")
            downloadMusicPack(context, it)
        }

        //Next Index
        i = (musicPackIndex + 1)
        if (i >= size) i = 0
        directMusicList?.get(i)?.let {
            LogSystem.e("MusicPackDownloader", "Queue Index: $i")
            downloadMusicPack(context, it)
        }
        
        // Build circular download list starting from current index
        for (i in 1..size) {
            val index = ((musicPackIndex + i - 1) % size)
            LogSystem.e("MusicPackDownloader", "Queue Index: $index")
            downloadMusicPack(context, (directMusicList[index]))
        }
    }
    
    /**
     * Collect all file URLs for both languages
     */
    private fun collectAllFileUrls(musicPack: AlbumMusic): List<Pair<String, String>> {
        val files = mutableListOf<Pair<String, String>>()

        // Lyric files (EN, FR)
        musicPack.lyrics?.let { if (it.isNotEmpty()) files.add(Pair(it, "lyric_en")) }
        musicPack.lyricsFrench?.let { if (it.isNotEmpty()) files.add(Pair(it, "lyric_fr")) }

        // Hue color files (EN, FR)
        musicPack.hueColorTime?.let { if (it.isNotEmpty() && !it.endsWith("default-user.jpg")) files.add(Pair(it, "hue_en")) }
        musicPack.hueColorTimeFrench?.let { if (it.isNotEmpty() && !it.endsWith("default-user.jpg")) files.add(Pair(it, "hue_fr")) }

        // Audio files (EN, FR)
        musicPack.audioFileMusic?.let { if (it.isNotEmpty()) files.add(Pair(it, "audio_en")) }
        musicPack.audioFileMusicFrench?.let { if (it.isNotEmpty()) files.add(Pair(it, "audio_fr")) }
        
        // Narrator audio files (EN, FR)
        musicPack.audioFileNarrator?.let { if (it.isNotEmpty()) files.add(Pair(it, "narrator_audio_en")) }
        musicPack.audioFileNarratorFrench?.let { if (it.isNotEmpty()) files.add(Pair(it, "narrator_audio_fr")) }
        
        // Video files (EN, FR)
        musicPack.videoFileStream?.let { if (it.isNotEmpty()) files.add(Pair(it, "video_en")) }
        musicPack.videoFileStreamFrench?.let { if (it.isNotEmpty()) files.add(Pair(it, "video_fr")) }
        
        // Narrator video files (EN, FR)
        musicPack.narratorMusicVideoFile?.let { if (it.isNotEmpty()) files.add(Pair(it, "narrator_video_en")) }
        musicPack.narratorMusicVideoFileFrench?.let { if (it.isNotEmpty()) files.add(Pair(it, "narrator_video_fr")) }
        
        // Guided files (EN, FR) - for future use
        musicPack.guidedFile?.let { if (it.isNotEmpty()) files.add(Pair(it, "guided_en")) }
        musicPack.guidedFileFrench?.let { if (it.isNotEmpty()) files.add(Pair(it, "guided_fr")) }
        
        return files
    }
    
    /**
     * Preload images using Glide for automatic caching
     */
    private fun preloadImages(context: Context, musicPack: AlbumMusic) {
        // Preload background images
        musicPack.getBackgroundVertical("en")?.let { enUrl ->
            if (enUrl.isNotEmpty()) {
                preloadImageWithGlide(context, enUrl)
            }
        }
        
        musicPack.getBackgroundVertical("fr")?.let { frUrl ->
            if (frUrl.isNotEmpty()) {
                preloadImageWithGlide(context, frUrl)
            }
        }
    }
    
    /**
     * Preload single image using Glide with a dummy target
     */
    private fun preloadImageWithGlide(context: Context, url: String) {
        try {
            Glide.with(context.applicationContext)
                .load(url)
                .into(object : CustomTarget<android.graphics.drawable.Drawable>() {
                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        transition: Transition<in android.graphics.drawable.Drawable>?
                    ) {
                        // Image cached successfully
                        LogSystem.e("MusicPackDownloader", "Image cached: $url")
                    }
                    
                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        // Cleanup if needed
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Download a single file
     * Uses cache-first strategy to handle app closure gracefully
     */
    private fun downloadFile(
        context: Context,
        url: String,
        musicPack: AlbumMusic,
        fileType: String
    ): Boolean {
        // Limit concurrent downloads to avoid resource exhaustion
        var downloadProgress = 0
        hasMap.values.forEach { value ->
            if (value) downloadProgress++
        }

        if (downloadProgress >= 3) {
            LogSystem.e("MusicPackDownloader", "Max concurrent downloads reached: $url")
            return false
        }
        
        // Check if already downloading or downloaded
        if (hasMap.containsKey(url)) {
            LogSystem.e("MusicPackDownloader", "Already in queue: $url")
            return false
        }
        
        hasMap[url] = false
        
        try {
            LogSystem.e("MusicPackDownloader", "Downloading: $fileType - $url\nMusicPack: ${musicPack.songName} (${musicPack.songId})")
            
            val folder = File(context.filesDir, "${Configrations.MusicPackFolderName}/${musicPack.songId}")
            if (!folder.exists()) folder.mkdirs()
            
            val fileName = CommonUtils.extractFileName(url) ?: "default_file"
            val targetFile = File(folder, fileName)
            
            // Skip if file already exists
            if (targetFile.exists()) {
                hasMap[url] = true
                LogSystem.e("MusicPackDownloader", "File already exists: $url")
                return true
            }
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val contentLength = connection.contentLength
                
                // Check available space
                val stat = StatFs(context.filesDir.path)
                val cacheDirectory = StatFs(context.cacheDir.path)
                
                LogSystem.e(
                    "MusicPackDownloader",
                    "File Space Available: ${FileHelper.fileSizeToHumanReadable(stat.availableBytes)}"
                )
                LogSystem.e(
                    "MusicPackDownloader",
                    "Cache Space Available: ${FileHelper.fileSizeToHumanReadable(cacheDirectory.availableBytes)}"
                )
                
                if (stat.availableBytes > contentLength && cacheDirectory.availableBytes > contentLength) {
                    // Delete cache file if exists (in case of incomplete previous download)
                    val cacheFile = File(context.cacheDir, fileName)
                    if (cacheFile.exists()) cacheFile.delete()
                    
                    // Download to cache first (safe for app closure)
                    cacheFile.outputStream().use { output ->
                        connection.inputStream.use { input ->
                            input.copyTo(output)
                        }
                    }

                    val downloadedSize = cacheFile.length()
                    if ((downloadedSize == contentLength.toLong()) || isTextFile(contentLength, targetFile)) {
                        cacheFile.renameTo(targetFile)
                        hasMap[url] = true
                        LogSystem.e("MusicPackDownloader", "Download verified: $url")
                        return true
                    } else {
                        LogSystem.e("MusicPackDownloader", "File incomplete ($downloadedSize/$contentLength): $url")
                        cacheFile.delete()
                        hasMap.remove(url)
                        return false
                    }
                } else {
                    LogSystem.e("MusicPackDownloader", "Insufficient space for: $url")
                    connection.disconnect()
                    hasMap.remove(url)
                    return false
                }
            } else {
                LogSystem.e("MusicPackDownloader", "HTTP Error ${connection.responseCode} for: $url")
            }
            
            connection.disconnect()
            hasMap.remove(url)
            return false
            
        } catch (e: Exception) {
            e.printStackTrace()
            LogSystem.e("MusicPackDownloader", "Error downloading $url: ${e.message}")
            hasMap.remove(url)
            return false
        } catch (e: Error) {
            e.printStackTrace()
            LogSystem.e("MusicPackDownloader", "Error downloading $url: ${e.message}")
            hasMap.remove(url)
            return false
        }
    }

    private fun isTextFile(contentLength: Int, targetFile: File): Boolean {
        // If File is Text file or HTML file
        if(targetFile.name.lowercase().endsWith(".txt") || targetFile.name.lowercase().endsWith(".html") || targetFile.name.lowercase().endsWith(".htm"))
        {
            if(contentLength == -1) return true
        }
        return false;
    }

    /**
     * Check if a file exists locally
     */
    fun findLocalFile(
        context: Context,
        musicPack: AlbumMusic?,
        url: String
    ): String {
        musicPack?.let { pack ->
            val fileName = CommonUtils.extractFileName(url) ?: ""
            if (fileName.isNotEmpty()) {
                val file = File(
                    context.filesDir,
                    "${Configrations.MusicPackFolderName}/${pack.songId}/$fileName"
                )
                if (file.exists()) {
                    return file.absolutePath
                }
            }
        }
        return url
    }
    
    /**
     * Find local video file URL
     */
    fun findLocalVideoFileURL(
        context: Context,
        musicPack: AlbumMusic?,
        isNarrator: Boolean
    ): String {
        musicPack?.let { pack ->
            val lan = KeyStorage.getInstance(context).getString(APP_SELECTED_LANGUAGE, "en")
            val streamFileURL = pack.getStreamFile(lan, isNarrator)
            if (streamFileURL.isNotEmpty()) {
                return findLocalFile(context, pack, streamFileURL)
            }
        }
        return ""
    }
    
    /**
     * Find local file by URL
     */
    fun findFileByURL(
        context: Context,
        musicPack: AlbumMusic,
        url: String
    ): String {
        return findLocalFile(context, musicPack, url)
    }
    
    /**
     * Download listener interface
     */
    interface DownloadListener {
        fun onProgress(packId: Int, downloaded: Int, total: Int) {}
        fun onComplete(packId: Int, packName: String?) {}
        fun onError(packId: Int, error: String?) {}
    }
}


