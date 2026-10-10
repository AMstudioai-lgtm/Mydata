package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Le SDK réellement exécuté est défini dans app/src/test/resources/robolectric.properties.
// L'API 36 exige Java 21, incompatible avec le JDK 17 utilisé par la CI et le build release.
@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mydata", appName)
  }
}
