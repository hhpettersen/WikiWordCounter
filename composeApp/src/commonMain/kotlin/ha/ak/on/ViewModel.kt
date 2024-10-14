package ha.ak.on

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class State {
    data object Idle : State()
    data object Loading : State()
    data class Success(
        val title: String,
        val content: String,
        val topic: String,
        val topicCount: Int,
    ) : State()

    data class Error(val message: String) : State()
}

class ViewModel {
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    suspend fun search(topic: String) {
        _state.value = State.Loading

        _state.value = fetchMyData(topic).fold(
            onSuccess = { apiResponse ->
                val sanitizedText = sanitizeText(apiResponse.parse.text.content)
                println(sanitizedText)
                State.Success(
                    title = apiResponse.parse.title,
                    content = sanitizedText,
                    topic = topic,
                    topicCount = countWordOccurrences(sanitizedText, topic)
                )
            },
            onFailure = { exception ->
                State.Error(exception.message ?: "Unknown error")
            }
        )
    }

    private fun String.removeStyleTags(): String {
        val styleRegex = Regex("<style.*?>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE)
        return replace(styleRegex, "")
    }

    private fun String.removeHtmlTags(): String {
        val tagRegex = Regex("<[^>]+>")
        return replace(tagRegex, " ")
    }

    private fun sanitizeText(htmlContent: String): String {
        return htmlContent.removeStyleTags()
            .removeHtmlTags()
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun countWordOccurrences(text: String, word: String): Int {
        val words = text.split(Regex("\\W+"))
        return words.count { it.equals(word, ignoreCase = true) }
    }
}