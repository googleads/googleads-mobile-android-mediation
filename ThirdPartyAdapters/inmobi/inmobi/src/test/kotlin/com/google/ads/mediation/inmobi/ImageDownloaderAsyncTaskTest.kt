package com.google.ads.mediation.inmobi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.ads.mediation.inmobi.ImageDownloaderAsyncTask.DrawableDownloadListener
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.util.concurrent.InlineExecutorService
import org.robolectric.shadows.ShadowBitmapFactory

@RunWith(RobolectricTestParameterInjector::class)
class ImageDownloaderAsyncTaskTest {

  private val drawableDownloadListener = mock<DrawableDownloadListener>()
  private val imageDownloaderAsyncTask =
    ImageDownloaderAsyncTask(drawableDownloadListener, /* timeout= */ 0)
  private val executor = InlineExecutorService()

  @Before fun setUp() {}

  @Test
  fun onIconKeyFoundInCache_invokesOnDownloadSuccessCallback() {
    val mockDrawable = mock<BitmapDrawable>()
    // pre-populate the cache
    imageDownloaderAsyncTask.memoryCache.put("http://www.google.com", mockDrawable)

    imageDownloaderAsyncTask.executeOnExecutor(executor, urlMap)
    shadowOf(Looper.getMainLooper()).idle()

    verify(drawableDownloadListener).onDownloadSuccess(any())
  }

  @Test
  fun onIconKeyNotFound_drawableFutureTimedOut_invokesOnDownloadFailureCallback() {
    // on empty cache...
    imageDownloaderAsyncTask.memoryCache.clear()

    // and Async task is invoked
    imageDownloaderAsyncTask.executeOnExecutor(executor, urlMap)
    shadowOf(Looper.getMainLooper()).idle()

    // ...drawable future timed out because of '0' timeout seconds
    verify(drawableDownloadListener).onDownloadFailure()
  }

  enum class InSampleSizeTestCase(
    val width: Int,
    val height: Int,
    val reqWidth: Int = 1024,
    val reqHeight: Int = 1024,
    val expectedInSampleSize: Int,
  ) {
    IMAGE_SMALLER_THAN_REQUESTED(width = 500, height = 500, expectedInSampleSize = 1),
    IMAGE_SAME_AS_REQUESTED(width = 1024, height = 1024, expectedInSampleSize = 1),
    IMAGE_TWICE_AS_LARGE(width = 2048, height = 2048, expectedInSampleSize = 2),
    IMAGE_FOUR_TIMES_AS_LARGE(width = 4096, height = 4096, expectedInSampleSize = 4),
    ONLY_HEIGHT_LARGER(width = 500, height = 2048, expectedInSampleSize = 1),
    ONLY_WIDTH_LARGER(width = 2048, height = 500, expectedInSampleSize = 1),
    NEGATIVE_OR_ZERO_DIMENSIONS(width = -1, height = -1, expectedInSampleSize = 1),
  }

  @Test
  fun calculateInSampleSize_returnsExpectedSampleSize(
    @TestParameter testCase: InSampleSizeTestCase
  ) {
    val options =
      BitmapFactory.Options().apply {
        outWidth = testCase.width
        outHeight = testCase.height
      }

    val inSampleSize =
      ImageDownloaderAsyncTask.calculateInSampleSize(
        options,
        /* reqWidth= */ testCase.reqWidth,
        /* reqHeight= */ testCase.reqHeight,
      )

    assertThat(inSampleSize).isEqualTo(testCase.expectedInSampleSize)
  }

  @Test
  fun onIconKeyNotFound_imageDownloadedSuccessfully_invokesOnDownloadSuccessCallback() {
    ShadowBitmapFactory.setAllowInvalidImageData(true)
    val bitmap = Bitmap.createBitmap(/* width= */ 10, /* height= */ 10, Bitmap.Config.ARGB_8888)
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempFile = File.createTempFile("test_icon", ".png", context.cacheDir)
    tempFile.deleteOnExit()
    FileOutputStream(tempFile).use { out ->
      bitmap.compress(Bitmap.CompressFormat.PNG, /* quality= */ 100, out)
    }
    val fileUrl = tempFile.toURI().toURL()

    val task = ImageDownloaderAsyncTask(drawableDownloadListener, /* timeout= */ 10)
    task.executeOnExecutor(executor, hashMapOf(ImageDownloaderAsyncTask.KEY_ICON to fileUrl))
    shadowOf(Looper.getMainLooper()).idle()

    verify(drawableDownloadListener).onDownloadSuccess(any())
  }

  @Test
  fun onIconKeyNotFound_failedToDecodeBitmap_invokesOnDownloadFailureCallback() {
    ShadowBitmapFactory.setAllowInvalidImageData(false)
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempFile = File.createTempFile("invalid_icon", ".png", context.cacheDir)
    tempFile.deleteOnExit()
    tempFile.writeText("invalid image bytes")
    val fileUrl = tempFile.toURI().toURL()

    val task = ImageDownloaderAsyncTask(drawableDownloadListener, /* timeout= */ 10)
    task.executeOnExecutor(executor, hashMapOf(ImageDownloaderAsyncTask.KEY_ICON to fileUrl))
    shadowOf(Looper.getMainLooper()).idle()

    verify(drawableDownloadListener).onDownloadFailure()
  }

  companion object {
    private val urlMap =
      hashMapOf(ImageDownloaderAsyncTask.KEY_ICON to URL("http://www.google.com"))
  }
}
