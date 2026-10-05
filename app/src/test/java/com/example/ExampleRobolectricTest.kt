package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  @Config(qualifiers = "ar")
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("الخزنة الذكية", appName)
  }

  @Test
  @Config(qualifiers = "en")
  fun `read English app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Smart Vault", context.getString(R.string.app_name))
  }
}
