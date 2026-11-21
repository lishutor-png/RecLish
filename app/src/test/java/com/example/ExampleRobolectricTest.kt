package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RecLish", appName)
  }

  @Test
  fun `recording file extension and mime type match format`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.RecordingRepository(context)

    assertEquals("audio/mpeg", repo.mimeTypeForFormat("MP3"))
    assertEquals("audio/wav", repo.mimeTypeForFormat("WAV"))
    assertEquals("audio/mp4", repo.mimeTypeForFormat("M4A"))

    assertEquals("mp3", repo.createNewRecordingFile("MP3").extension)
    assertEquals("wav", repo.createNewRecordingFile("WAV").extension)
    assertEquals("m4a", repo.createNewRecordingFile("M4A").extension)
  }
}
