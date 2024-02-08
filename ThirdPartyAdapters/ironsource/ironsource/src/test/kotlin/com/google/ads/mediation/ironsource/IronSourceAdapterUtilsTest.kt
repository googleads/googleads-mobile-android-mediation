package com.google.ads.mediation.ironsource

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IronSourceAdapterUtilsTest {

  /** A dummy Ad class for the tests. */
  class Ad {}

  @Test
  fun canLoadIronSourceAdInstance_noReferent_returnsTrue() {
    val instanceId = "InstanceId"
    val adInstancesMap = ConcurrentHashMap<String, WeakReference<Ad>>()
    val referenceToInstance = WeakReference(Ad())
    adInstancesMap[instanceId] = referenceToInstance
    // Remove the referent.
    referenceToInstance.clear()

    assertThat(IronSourceAdapterUtils.canLoadIronSourceAdInstance(instanceId, adInstancesMap))
      .isTrue()
  }

  @Test
  fun canLoadIronSourceAdInstance_noReference_returnsTrue() {
    val instanceId = "InstanceId"
    val adInstancesMap = ConcurrentHashMap<String, WeakReference<Ad>>()

    assertThat(IronSourceAdapterUtils.canLoadIronSourceAdInstance(instanceId, adInstancesMap))
      .isTrue()
  }

  @Test
  fun canLoadIronSourceAdInstance_referenceWithReferentIsPresent_returnsFalse() {
    val instanceId = "InstanceId"
    val adInstancesMap = ConcurrentHashMap<String, WeakReference<Ad>>()
    val referenceToInstance = WeakReference(Ad())
    adInstancesMap[instanceId] = referenceToInstance

    assertThat(IronSourceAdapterUtils.canLoadIronSourceAdInstance(instanceId, adInstancesMap))
      .isFalse()
  }
}
