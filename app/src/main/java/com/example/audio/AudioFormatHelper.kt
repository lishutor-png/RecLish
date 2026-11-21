package com.example.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import de.sciss.jump3r.mp3.BitStream
import de.sciss.jump3r.mp3.GainAnalysis
import de.sciss.jump3r.mp3.ID3Tag
import de.sciss.jump3r.mp3.Lame
import de.sciss.jump3r.mp3.MPEGMode
import de.sciss.jump3r.mp3.Presets
import de.sciss.jump3r.mp3.PsyModel
import de.sciss.jump3r.mp3.Quantize
import de.sciss.jump3r.mp3.QuantizePVT
import de.sciss.jump3r.mp3.Reservoir
import de.sciss.jump3r.mp3.Takehiro
import de.sciss.jump3r.mp3.VBRTag
import de.sciss.jump3r.mp3.VbrMode
import de.sciss.jump3r.mp3.Version
import de.sciss.jump3r.mpg.Common
import de.sciss.jump3r.mpg.Interface
import de.sciss.jump3r.mpg.MPGLib
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer

object AudioFormatHelper {

    /**
     * Writes the RIFF/WAVE header and copies 16-bit PCM data into [targetWavFile].
     */
    fun writeWavHeader(
        rawPcmFile: File,
        targetWavFile: File,
        sampleRate: Int = 44100,
        channels: Int = 2,
        bitsPerSample: Int = 16
    ) {
        val pcmDataLength = rawPcmFile.length()
        val totalDataLen = pcmDataLength + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        // RIFF chunk descriptor
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        // WAVE
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // 'fmt ' chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 16 for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // Audio format 1 = PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        // 'data' chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmDataLength and 0xff).toByte()
        header[41] = ((pcmDataLength shr 8) and 0xff).toByte()
        header[42] = ((pcmDataLength shr 16) and 0xff).toByte()
        header[43] = ((pcmDataLength shr 24) and 0xff).toByte()

        FileOutputStream(targetWavFile).use { out ->
            out.write(header)
            rawPcmFile.inputStream().use { inp ->
                inp.copyTo(out)
            }
        }
    }

    /**
     * Encodes raw 16-bit little-endian PCM data into a genuine MPEG-1 Layer III (.mp3) file
     * using pure-Java LAME (jump3r).
     */
    fun encodePcmToMp3(
        rawPcmFile: File,
        targetMp3File: File,
        sampleRate: Int = 44100,
        channels: Int = 2,
        bitrateKbps: Int = 192
    ) {
        val lame = Lame()
        val ga = GainAnalysis()
        val bs = BitStream()
        val presets = Presets()
        val qpvt = QuantizePVT()
        val qu = Quantize()
        val vbr = VBRTag()
        val ver = Version()
        val id3 = ID3Tag()
        val mpg = MPGLib()
        val res = Reservoir()
        val tak = Takehiro()
        val psy = PsyModel()
        val iface = Interface()
        val common = Common()

        lame.setModules(ga, bs, presets, qpvt, qu, vbr, ver, id3, mpg)
        bs.setModules(ga, mpg, ver, vbr)
        presets.setModules(lame)
        qpvt.setModules(tak, res, psy)
        qu.setModules(bs, res, qpvt, tak)
        vbr.setModules(lame, bs, ver)
        id3.setModules(bs, ver)
        res.setModules(bs)
        tak.setModules(qpvt)
        mpg.setModules(iface, common)
        iface.setModules(vbr, common)

        val gfp = lame.lame_init()
        gfp.num_channels = channels.coerceIn(1, 2)
        gfp.in_samplerate = sampleRate
        gfp.out_samplerate = sampleRate
        gfp.brate = bitrateKbps
        gfp.quality = 2
        gfp.VBR = VbrMode.vbr_off
        gfp.bWriteVbrTag = false
        gfp.write_id3tag_automatic = false
        gfp.mode = if (gfp.num_channels == 1) MPEGMode.MONO else MPEGMode.JOINT_STEREO

        val initStatus = lame.lame_init_params(gfp)
        check(initStatus >= 0) { "Gagal menginisialisasi encoder MP3 (kode $initStatus)" }

        val framesPerChunk = 4096
        val bytesPerFrame = gfp.num_channels * 2
        val pcmBuffer = ByteArray(framesPerChunk * bytesPerFrame)
        val leftInts = IntArray(framesPerChunk)
        val rightInts = IntArray(framesPerChunk)
        // LAME worst-case MP3 buffer formula: 1.25 * num_samples + 7200
        val mp3Buffer = ByteArray((1.25 * framesPerChunk).toInt() + 8192)

        try {
            BufferedInputStream(FileInputStream(rawPcmFile)).use { inp ->
                BufferedOutputStream(FileOutputStream(targetMp3File)).use { out ->
                    while (true) {
                        val bytesRead = inp.read(pcmBuffer)
                        if (bytesRead <= 0) break

                        val frameCount = bytesRead / bytesPerFrame
                        if (frameCount <= 0) continue

                        if (gfp.num_channels == 1) {
                            for (i in 0 until frameCount) {
                                val offset = i * 2
                                val low = pcmBuffer[offset].toInt() and 0xFF
                                val high = pcmBuffer[offset + 1].toInt()
                                val sample32 = ((high shl 8) or low).toShort().toInt() shl 16
                                leftInts[i] = sample32
                                rightInts[i] = sample32
                            }
                        } else {
                            for (i in 0 until frameCount) {
                                val offset = i * 4
                                val lLow = pcmBuffer[offset].toInt() and 0xFF
                                val lHigh = pcmBuffer[offset + 1].toInt()
                                leftInts[i] = ((lHigh shl 8) or lLow).toShort().toInt() shl 16

                                val rLow = pcmBuffer[offset + 2].toInt() and 0xFF
                                val rHigh = pcmBuffer[offset + 3].toInt()
                                rightInts[i] = ((rHigh shl 8) or rLow).toShort().toInt() shl 16
                            }
                        }

                        val encodedBytes = lame.lame_encode_buffer_int(
                            gfp,
                            leftInts,
                            rightInts,
                            frameCount,
                            mp3Buffer,
                            0,
                            mp3Buffer.size
                        )
                        if (encodedBytes < 0) {
                            throw IllegalStateException("MP3 encode error: $encodedBytes")
                        }
                        if (encodedBytes > 0) {
                            out.write(mp3Buffer, 0, encodedBytes)
                        }
                    }

                    val flushedBytes = lame.lame_encode_flush(gfp, mp3Buffer, 0, mp3Buffer.size)
                    if (flushedBytes > 0) {
                        out.write(mp3Buffer, 0, flushedBytes)
                    }
                    out.flush()
                }
            }
        } finally {
            lame.lame_close(gfp)
        }
    }

    /**
     * Encodes raw 16-bit little-endian PCM data into an AAC-LC MPEG-4 (.m4a) container
     * using Android's [MediaCodec] and [MediaMuxer].
     */
    fun encodePcmToM4a(
        rawPcmFile: File,
        targetM4aFile: File,
        sampleRate: Int = 44100,
        channels: Int = 2,
        bitrateBps: Int = 192_000
    ) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val muxer = MediaMuxer(targetM4aFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        var trackIndex = -1

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val bytesPerSecond = sampleRate * channels * 2L
            var totalBytesFed = 0L
            var inputDone = false
            var outputDone = false
            val readScratch = ByteArray(4096)

            FileInputStream(rawPcmFile).use { inp ->
                while (!outputDone) {
                    if (!inputDone) {
                        val inputIndex = encoder.dequeueInputBuffer(10_000L)
                        if (inputIndex >= 0) {
                            val inputBuf = encoder.getInputBuffer(inputIndex)
                            inputBuf?.clear()
                            val maxRead = minOf(readScratch.size, inputBuf?.remaining() ?: readScratch.size)
                            val read = inp.read(readScratch, 0, maxRead)
                            if (read < 0) {
                                val ptsUs = (totalBytesFed * 1_000_000L) / bytesPerSecond
                                encoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    ptsUs,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                inputBuf?.put(readScratch, 0, read)
                                val ptsUs = (totalBytesFed * 1_000_000L) / bytesPerSecond
                                totalBytesFed += read
                                encoder.queueInputBuffer(inputIndex, 0, read, ptsUs, 0)
                            }
                        }
                    }

                    val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            check(!muxerStarted) { "Muxer format changed twice" }
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outputIndex >= 0 -> {
                            val encodedBuf = encoder.getOutputBuffer(outputIndex)
                            if (encodedBuf != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0) {
                                if (muxerStarted) {
                                    encodedBuf.position(bufferInfo.offset)
                                    encodedBuf.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(trackIndex, encodedBuf, bufferInfo)
                                }
                            }
                            encoder.releaseOutputBuffer(outputIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputDone = true
                            }
                        }
                    }
                }
            }
        } finally {
            try { encoder.stop() } catch (_: Exception) {}
            try { encoder.release() } catch (_: Exception) {}
            if (muxerStarted) {
                try { muxer.stop() } catch (_: Exception) {}
            }
            try { muxer.release() } catch (_: Exception) {}
        }
    }

    /**
     * Decodes an encoded audio file (e.g. AAC/M4A from [android.media.MediaRecorder]) into raw
     * 16-bit little-endian PCM and returns `(sampleRate, channelCount)`.
     */
    fun decodeAudioToPcm(inputFile: File, targetPcmFile: File): Pair<Int, Int> {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var sampleRate = 44100
        var channels = 1

        try {
            extractor.setDataSource(inputFile.absolutePath)
            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME)
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = fmt
                    break
                }
            }
            check(audioTrackIndex >= 0 && trackFormat != null) { "Track audio tidak ditemukan" }

            extractor.selectTrack(audioTrackIndex)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)!!
            if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            }
            if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                channels = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            }

            decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(trackFormat, null, null, 0)
                start()
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            BufferedOutputStream(FileOutputStream(targetPcmFile)).use { out ->
                while (!outputDone) {
                    if (!inputDone) {
                        val inIdx = decoder.dequeueInputBuffer(10_000L)
                        if (inIdx >= 0) {
                            val inBuf: ByteBuffer = decoder.getInputBuffer(inIdx)!!
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outIdx = decoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outFmt = decoder.outputFormat
                            if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                                sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            }
                            if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                                channels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            }
                        }
                        outIdx >= 0 -> {
                            val outBuf = decoder.getOutputBuffer(outIdx)
                            if (outBuf != null && bufferInfo.size > 0) {
                                val chunk = ByteArray(bufferInfo.size)
                                outBuf.position(bufferInfo.offset)
                                outBuf.get(chunk)
                                out.write(chunk)
                            }
                            decoder.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputDone = true
                            }
                        }
                    }
                }
                out.flush()
            }
        } finally {
            try { decoder?.stop() } catch (_: Exception) {}
            try { decoder?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }

        return sampleRate to channels
    }

    /**
     * Appends an ID3v1 metadata tag (128 bytes) to an MP3 file so players detect title and artist.
     */
    fun attachMp3Metadata(file: File, title: String) {
        if (!file.exists() || file.length() < 10) return
        try {
            val raf = RandomAccessFile(file, "rw")
            raf.seek(raf.length())
            val tag = ByteArray(128)
            // Header 'TAG'
            tag[0] = 'T'.code.toByte()
            tag[1] = 'A'.code.toByte()
            tag[2] = 'G'.code.toByte()
            // Title (30 bytes)
            val titleBytes = title.toByteArray(Charsets.ISO_8859_1)
            System.arraycopy(titleBytes, 0, tag, 3, minOf(30, titleBytes.size))
            // Artist (30 bytes)
            val artist = "RecLish".toByteArray(Charsets.ISO_8859_1)
            System.arraycopy(artist, 0, tag, 33, minOf(30, artist.size))
            // Album (30 bytes)
            val album = "Browser Audio".toByteArray(Charsets.ISO_8859_1)
            System.arraycopy(album, 0, tag, 63, minOf(30, album.size))
            raf.write(tag)
            raf.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
