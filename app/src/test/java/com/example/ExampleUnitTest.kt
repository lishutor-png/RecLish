package com.example

import com.example.audio.AudioFormatHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.math.PI
import kotlin.math.sin

class ExampleUnitTest {

  private fun createStereoSinePcm(file: File, sampleRate: Int = 44100, durationSeconds: Double = 0.5) {
    val totalFrames = (sampleRate * durationSeconds).toInt()
    val bytes = ByteArray(totalFrames * 4)
    for (i in 0 until totalFrames) {
      val sample = (sin(2.0 * PI * 440.0 * i / sampleRate) * 16000.0).toInt().toShort()
      val lo = (sample.toInt() and 0xFF).toByte()
      val hi = ((sample.toInt() shr 8) and 0xFF).toByte()
      val offset = i * 4
      bytes[offset] = lo
      bytes[offset + 1] = hi
      bytes[offset + 2] = lo
      bytes[offset + 3] = hi
    }
    FileOutputStream(file).use { it.write(bytes) }
  }

  @Test
  fun encodePcmToMp3_producesValidMpegFrames_notWav() {
    val tempDir = Files.createTempDirectory("mp3test").toFile()
    try {
      val pcmFile = File(tempDir, "test.pcm")
      val mp3File = File(tempDir, "test.mp3")
      createStereoSinePcm(pcmFile, sampleRate = 44100, durationSeconds = 0.5)

      AudioFormatHelper.encodePcmToMp3(pcmFile, mp3File, sampleRate = 44100, channels = 2)
      AudioFormatHelper.attachMp3Metadata(mp3File, "TestMP3")

      assertTrue("MP3 file must exist and be non-empty", mp3File.exists() && mp3File.length() > 1000L)
      assertTrue("MP3 must be significantly smaller than raw PCM", mp3File.length() < pcmFile.length() / 4)

      val data = mp3File.readBytes()
      val headerText = String(data.copyOfRange(0, 4), Charsets.US_ASCII)
      assertFalse("MP3 file must NOT have RIFF WAV header", headerText == "RIFF")

      // MPEG Audio Frame Sync: 11 bits set (0xFF followed by 0xE0 mask)
      val firstByte = data[0].toInt() and 0xFF
      val secondByte = data[1].toInt() and 0xFF
      assertEquals("First byte must be MPEG frame sync 0xFF", 0xFF, firstByte)
      assertEquals("Second byte must have MPEG sync bits 0xE0", 0xE0, secondByte and 0xE0)

      // ID3v1 tag at the end (last 128 bytes start with 'TAG')
      val id3TagHeader = String(data.copyOfRange(data.size - 128, data.size - 125), Charsets.US_ASCII)
      assertEquals("TAG", id3TagHeader)
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun writeWavHeader_producesValidRiffWaveFile() {
    val tempDir = Files.createTempDirectory("wavtest").toFile()
    try {
      val pcmFile = File(tempDir, "test.pcm")
      val wavFile = File(tempDir, "test.wav")
      createStereoSinePcm(pcmFile, sampleRate = 44100, durationSeconds = 0.25)

      AudioFormatHelper.writeWavHeader(pcmFile, wavFile, sampleRate = 44100, channels = 2, bitsPerSample = 16)

      assertEquals(pcmFile.length() + 44L, wavFile.length())
      val header = wavFile.inputStream().use { inp ->
        ByteArray(12).also { inp.read(it) }
      }
      assertEquals("RIFF", String(header.copyOfRange(0, 4), Charsets.US_ASCII))
      assertEquals("WAVE", String(header.copyOfRange(8, 12), Charsets.US_ASCII))
    } finally {
      tempDir.deleteRecursively()
    }
  }
}
