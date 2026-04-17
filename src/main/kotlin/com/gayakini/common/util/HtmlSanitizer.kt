package com.gayakini.common.util

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

/**
 * Utility for sanitizing HTML input to prevent XSS.
 * Uses Jsoup with a relaxed safelist suitable for product descriptions.
 */
object HtmlSanitizer {
    private val safelist =
        Safelist.relaxed()
            .addTags("span", "div", "hr", "br")
            .addAttributes("span", "style")
            .addAttributes("div", "style")

    /**
     * Sanitizes the input string.
     */
    fun sanitize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return Jsoup.clean(input, safelist)
    }

    /**
     * Strips all HTML tags, returning only plain text.
     */
    fun stripAll(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return Jsoup.clean(input, Safelist.none())
    }
}
