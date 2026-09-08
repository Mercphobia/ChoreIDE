package com.vibe.choreide.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Sora-Editor Compose wrapper with custom syntax highlighting for
 * Smali (.smali), XML, Kotlin and Java. Highlighting for smali is applied
 * with a lightweight regex pass over the editor content region, avoiding
 * a full custom lexer implementation.
 */
@Composable
fun SmaliEditor(
    text: String,
    fileName: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CodeEditor(context).apply {
                applyChoreTheme(this)
                setText(Content(text))
                subscribeAlways(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) {
                    onTextChanged(getText().toString())
                }
            }
        },
        update = { editor ->
            val current = editor.text.toString()
            if (current != text) {
                editor.setText(Content(text))
            }
            if (fileName.endsWith(".smali")) {
                SmaliHighlighter.apply(editor)
            }
        }
    )
}

private fun applyChoreTheme(editor: CodeEditor) {
    try {
        val scheme = editor.colorScheme
        scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, 0xFF1E1F22.toInt())
        scheme.setColor(EditorColorScheme.TEXT_NORMAL, 0xFFDFE1E5.toInt())
        scheme.setColor(EditorColorScheme.LINE_NUMBER, 0xFF6F737A.toInt())
        scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, 0xFF2B2D30.toInt())
        scheme.setColor(EditorColorScheme.KEYWORD, 0xFFCC7832.toInt())
        scheme.setColor(EditorColorScheme.COMMENT, 0xFF808080.toInt())
    } catch (t: Throwable) {
        // best-effort theming
    }
}
