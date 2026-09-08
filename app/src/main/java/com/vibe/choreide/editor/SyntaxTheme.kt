package com.vibe.choreide.editor

import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

object SyntaxTheme {
    fun applyDark(editor: io.github.rosemoe.sora.widget.CodeEditor) {
        // Sora ships a built-in dark scheme; apply Darcula-like overrides for key colors
        try {
            val scheme = editor.colorScheme
            scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, 0xFF1E1F22.toInt())
            scheme.setColor(EditorColorScheme.TEXT_NORMAL, 0xFFDFE1E5.toInt())
            scheme.setColor(EditorColorScheme.LINE_NUMBER, 0xFF6F737A.toInt())
            scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, 0xFF2B2D30.toInt())
            scheme.setColor(EditorColorScheme.KEYWORD, 0xFFCC7832.toInt())
            scheme.setColor(EditorColorScheme.COMMENT, 0xFF808080.toInt())
        } catch (t: Throwable) {
            // Best-effort theming; ignore if scheme fields differ by version
        }
    }
}
