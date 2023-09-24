package com.wavecat.mivlgu.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wavecat.mivlgu.R
import com.wavecat.mivlgu.chat.models.Message
import com.wavecat.mivlgu.databinding.ChatMessageBinding
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import io.noties.prism4j.annotations.PrismBundle


@PrismBundle(
    includeAll = true,
    grammarLocatorClassName = ".GrammarLocatorDef"
)
class MessagesAdapter(
    val context: Context,
    var messages: List<Message>,
    val onClick: (Int, Message) -> Unit
) : RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {

    private val markwon = Markwon.builder(context)
        .usePlugin(GlideImagesPlugin.create(context))
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(MarkwonInlineParserPlugin.create())
        .usePlugin(SoftBreakAddsNewLinePlugin.create())
        .usePlugin(
            JLatexMathPlugin.create(context.resources.getDimension(R.dimen.text_medium)) { builder ->
                builder.inlinesEnabled(true)
            })
        .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create()))
        .usePlugin(
            SyntaxHighlightPlugin.create(
                Prism4j(GrammarLocatorDef()),
                Prism4jThemeDarkula.create()
            )
        )
        .build()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = ChatMessageBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_message, parent, false)
        )
    }

    @SuppressLint("RtlHardcoded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            val message = messages[position]

            role.text = when {
                message.isAssistantRole() -> context.getString(R.string.assistant)
                message.isUserRole() -> context.getString(R.string.you)
                else -> message.role.replaceFirstChar {
                    it.uppercase()
                }
            }

            if (message.isAssistantRole() || message.isInternalRole())
                markwon.setMarkdown(content, message.content)
            else
                content.text = message.content

            roleIcon.setImageResource(
                when {
                    message.isAssistantRole() -> R.drawable.chatgpt_icon
                    message.isUserRole() -> R.drawable.you_icon
                    message.isInternalRole() -> R.drawable.error_icon
                    else -> R.drawable.you_icon
                }
            )

            content.setTextIsSelectable(false)
            content.post { content.setTextIsSelectable(true) }

            content.setOnClickListener { onClick(position, message) }
        }
    }

    override fun getItemCount() = messages.size
}