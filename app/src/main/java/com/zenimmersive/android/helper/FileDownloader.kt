package com.zenimmersive.android.helper

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileDownloader {

    interface DownloadListener {
        fun onDownloadSuccess(filePath: String)
        fun onDownloadError(errorMessage: String)
        fun onDownloadProgress(progress: Int, downloadedSize: Long, totalSize: Long)
    }


    companion object {
        private val client = OkHttpClient()

        @JvmStatic
        fun downloadFile(url: String, outputDir: String, listener: DownloadListener? = null) {
            LogSystem.e("TAG", "downloadFile $url")
            var mainHandler: Handler = Handler(Looper.getMainLooper())

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    // Handle the error
                    mainHandler.post { listener?.onDownloadError(e.message ?: "Unknown error") }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.body?.let { body ->
                        try {
                            // Determine the file extension from the Content-Type header
                            val contentType = response.header("Content-Type")
                            LogSystem.e("TAG", "File Content-Type $contentType")
                            val extension = when (contentType) {
                                "video/mp4" -> ".mp4"
                                "video/avi" -> ".avi"
                                "video/mpeg" -> ".mpeg"
                                "video/quicktime" -> ".mov"
                                "video/annodex" -> ".axv"
                                "video/dl" -> ".dl"
                                "video/dv" -> ".dv"
                                "video/fli" -> ".fli"
                                "video/gl" -> ".gl"
                                "video/MP2T" -> ".ts"
                                "video/webm" -> ".webm"
                                "video/x-la-asf" -> ".lsf"
                                "video/x-la-asf" -> ".lsx"
                                "video/x-mng" -> ".mng"
                                "video/x-matroska" -> ".mpv"
                                "video/x-matroska" -> ".mkv"
                                "audio/aac" -> ".aac"
                                "audio/flac" -> ".flac"
                                "audio/midi" -> ".midi"
                                "audio/x-midi" -> ".mid"
                                "audio/mpeg" -> ".mp3"
                                "audio/ogg" -> ".ogg"
                                "audio/opus" -> ".opus"
                                "audio/wav" -> ".wav"
                                "audio/webm" -> ".webm"
                                "audio/3gpp" -> ".3gp"
                                "audio/3gpp2" -> ".3g2"
                                "audio/aiff" -> ".aiff"
                                "audio/basic" -> ".au"
                                "audio/it" -> ".it"
                                "audio/m4a" -> ".m4a"
                                "audio/mod" -> ".mod"
                                "audio/s3m" -> ".s3m"
                                "audio/xm" -> ".xm"
                                "audio/vox" -> ".vox"
                                "audio/x-realaudio" -> ".ra"
                                "audio/x-pn-realaudio" -> ".ram"
                                "audio/x-pn-realaudio-plugin" -> ".rpm"
                                "audio/vnd.rn-realaudio" -> ".rm"
                                else -> "-NA"
                            }

                            // Determine the file name from the Content-Disposition header
                            val contentDisposition = response.header("Content-Disposition")
                            val fileName =
                                if (contentDisposition != null && contentDisposition.contains("filename=")) {
                                    contentDisposition.split("filename=")[1].trim('"')
                                } else {
                                    "${System.currentTimeMillis()}$extension"
                                }

                            // Create the file
                            val file = File(outputDir, fileName)
                            val fileOutputStream = FileOutputStream(file)

                            val buffer = ByteArray(2048)
                            var bytesRead: Int
                            val totalSize = body.contentLength()
                            var downloadedSize: Long = 0
                            val updateThreshold = totalSize / 100 // Adjust this value as needed
                            var lastUpdateSize = 0L

                            val inputStream = body.byteStream()
                            inputStream.use { input ->
                                fileOutputStream.use { output ->
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        output.write(buffer, 0, bytesRead)
                                        downloadedSize += bytesRead
                                        if (downloadedSize - lastUpdateSize >= updateThreshold) {
                                            val progress =
                                                (downloadedSize * 100 / totalSize).toInt()
                                            mainHandler.post {
                                                listener?.onDownloadProgress(
                                                    progress,
                                                    downloadedSize,
                                                    totalSize
                                                )
                                            }
                                            lastUpdateSize =
                                                downloadedSize // Update the last posted size
                                        }
                                    }
                                }
                            }

                            // Notify success
                            mainHandler.post { listener?.onDownloadSuccess(file.absolutePath) }
                        } catch (e: Exception) {
                            // Handle the exception
                            mainHandler.post {
                                listener?.onDownloadError(
                                    e.message ?: "Error saving file"
                                )
                            }
                        }
                    } ?: run {
                        // Handle case where response body is null
                        mainHandler.post { listener?.onDownloadError("Response body is null") }
                    }
                }
            })
        }
    }
}
