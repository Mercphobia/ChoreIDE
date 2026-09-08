package com.vibe.choreide.editor

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import java.util.regex.Pattern

/**
 * Lightweight smali syntax highlighter.
 *
 * Recognises:
 *  - directives: .class .super .method .end method .registers .locals .field
 *  - opcodes:    invoke-*, move*, const*, return-*, if-*, goto*, new-instance, ...
 *  - registers:  v0..v31, p0..pN
 *  - types:      Lpackage/Class;, [I, V, Z, B, S, C, I, J, F, D
 *  - comments:   # ...
 */
object SmaliHighlighter {

    private val DIRECTIVE_COLOR = 0xFFCC7832.toInt()   // orange
    private val OPCODE_COLOR = 0xFF56B6C2.toInt()      // cyan
    private val REGISTER_COLOR = 0xFF6A8759.toInt()    // green
    private val TYPE_COLOR = 0xFF9876AA.toInt()        // purple
    private val COMMENT_COLOR = 0xFF808080.toInt()     // grey
    private val STRING_COLOR = 0xFFCE9178.toInt()      // light brown

    private val directive = Pattern.compile("^\\s*\\.[a-z][a-z0-9-]*", Pattern.MULTILINE)
    private val opcode = Pattern.compile(
        "\b(invoke-[a-z-]+|move[a-z0-9/-]*|const[a-z0-9/-]*|return[a-z-]*|" +
                "if-[a-z]+|goto[a-z0-9/]*|new-instance|check-cast|instance-of|" +
                "sget[a-z-]*|sput[a-z-]*|iget[a-z-]*|iput[a-z-]*|aget[a-z-]*|" +
                "aput[a-z-]*|add-[a-z]+|sub-[a-z]+|mul-[a-z]+|div-[a-z]+|" +
                "rem-[a-z]+|and-[a-z]+|or-[a-z]+|xor-[a-z]+|cmp[a-z-]*|" +
                "throw|monitor-[a-z]+|array-length|fill-array-data)\b")
    private val register = Pattern.compile("\b[vp][0-9]+\b")
    private val type = Pattern.compile("L[a-zA-Z0-9_/$]+;|\\[[LZBSCIJFDV]|\\b[VZBSCIJFD]\\b")
    private val comment = Pattern.compile("#.*$", Pattern.MULTILINE)
    private val string = Pattern.compile("\"[^\"\n]*\"")

    /**
     * Rebuilds the editor content with spans applied. Sora accepts a CharSequence;
     * spans are rendered through its text painter.
     */
    fun apply(editor: CodeEditor) {
        try {
            val raw = editor.text.toString()
            val spannable = SpannableStringBuilder(raw)

            applyPattern(spannable, raw, directive, DIRECTIVE_COLOR)
            applyPattern(spannable, raw, opcode, OPCODE_COLOR)
            applyPattern(spannable, raw, register, REGISTER_COLOR)
            applyPattern(spannable, raw, type, TYPE_COLOR)
            applyPattern(spannable, raw, string, STRING_COLOR)
            applyPattern(spannable, raw, comment, COMMENT_COLOR)

            editor.setText(Content(spannable))
        } catch (t: Throwable) {
            // never break editing because of highlighting
        }
    }

    private fun applyPattern(
        spannable: Spannable,
        raw: String,
        pattern: Pattern,
        color: Int
    ) {
        val matcher = pattern.matcher(raw)
        while (matcher.find()) {
            spannable.setSpan(
                ForegroundColorSpan(color),
                matcher.start(),
                matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
