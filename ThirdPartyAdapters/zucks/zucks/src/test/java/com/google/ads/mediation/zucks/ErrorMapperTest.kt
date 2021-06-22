package com.google.ads.mediation.zucks

import net.zucks.exception.FrameIdNotFoundException
import net.zucks.exception.NetworkNotFoundException
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import java.lang.RuntimeException

class ErrorMapperTest {

    @Test
    fun testConvertSdkErrorCode_frameIdNotFound() {
        val e = FrameIdNotFoundException()

        MatcherAssert.assertThat(
            ErrorMapper.convertSdkErrorCode(e),
            CoreMatchers.`is`(ErrorMapper.ERROR_INVALID_REQUEST)
        )
    }

    @Test
    fun testConvertSdkErrorCode_offline() {
        val e = NetworkNotFoundException()

        MatcherAssert.assertThat(
            ErrorMapper.convertSdkErrorCode(e),
            CoreMatchers.`is`(ErrorMapper.ERROR_NETWORK_ERROR)
        )
    }

    @Test
    fun testConvertSdkErrorCode_unknown() {
        val e = RuntimeException()

        MatcherAssert.assertThat(
            ErrorMapper.convertSdkErrorCode(e),
            CoreMatchers.`is`(ErrorMapper.ERROR_INTERNAL_ERROR)
        )
    }

    @Test
    fun testConvertSdkError() {
        val first = ErrorMapper.convertSdkError(null)
        val second = ErrorMapper.convertSdkError(RuntimeException("foo"))

        MatcherAssert.assertThat(
            first.message,
            CoreMatchers.`is`(CoreMatchers.not(CoreMatchers.equalTo(second.message)))
        )

        MatcherAssert.assertThat(
            first.message,
            CoreMatchers.`is`(CoreMatchers.not(CoreMatchers.nullValue()))
        )
        MatcherAssert.assertThat(
            second.message,
            CoreMatchers.`is`(CoreMatchers.not(CoreMatchers.nullValue()))
        )
    }

    @Test
    fun testCreateAdapterError() {
        val first = ErrorMapper.createAdapterError(Int.MAX_VALUE, "a")
        val second = ErrorMapper.createAdapterError(Int.MAX_VALUE, "b")

        MatcherAssert.assertThat(
            first.message,
            CoreMatchers.`is`(CoreMatchers.not(CoreMatchers.equalTo(second.message)))
        )
    }

}
