package ha.ak.on

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ApiResponse(
    val parse: Parse
)

@Serializable
data class Parse(
    val title: String,
    val text: Text
)

@Serializable
data class Text(
    @SerialName("*")
    val content: String
)

val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 5_000
    }
}

suspend fun fetchMyData(topic: String): Result<ApiResponse> {
    val url = URLBuilder("https://en.wikipedia.org/w/api.php").apply {
        parameters.append("action", "parse")
        parameters.append("section", "0")
        parameters.append("prop", "text")
        parameters.append("format", "json")
        parameters.append("page", topic)
    }.build()
    return try {
        val response = httpClient.get(url).body<ApiResponse>()
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
