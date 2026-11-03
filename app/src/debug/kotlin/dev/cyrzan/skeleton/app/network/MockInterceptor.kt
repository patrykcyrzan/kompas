package dev.cyrzan.skeleton.app.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Answers requests from JSON files in `src/debug/assets/mocks`, keyed by method and path:
 * `GET /v1/orders/42` -> `mocks/get/v1/orders/42.json`. Requests without a file go to the real backend,
 * so single endpoints can be mocked (e.g. ones the backend does not have yet).
 *
 * The status code can be put in the name to test error states: `mocks/get/v1/orders.500.json`
 * wins over `mocks/get/v1/orders.json`.
 */
internal class MockInterceptor(
    private val isEnabled: () -> Boolean,
    /** Contents of the asset at the path, or null if there is none. */
    private val readAsset: (path: String) -> String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isEnabled()) return chain.proceed(request)

        val base = "mocks/${request.method.lowercase()}${request.url.encodedPath.trimEnd('/')}"
        val (code, body) = ERROR_CODES.firstNotNullOfOrNull { code -> readAsset("$base.$code.json")?.let { code to it } }
            ?: readAsset("$base.json")?.let { 200 to it }
            ?: return chain.proceed(request)

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Mock")
            .header("X-Mock", "true")
            .body(body.toResponseBody(JSON))
            .build()
    }

    private companion object {
        val JSON = "application/json".toMediaType()
        val ERROR_CODES = listOf(400, 401, 403, 404, 409, 422, 500, 503)
    }
}
