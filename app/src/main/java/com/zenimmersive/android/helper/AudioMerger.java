package com.zenimmersive.android.helper;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioMerger {

    public static void mergeAudioFiles(String inputFile1, String inputFile2, String outputFile) throws IOException {
        MediaExtractor extractor1 = new MediaExtractor();
        MediaExtractor extractor2 = new MediaExtractor();
        extractor1.setDataSource(inputFile1);
        extractor2.setDataSource(inputFile2);

        MediaFormat format1 = extractor1.getTrackFormat(0);
        MediaFormat format2 = extractor2.getTrackFormat(0);

        int sampleRate = format1.getInteger(MediaFormat.KEY_SAMPLE_RATE);  // Choose a common sample rate
        int channelCount = format1.getInteger(MediaFormat.KEY_CHANNEL_COUNT);    // Choose a common channel count (stereo)

        MediaFormat outputFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount);
        outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, format1.getInteger(MediaFormat.KEY_BIT_RATE));

        MediaMuxer muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int audioTrackIndex = muxer.addTrack(outputFormat);
        muxer.start();

        extractor1.selectTrack(0);
        extractor2.selectTrack(0);

        decodeAndWriteToMuxer(extractor1, muxer, audioTrackIndex, sampleRate, channelCount);
        decodeAndWriteToMuxer(extractor2, muxer, audioTrackIndex, sampleRate, channelCount);

        muxer.stop();
        muxer.release();
        extractor1.release();
        extractor2.release();
    }

    private static void decodeAndWriteToMuxer(MediaExtractor extractor, MediaMuxer muxer, int trackIndex, int sampleRate, int channelCount) throws IOException {
        MediaFormat inputFormat = extractor.getTrackFormat(0);
        String mime = inputFormat.getString(MediaFormat.KEY_MIME);

        MediaCodec decoder = MediaCodec.createDecoderByType(mime);
        decoder.configure(inputFormat, null, null, 0);
        decoder.start();

        MediaCodec encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount);
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        encoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        boolean inputDone = false;
        boolean decodeDone = false;
        boolean encodeDone = false;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();

        while (!encodeDone) {
            if (!inputDone) {
                int inputBufferIndex = decoder.dequeueInputBuffer(10000);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = decoderInputBuffers[inputBufferIndex];
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }

            boolean decoderOutputAvailable = !decodeDone;
            boolean encoderOutputAvailable = true;

            while (decoderOutputAvailable || encoderOutputAvailable) {
                int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000);
                if (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = decoder.getOutputBuffer(outputBufferIndex);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        decodeDone = true;
                    }
                    if (bufferInfo.size != 0) {
                        int inputBufferIndex = encoder.dequeueInputBuffer(10000);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = encoderInputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(outputBuffer);
                            encoder.queueInputBuffer(inputBufferIndex, 0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
                        }
                    }
                    decoder.releaseOutputBuffer(outputBufferIndex, false);
                    decoderOutputAvailable = true;
                } else {
                    decoderOutputAvailable = false;
                }

                int encoderOutputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000);
                if (encoderOutputBufferIndex >= 0) {
                    ByteBuffer encodedData = encoder.getOutputBuffer(encoderOutputBufferIndex);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                    }
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        encodeDone = true;
                    }
                    encoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                    encoderOutputAvailable = true;
                } else {
                    encoderOutputAvailable = false;
                }
            }
        }

        decoder.stop();
        decoder.release();
        encoder.stop();
        encoder.release();
    }
}
