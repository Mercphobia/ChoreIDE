package com.vibe.choreide.system

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Xml
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object AospMockupInflater {

    fun inflate(context: Context, rawXml: String): View {
        val sanitized = XmlResourceInterceptor.sanitizeAospXml(rawXml)

        val parser: XmlPullParser = Xml.newPullParser().apply {
            setInput(StringReader(sanitized))
        }

        val baseInflater = LayoutInflater.from(context)
        val inflater = baseInflater.cloneInContext(context)

        inflater.factory2 = object : LayoutInflater.Factory2 {
            override fun onCreateView(
                parent: View?,
                name: String,
                context: Context,
                attrs: AttributeSet
            ): View? = createSafe(parent, name, context, attrs)

            override fun onCreateView(
                name: String,
                context: Context,
                attrs: AttributeSet
            ): View? = createSafe(null, name, context, attrs)

            private fun createSafe(
                parent: View?,
                name: String,
                context: Context,
                attrs: AttributeSet
            ): View? {
                return try {
                    // Try standard prefixes for framework widgets
                    var view: View? = null
                    for (prefix in arrayOf("android.widget.", "android.view.", "androidx.compose.ui.platform.")) {
                        try {
                            view = inflater.createView(name, prefix, attrs)
                            if (view != null) break
                        } catch (ignored: ClassNotFoundException) {
                        }
                    }
                    view ?: mockView(context, name)
                } catch (e: ClassNotFoundException) {
                    mockView(context, name)
                } catch (e: InflateException) {
                    mockView(context, name)
                } catch (t: Throwable) {
                    mockView(context, name)
                }
            }

            private fun mockView(context: Context, tagName: String): View {
                val container = FrameLayout(context)
                container.setBackgroundColor(0x33FF0000)
                val label = TextView(context)
                label.text = tagName.substringAfterLast('.')
                label.setTextColor(Color.RED)
                label.textSize = 10f
                container.addView(label)
                return container
            }
        }

        return inflater.inflate(parser, null)
    }
}
