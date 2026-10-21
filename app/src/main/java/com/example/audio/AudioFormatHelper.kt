package com.example.audio

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

object AudioFormatHelper {

    /**
     * Writes or fixes the WAV header for a raw PCM 16-bit file.
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
     * Writes ID3v2 tag header for MP3 files so players correctly detect title and artist.
     */
    fun attachMp3Metadata(file: File, title: String) {
        if (!file.exists() || file.length() < 10) return
        try {
            // Append basic ID3v1 tag at end of MP3 (128 bytes)
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
