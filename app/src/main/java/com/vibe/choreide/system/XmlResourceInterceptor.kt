package com.vibe.choreide.system

object XmlResourceInterceptor {

    private val colorPattern = Regex("\"@\*android:color/[a-zA-Z0-9_]+\"")
    private val dimenPattern = Regex("\"@\*android:dimen/[a-zA-Z0-9_]+\"")
    private val drawablePattern = Regex("\"@\*android:drawable/[a-zA-Z0-9_]+\"")
    private val stylePattern = Regex("\"@\*android:style/[a-zA-Z0-9_.]+\"")

    fun sanitizeAospXml(rawXml: String): String {
        return rawXml
            .replace(colorPattern, "\"#808080\"")
            .replace(dimenPattern, "\"24dp\"")
            .replace(drawablePattern, "\"#404040\"")
            .replace(stylePattern, "\"@android:style/TextAppearance\"")
    }
}
