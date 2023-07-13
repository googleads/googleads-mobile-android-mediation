package com.google.ads.mediation.inmobi

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InMobiMemoryCacheTest {

  private lateinit var inMobiMemoryCache: InMobiMemoryCache

  @Before
  fun setUp() {
    inMobiMemoryCache = InMobiMemoryCache(1000)
  }

  @Test
  fun put_sizeDidNotExceed_itemStoredInCache() {
    val drawable = mock<BitmapDrawable>()
    whenever(drawable.bitmap).thenReturn(Bitmap.createBitmap(20, 20, Bitmap.Config.ALPHA_8))

    // when item of size width x height i.e 20 x 20 bytes is put in cache with size 1000 bytes
    inMobiMemoryCache.put("id1", drawable)

    // ...item is stored in cache
    assertThat(inMobiMemoryCache.get("id1")).isNotNull()
  }

  @Test
  fun put_sizeExceeded_itemLRURemovedFromCache() {
    val drawable1 = mock<BitmapDrawable>()
    whenever(drawable1.bitmap).thenReturn(Bitmap.createBitmap(30, 30, Bitmap.Config.ALPHA_8))
    // put an item of size 900 bytes in the cache of size 1000 bytes
    inMobiMemoryCache.put("id1", drawable1)
    val drawable2 = mock<BitmapDrawable>()
    whenever(drawable2.bitmap).thenReturn(Bitmap.createBitmap(20, 20, Bitmap.Config.ALPHA_8))

    // ..verify "id1" exists in cache
    assertThat(inMobiMemoryCache.get("id1")).isNotNull()
    // put item if size 400 bytes in cache with remaining capacity 100 bytes
    inMobiMemoryCache.put("id2", drawable2)

    // verify least recently used item is removed from cache
    assertThat(inMobiMemoryCache.get("id1")).isNull()
    assertThat(inMobiMemoryCache.get("id2")).isNotNull()
  }

  @Test
  fun put_itemAlreadyInCache_itemGetsUpdated() {
    val width = 20
    val height = 20
    val drawable1 = mock<BitmapDrawable>()
    whenever(drawable1.bitmap).thenReturn(Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8))
    // put item with id - id1 in cache
    inMobiMemoryCache.put("id1", drawable1)
    val drawable2 = mock<BitmapDrawable>()
    whenever(drawable2.bitmap).thenReturn(Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8))

    // put item with same id in cache
    inMobiMemoryCache.put("id1", drawable2)

    // verify item gets updated
    assertThat(inMobiMemoryCache.get("id1")).isEqualTo(drawable2)
    assertThat(inMobiMemoryCache.size).isEqualTo(height * width)
  }
}
