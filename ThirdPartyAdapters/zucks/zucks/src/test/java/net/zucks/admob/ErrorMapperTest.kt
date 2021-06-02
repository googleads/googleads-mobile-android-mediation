package net.zucks.admob

import net.zucks.exception.FrameIdNotFoundException
import net.zucks.exception.NetworkNotFoundException
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.lang.RuntimeException

class ErrorMapperTest {

    @Test
    fun testConvertSdkErrorCode_frameIdNotFound() {
        val e = FrameIdNotFoundException()

        assertThat(
            ErrorMapper.convertSdkErrorCode(e),
            `is`(ErrorMapper.ERROR_INVALID_REQUEST)
        )
    }

    @Test
    fun testConvertSdkErrorCode_offline() {
        val e = NetworkNotFoundException()

        assertThat(
            ErrorMapper.convertSdkErrorCode(e),
            `is`(ErrorMapper.ERROR_NETWORK_ERROR)
        )
    }

    @Test
    fun testConvertSdkErrorCode_unknown() {
        val e = RuntimeException()

        assertThat(
            ErrorMapper.convertSdkErrorCode(e),
            `is`(ErrorMapper.ERROR_INTERNAL_ERROR)
        )
    }

    @Test
    fun testConvertSdkError() {
        val first = ErrorMapper.convertSdkError(null)
        val second = ErrorMapper.convertSdkError(RuntimeException("foo"))

        assertThat(
            first.message,
            `is`(not(equalTo(second.message)))
        )

        assertThat(first.message, `is`(not(nullValue())))
        assertThat(second.message, `is`(not(nullValue())))
    }

    @Test
    fun testCreateAdapterError() {
        val first = ErrorMapper.createAdapterError(Int.MAX_VALUE, "a")
        val second = ErrorMapper.createAdapterError(Int.MAX_VALUE, "b")

        assertThat(
            first.message,
            `is`(not(equalTo(second.message)))
        )
    }

}
