package ha.ak.on

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun App(viewModel: ViewModel = ViewModel()) {
    val state by viewModel.state.collectAsState()
    var topic by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SearchBar(
                    topic = topic,
                    onSearchClicked = {
                        scope.launch { viewModel.search(topic) }
                    },
                    isLoading = state is State.Loading,
                    onTopicChanged = { topic = it }
                )

                when (val currentState = state) {
                    is State.Error -> {
                        Text(
                            text = "An error occurred: ${currentState.message}",
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    State.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is State.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Word count: ${currentState.topicCount}",
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Text(
                                text = "Title: ${currentState.title}",
                                style = MaterialTheme.typography.subtitle1,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            HighlightMentions(
                                text = currentState.content,
                                topic = currentState.topic
                            )
                        }
                    }

                    State.Idle -> {
                        Text(
                            text = "Please enter a topic to search.",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    topic: String,
    isLoading: Boolean,
    onSearchClicked: () -> Unit,
    onTopicChanged: (String) -> Unit
) {
    val areButtonsEnabled = topic.isNotEmpty() && !isLoading
    val focusManager = LocalFocusManager.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            enabled = areButtonsEnabled,
            onClick = onSearchClicked
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search icon"
            )
        }
        OutlinedTextField(
            value = topic,
            onValueChange = onTopicChanged,
            enabled = !isLoading,
            label = { Text(text = "Enter a topic") },
            placeholder = { Text(text = "e.g. Håkon Helgetun Pettersen") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    onSearchClicked()
                }
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            enabled = areButtonsEnabled,
            onClick = { onTopicChanged("") }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear text"
            )
        }
    }
}

/**
 * Highlights all occurrences of [topic] in [text] with red color and underline.
 */
@Composable
private fun HighlightMentions(text: String, topic: String) {
    val words = text.split(Regex("\\W+")) // Split text into words based on non-word characters
    val annotatedText = buildAnnotatedString {
        var lastIndex = 0
        for (word in words) {
            // Find the index of the current word in the original text
            val startIndex = text.indexOf(word, lastIndex, ignoreCase = true)
            val endIndex = startIndex + word.length

            // Append text before the current word
            if (lastIndex < startIndex) {
                append(text.substring(lastIndex, startIndex))
            }

            // Highlight the word if it matches the topic (case-insensitive)
            if (word.equals(topic, ignoreCase = true)) {
                withStyle(
                    style = SpanStyle(
                        color = Color.Red,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(text.substring(startIndex, endIndex))
                }
            } else {
                append(text.substring(startIndex, endIndex))
            }

            lastIndex = endIndex
        }

        // Append the remaining text if any
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    Text(text = annotatedText)
}