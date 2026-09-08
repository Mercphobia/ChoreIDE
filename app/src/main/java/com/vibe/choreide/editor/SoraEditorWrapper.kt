package com.vibe.choreide.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.widget.CodeEditor

@Composable
fun SoraEditorWrapper(
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CodeEditor(context).apply {
                setText(text)
                subscribeAlways(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) {
                    onTextChanged(getText().toString())
                }
            }
        },
        update = { editor ->
            val current = editor.text.toString()
            if (current != text) {
                editor.setText(text)
            }
        }
    )
}
