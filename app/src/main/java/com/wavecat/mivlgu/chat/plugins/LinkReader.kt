package com.wavecat.mivlgu.chat.plugins

import com.knuddels.jtokkit.api.Encoding
import com.wavecat.mivlgu.chat.models.Message
import org.jsoup.Jsoup

class LinkReader(private val encoding: Encoding) : Plugin {

    private val cache: MutableMap<String, String> = mutableMapOf()
    private val context: LinkedHashSet<String> = linkedSetOf()

    override suspend fun onPostProcessMessage(assistantMessage: Message) {}

    override suspend fun onPreProcessMessage(userMessage: Message): String {
        val result = urlPattern
            .findAll(userMessage.content)
            .take(3)
            .map { it.value }

        context.addAll(result)

        val urls = mutableListOf<String>().apply {
            addAll(result)
            if (size < 3)
                addAll(context.reversed().take(3))
        }

        val data = urls.map {
            try {
                val url = if (!it.startsWith("http")) "https://${it}" else it
                val content =
                    if (cache.containsKey(url)) cache[url] else Jsoup.connect(url).get().text()
                cache[url] = content.toString()
                val text = content?.ifEmpty { "Empty" }
                "Contents of ${it}: $text"
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }

        return data.joinToString("\n") {
            encoding.decode(
                encoding.encode(it)
                    .take(MAX_DATA_TOKENS / data.size)
            )
        }
    }

    override suspend fun onClearContext() {
        context.clear()
    }

    companion object {
        private const val MAX_DATA_TOKENS = 2000

        private const val GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF"
        private const val IP_ADDRESS =
            ("((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))")

        private const val IRI =
            "[$GOOD_IRI_CHAR]([$GOOD_IRI_CHAR\\-]{0,61}[$GOOD_IRI_CHAR]){0,1}"

        private const val GOOD_GTLD_CHAR = "a-zA-Z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF"
        private const val GTLD = "[$GOOD_GTLD_CHAR]{2,63}"
        private const val HOST_NAME = "($IRI\\.)+$GTLD"
        private const val DOMAIN_NAME = "($HOST_NAME|$IP_ADDRESS)"

        val urlPattern = (
                ("((?:(http|https|Http|Https):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                        + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                        + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                        + "(?:" + DOMAIN_NAME + ")"
                        + "(?:\\:\\d{1,5})?)"
                        + "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~"
                        + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                        + "(?:\\b|$)")).toRegex()
    }
}