package dev.cyrzan.skeleton.app.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class MockInterceptorTest {

    private val assets = mutableMapOf<String, String>()
    private var enabled = true

    /** Stands in for the network: whatever MockInterceptor passes on ends here. */
    private val backend = Interceptor { chain ->
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(299)
            .message("Backend")
            .body("backend".toResponseBody())
            .build()
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(MockInterceptor(isEnabled = { enabled }, readAsset = assets::get))
        .addInterceptor(backend)
        .build()

    private fun get(path: String): Pair<Int, String> =
        client.newCall(Request.Builder().url("https://api.test$path").build()).execute()
            .use { it.code to it.body.string() }

    @Test
    fun `answers from the asset matching method and path`() {
        assets["mocks/get/v1/orders.json"] = "[]"

        assertEquals(200 to "[]", get("/v1/orders"))
    }

    @Test
    fun `status code in the file name wins`() {
        assets["mocks/get/v1/orders.json"] = "[]"
        assets["mocks/get/v1/orders.500.json"] = """{"error":"boom"}"""

        assertEquals(500 to """{"error":"boom"}""", get("/v1/orders"))
    }

    @Test
    fun `goes to the backend when there is no mock`() {
        assertEquals(299 to "backend", get("/v1/profile"))
    }

    @Test
    fun `goes to the backend when disabled`() {
        assets["mocks/get/v1/orders.json"] = "[]"
        enabled = false

        assertEquals(299 to "backend", get("/v1/orders"))
    }
}
