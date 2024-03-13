package com.google.ads.mediation.inmobi

import android.graphics.drawable.BitmapDrawable
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.inmobi.ImageDownloaderAsyncTask.DrawableDownloadListener
import java.net.URL
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.util.concurrent.InlineExecutorService

@RunWith(AndroidJUnit4::class)
class ImageDownloaderAsyncTaskTest {

  private val drawableDownloadListener = mock<DrawableDownloadListener>()
  private val imageDownloaderAsyncTask = ImageDownloaderAsyncTask(drawableDownloadListener, 0)
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

  companion object {
    private val urlMap =
      hashMapOf(ImageDownloaderAsyncTask.KEY_ICON to URL("http://www.google.com"))
  }
}
