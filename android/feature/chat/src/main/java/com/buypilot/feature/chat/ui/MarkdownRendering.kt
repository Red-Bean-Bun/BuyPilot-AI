package com.buypilot.feature.chat.ui

import android.content.Context
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.buypilot.feature.chat.R
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import java.util.LinkedHashMap
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as MarkdownText
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

private const val MarkdownRenderCacheMaxEntries = 36
private val MarkdownSoftBlockColor = BuyPilotColors.MarkdownSoftBlock.toArgb()
private val MarkdownComposeCodeBackground = BuyPilotColors.MarkdownSoftBlock
private val PlainMarkdownParser: Parser = Parser.builder().build()
private val MarkdownBlockQuoteMarkerRegex = Regex("""^\s{0,3}>\s?""")
private val InternalDebugLabelValueRegex = Regex(
    """(?i)\b(?:product_id|evidence_id|source_id|action_id|feedback_type|criteria_patch|cart_id|trace_id|client_trace_id|session_id|turn_id)\s*[:：=]\s*[\w.-]+""",
)
private val InternalDebugLabelRegex = Regex(
    """(?i)\b(?:product_id|evidence_id|source_id|action_id|feedback_type|criteria_patch|cart_id|trace_id|client_trace_id|session_id|turn_id)\b""",
)
private val InternalDebugValueRegex = Regex(
    """(?i)\b(?:not_interested|view_detail|open_evidence|criteria_patch|add_to_cart|show_evidence)\b""",
)
private val InternalIdTokenRegex = Regex("""(?i)\b(?:pg|p)_[a-z0-9_-]*\b""")

private data class MarkdownTextRenderTag(
    val contentHash: Int,
    val contentLength: Int,
    val textColorArgb: Int,
    val fontSizePx: Float,
    val lineHeightPx: Float,
    val typefaceStyle: Int,
)

private fun String.withoutMarkdownBlockQuoteMarkers(): String =
    lineSequence()
        .joinToString("\n") { line -> line.replace(MarkdownBlockQuoteMarkerRegex, "") }

private fun String.withoutStreamingMarkdownChrome(): String =
    withoutMarkdownBlockQuoteMarkers()
        .lineSequence()
        .joinToString("\n") { line ->
            if (Regex("""^\s{0,3}(?:-{3,}|\*{3,}|_{3,})\s*$""").matches(line)) {
                "────────────"
            } else {
                line
                    .replace(Regex("""^\s{0,3}#{1,6}\s+"""), "")
                    .replace("**", "")
                    .replace("`", "")
            }
        }

internal fun String.withoutInternalDebugTokens(): String =
    replace(InternalDebugLabelValueRegex, "")
        .replace(InternalIdTokenRegex, "")
        .replace(InternalDebugValueRegex, "")
        .replace(InternalDebugLabelRegex, "")
        .replace(Regex("""[（(]\s*[，,、;；:\s]*[）)]"""), "")
        .replace(Regex("""\s+([，。！？；：、,.!?;:])"""), "$1")
        .replace(Regex("""([（(])\s+"""), "$1")
        .replace(Regex("""\s+([）)])"""), "$1")
        .replace(Regex("""[ \t]{2,}"""), " ")
        .replace(Regex("""(?m)^\s*[-•、,，;；:：]+\s*$"""), "")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()

internal fun String.withoutMarkdownMarkup(): String {
    val source = trim()
    if (source.isBlank()) return ""

    val builder = StringBuilder()
    val visitor = object : AbstractVisitor() {
        override fun visit(text: MarkdownText) {
            builder.append(text.literal)
        }

        override fun visit(code: Code) {
            builder.append(code.literal)
        }

        override fun visit(codeBlock: FencedCodeBlock) {
            appendSeparated(codeBlock.literal)
        }

        override fun visit(codeBlock: IndentedCodeBlock) {
            appendSeparated(codeBlock.literal)
        }

        override fun visit(link: Link) {
            visitChildren(link)
        }

        override fun visit(image: Image) {
            visitChildren(image)
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            builder.append(' ')
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            builder.append('\n')
        }

        override fun visit(paragraph: Paragraph) {
            appendBlockSeparator()
            visitChildren(paragraph)
        }

        private fun appendSeparated(value: String?) {
            val clean = value?.trim().orEmpty()
            if (clean.isBlank()) return
            appendBlockSeparator()
            builder.append(clean)
        }

        private fun appendBlockSeparator() {
            if (builder.isNotEmpty() && !builder.endsWithWhitespace()) {
                builder.append('\n')
            }
        }
    }
    PlainMarkdownParser.parse(source).accept(visitor)
    return builder.toString()
        .replace(Regex("""[ \t]+\n"""), "\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

private fun StringBuilder.endsWithWhitespace(): Boolean =
    isNotEmpty() && last().isWhitespace()

internal fun String.needsFinalMarkdownRender(): Boolean {
    val source = trim()
    if (source.isBlank()) return false
    return source.contains("```") ||
        Regex("""(?m)^\s{0,3}#{1,6}\s+""").containsMatchIn(source) ||
        Regex("""(?m)^\s{0,3}(?:-{3,}|\*{3,}|_{3,})\s*$""").containsMatchIn(source) ||
        Regex("""(?m)^\s*(?:[-*+]|\d+\.)\s+""").containsMatchIn(source) ||
        Regex("""\[[^\]]+]\([^)]+\)""").containsMatchIn(source) ||
        Regex("""(?<!\*)\*\*[^*\n]+\*\*(?!\*)""").containsMatchIn(source) ||
        Regex("""(?<!`)`[^`\n]+`(?!`)""").containsMatchIn(source) ||
        source.hasCompletedMarkdownTable()
}

internal fun String.requiresAndroidMarkdownRender(): Boolean {
    val source = trim()
    if (source.isBlank()) return false
    return source.contains("```") ||
        source.contains("<") ||
        Regex("""\[[^\]]+]\([^)]+\)""").containsMatchIn(source) ||
        source.hasCompletedMarkdownTable()
}

internal fun String.toNativeMarkdownAnnotatedString(): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var written = 0
    var lastChar: Char? = null
    var listDepth = 0
    val orderedCounters = ArrayDeque<Int>()

    fun appendText(value: String?) {
        if (value.isNullOrEmpty()) return
        builder.append(value)
        written += value.length
        lastChar = value.last()
    }

    fun appendChar(value: Char) {
        builder.append(value)
        written += 1
        lastChar = value
    }

    fun appendBlockSeparator() {
        if (written > 0 && lastChar?.isWhitespace() != true) {
            appendChar('\n')
        }
    }

    fun push(style: SpanStyle, block: () -> Unit) {
        builder.pushStyle(style)
        block()
        builder.pop()
    }

    val visitor = object : AbstractVisitor() {
        override fun visit(text: MarkdownText) {
            appendText(text.literal)
        }

        override fun visit(code: Code) {
            push(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = MarkdownComposeCodeBackground,
                ),
            ) {
                appendText(code.literal)
            }
        }

        override fun visit(strongEmphasis: StrongEmphasis) {
            push(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                visitChildren(strongEmphasis)
            }
        }

        override fun visit(emphasis: Emphasis) {
            push(SpanStyle(fontStyle = FontStyle.Italic)) {
                visitChildren(emphasis)
            }
        }

        override fun visit(link: Link) {
            visitChildren(link)
        }

        override fun visit(image: Image) {
            visitChildren(image)
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            appendChar(' ')
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            appendChar('\n')
        }

        override fun visit(paragraph: Paragraph) {
            appendBlockSeparator()
            visitChildren(paragraph)
        }

        override fun visit(heading: Heading) {
            appendBlockSeparator()
            push(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                visitChildren(heading)
            }
        }

        override fun visit(thematicBreak: ThematicBreak) {
            appendBlockSeparator()
            push(SpanStyle(color = BuyPilotColors.Border)) {
                appendText("────────────")
            }
            appendBlockSeparator()
        }

        override fun visit(bulletList: BulletList) {
            appendBlockSeparator()
            listDepth += 1
            visitChildren(bulletList)
            listDepth -= 1
        }

        override fun visit(orderedList: OrderedList) {
            appendBlockSeparator()
            listDepth += 1
            orderedCounters.addLast(orderedList.startNumber)
            visitChildren(orderedList)
            orderedCounters.removeLast()
            listDepth -= 1
        }

        override fun visit(listItem: ListItem) {
            appendBlockSeparator()
            appendText("  ".repeat((listDepth - 1).coerceAtLeast(0)))
            val prefix = if (orderedCounters.isEmpty()) {
                "• "
            } else {
                val next = orderedCounters.removeLast()
                orderedCounters.addLast(next + 1)
                "$next. "
            }
            appendText(prefix)
            visitChildren(listItem)
        }

        override fun visit(codeBlock: FencedCodeBlock) {
            appendBlockSeparator()
            push(SpanStyle(fontFamily = FontFamily.Monospace, background = MarkdownComposeCodeBackground)) {
                appendText(codeBlock.literal.trim())
            }
        }

        override fun visit(codeBlock: IndentedCodeBlock) {
            appendBlockSeparator()
            push(SpanStyle(fontFamily = FontFamily.Monospace, background = MarkdownComposeCodeBackground)) {
                appendText(codeBlock.literal.trim())
            }
        }
    }

    PlainMarkdownParser.parse(withoutMarkdownBlockQuoteMarkers()).accept(visitor)
    return builder.toAnnotatedString()
}

internal object NativeMarkdownRenderer {
    private val renderedCache = object : LinkedHashMap<String, AnnotatedString>(
        MarkdownRenderCacheMaxEntries,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AnnotatedString>?): Boolean =
            size > MarkdownRenderCacheMaxEntries
    }

    fun render(content: String): AnnotatedString {
        synchronized(this) {
            renderedCache[content]?.let { return it }
        }
        val rendered = content.toNativeMarkdownAnnotatedString()
        synchronized(this) {
            renderedCache[content]?.let { return it }
            renderedCache[content] = rendered
            return rendered
        }
    }

    fun clearForTest() {
        synchronized(this) {
            renderedCache.clear()
        }
    }
}

private fun String.hasCompletedMarkdownTable(): Boolean {
    val lines = lineSequence().map { it.trim() }.toList()
    return lines.windowed(size = 2).any { (header, separator) ->
        header.startsWith("|") &&
            header.endsWith("|") &&
            separator.startsWith("|") &&
            separator.endsWith("|") &&
            separator.contains("---")
    }
}

private fun typingStep(backlog: Int, done: Boolean): Int =
    when {
        backlog <= 0 -> 0
        done && backlog > 160 -> minOf(4, backlog)
        done && backlog > 72 -> minOf(3, backlog)
        backlog > 120 -> minOf(3, backlog)
        backlog > 48 -> minOf(2, backlog)
        else -> 1
    }

private fun typingDelayMs(lastVisibleChar: Char?, backlog: Int, done: Boolean): Long =
    when {
        lastVisibleChar == '\n' && backlog <= 48 -> if (done) 80L else 150L
        lastVisibleChar != null && backlog <= 48 && lastVisibleChar.isTypingPausePunctuation() -> {
            if (done) 72L else 128L
        }
        done && backlog > 160 -> 16L
        done && backlog > 72 -> 20L
        done -> 30L
        backlog > 120 -> 18L
        backlog > 48 -> 24L
        backlog > 20 -> 30L
        else -> 36L
    }

private fun Char.isTypingPausePunctuation(): Boolean =
    this in setOf('，', '。', '、', '；', '：', '！', '？', ',', '.', ';', ':', '!', '?')

@Composable
internal fun AssistantText(
    content: String,
    style: TextStyle = TextStyle(
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
    ),
) {
    if (content.isBlank()) return
    val renderMarkdown = remember(content) { content.needsFinalMarkdownRender() }
    if (!renderMarkdown) {
        PlainStreamingTextBlock(content = content, style = style)
        return
    }
    val useAndroidMarkdown = remember(content) { content.requiresAndroidMarkdownRender() }
    if (!useAndroidMarkdown) {
        NativeMarkdownTextBlock(content = content, style = style)
        return
    }
    MarkdownTextBlock(
        content = content,
        style = style,
    )
}

@Composable
internal fun StreamingAssistantText(
    nodeKey: String,
    content: String,
    done: Boolean = false,
    revealState: TextRevealProgress? = null,
    alreadyCompleted: Boolean = false,
    stablePlainAfterLiveReveal: Boolean = false,
    animateInitialCompleted: Boolean = false,
    initialRevealDelayMs: Long = 0L,
    style: TextStyle = TextStyle(
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
    ),
    onRevealComplete: (() -> Unit)? = null,
    onRevealActiveChange: ((Boolean) -> Unit)? = null,
    onRevealProgress: ((Int, Int) -> Unit)? = null,
) {
    if (content.isBlank()) {
        LaunchedEffect(nodeKey) {
            onRevealActiveChange?.invoke(false)
        }
        return
    }
    var hasSeenLiveStream by remember(nodeKey) { mutableStateOf(!done && !alreadyCompleted) }
    LaunchedEffect(nodeKey, done, alreadyCompleted) {
        if (!done && !alreadyCompleted) {
            hasSeenLiveStream = true
        }
    }
    val shouldRenderStatic = alreadyCompleted || (done && !hasSeenLiveStream && !animateInitialCompleted)
    if (shouldRenderStatic) {
        LaunchedEffect(nodeKey) {
            if (!alreadyCompleted) {
                onRevealActiveChange?.invoke(false)
                onRevealProgress?.invoke(content.length, content.length)
                if (done) onRevealComplete?.invoke()
            }
        }
        if (
            shouldKeepPlainTextRendererAfterStreaming(
                stablePlainAfterLiveReveal = stablePlainAfterLiveReveal,
                hasSeenLiveStream = hasSeenLiveStream,
                animateInitialCompleted = animateInitialCompleted,
            )
        ) {
            PlainStreamingTextBlock(content = content, style = style)
        } else {
            AssistantText(content = content, style = style)
        }
        return
    }

    var localVisibleLength by remember(nodeKey) {
        mutableIntStateOf(
            initialStreamingVisibleLength(
                contentLength = content.length,
                revealVisibleLength = revealState?.visibleLength,
                alreadyCompleted = alreadyCompleted,
                stablePlainAfterLiveReveal = stablePlainAfterLiveReveal,
            ),
        )
    }
    val latestOnRevealComplete by rememberUpdatedState(onRevealComplete)
    val latestOnRevealActiveChange by rememberUpdatedState(onRevealActiveChange)
    val latestOnRevealProgress by rememberUpdatedState(onRevealProgress)
    var completionReported by remember(nodeKey) { mutableStateOf(false) }
    val visibleLength = maxOf(localVisibleLength, revealState?.visibleLength ?: 0).coerceAtMost(content.length)

    DisposableEffect(nodeKey) {
        onDispose {
            latestOnRevealActiveChange?.invoke(false)
        }
    }

    LaunchedEffect(nodeKey, revealState?.visibleLength) {
        val target = maxOf(localVisibleLength, revealState?.visibleLength ?: 0).coerceAtMost(content.length)
        if (localVisibleLength < target || localVisibleLength > content.length) {
            localVisibleLength = target
        }
    }

    val latestContent by rememberUpdatedState(content)
    val latestDone by rememberUpdatedState(done)

    LaunchedEffect(nodeKey) {
        if (initialRevealDelayMs > 0 && localVisibleLength <= 0) {
            kotlinx.coroutines.delay(initialRevealDelayMs)
        }
        var activeReported = false
        while (true) {
            val targetContent = latestContent
            val targetLength = targetContent.length
            if (targetLength <= 0) {
                kotlinx.coroutines.delay(24L)
                continue
            }
            if (localVisibleLength > targetLength) {
                localVisibleLength = targetLength
            }
            val backlog = targetLength - localVisibleLength
            if (backlog <= 0) {
                if (latestDone) break
                kotlinx.coroutines.delay(24L)
                continue
            }
            if (!activeReported) {
                activeReported = true
                latestOnRevealActiveChange?.invoke(true)
            }
            val step = typingStep(backlog = backlog, done = latestDone)
            localVisibleLength = (localVisibleLength + step).coerceAtMost(targetLength)
            latestOnRevealProgress?.invoke(localVisibleLength, targetLength)
            val lastVisibleChar = targetContent.getOrNull(localVisibleLength - 1)
            kotlinx.coroutines.delay(typingDelayMs(lastVisibleChar, backlog = backlog, done = latestDone))
        }
        if (activeReported) {
            latestOnRevealActiveChange?.invoke(false)
        }
        if (latestDone && !completionReported) {
            completionReported = true
            val finalLength = latestContent.length
            latestOnRevealProgress?.invoke(finalLength, finalLength)
            latestOnRevealComplete?.invoke()
        }
    }

    if (visibleLength <= 0) return

    val visibleContent = content.take(visibleLength.coerceAtMost(content.length))
    PlainStreamingTextBlock(
        content = visibleContent,
        modifier = Modifier.fillMaxWidth(),
        style = style,
    )
}

internal fun shouldKeepPlainTextRendererAfterStreaming(
    stablePlainAfterLiveReveal: Boolean,
    hasSeenLiveStream: Boolean,
    animateInitialCompleted: Boolean,
): Boolean = stablePlainAfterLiveReveal || hasSeenLiveStream || animateInitialCompleted

internal fun initialStreamingVisibleLength(
    contentLength: Int,
    revealVisibleLength: Int?,
    alreadyCompleted: Boolean,
    stablePlainAfterLiveReveal: Boolean,
): Int {
    val boundedContentLength = contentLength.coerceAtLeast(0)
    return when {
        alreadyCompleted || stablePlainAfterLiveReveal -> boundedContentLength
        else -> revealVisibleLength.orZero().coerceIn(0, boundedContentLength)
    }
}

private fun Int?.orZero(): Int = this ?: 0

@Composable
internal fun StreamingAssistantText(
    content: String,
) {
    StreamingAssistantText(nodeKey = content, content = content, done = true)
}

@Composable
private fun PlainStreamingTextBlock(
    content: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    style: TextStyle = TextStyle(
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
    ),
) {
    val displayContent = remember(content) {
        content.withoutStreamingMarkdownChrome().withoutInternalDebugTokens()
    }
    if (displayContent.isBlank()) return
    Text(
        text = displayContent,
        style = style,
        modifier = modifier,
    )
}

@Composable
private fun NativeMarkdownTextBlock(
    content: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    style: TextStyle = TextStyle(
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
    ),
) {
    val displayContent = remember(content) { content.withoutInternalDebugTokens() }
    if (displayContent.isBlank()) return
    val annotated = remember(displayContent) {
        NativeMarkdownRenderer.render(displayContent)
    }
    if (annotated.isEmpty()) return
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
    )
}

private object ChatMarkdownRenderer {
    @Volatile private var instance: Markwon? = null
    private val renderedCache = object : LinkedHashMap<String, Spanned>(
        MarkdownRenderCacheMaxEntries,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Spanned>?): Boolean =
            size > MarkdownRenderCacheMaxEntries
    }

    fun get(context: Context): Markwon {
        val cached = instance
        if (cached != null) return cached
        return synchronized(this) {
            instance ?: Markwon.builder(context.applicationContext)
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                            .blockQuoteWidth(0)
                            .blockQuoteColor(android.graphics.Color.TRANSPARENT)
                            .codeBackgroundColor(MarkdownSoftBlockColor)
                            .codeBlockBackgroundColor(MarkdownSoftBlockColor)
                            .codeBlockMargin(8)
                    }
                })
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(context.applicationContext))
                .usePlugin(HtmlPlugin.create())
                .build()
                .also { instance = it }
        }
    }

    fun render(context: Context, content: String): Spanned {
        synchronized(this) {
            renderedCache[content]?.let { return it }
        }
        val rendered = get(context).toMarkdown(content.withoutMarkdownBlockQuoteMarkers())
        synchronized(this) {
            renderedCache[content]?.let { return it }
            renderedCache[content] = rendered
            return rendered
        }
    }
}

@Composable
internal fun MarkdownTextBlock(
    content: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    style: TextStyle = TextStyle(
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
    ),
) {
    val displayContent = remember(content) { content.withoutInternalDebugTokens() }
    if (displayContent.isBlank()) return
    val requiresAndroidMarkdown = remember(displayContent) { displayContent.requiresAndroidMarkdownRender() }
    if (!requiresAndroidMarkdown) {
        NativeMarkdownTextBlock(
            content = displayContent,
            modifier = modifier,
            style = style,
        )
        return
    }
    val context = LocalContext.current
    val appContext = context.applicationContext
    val markwon = remember(appContext) {
        ChatMarkdownRenderer.get(appContext)
    }
    val textColor = style.color.takeOrElse { BuyPilotColors.TextPrimary }
    val textColorArgb = remember(textColor) { textColor.toArgb() }
    val density = LocalDensity.current
    val fontSizePx = remember(density, style.fontSize) { with(density) { style.fontSize.toPx() } }
    val lineHeightPx = remember(density, style.lineHeight) { with(density) { style.lineHeight.toPx() } }
    val typefaceStyle = when (style.fontWeight) {
        FontWeight.Bold,
        FontWeight.ExtraBold,
        FontWeight.Black,
        FontWeight.SemiBold -> android.graphics.Typeface.BOLD
        else -> android.graphics.Typeface.NORMAL
    }
    val typeface = remember(typefaceStyle) {
        android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, typefaceStyle)
    }
    val renderKey = remember(displayContent, textColorArgb, fontSizePx, lineHeightPx, typefaceStyle) {
        MarkdownTextRenderTag(
            contentHash = displayContent.hashCode(),
            contentLength = displayContent.length,
            textColorArgb = textColorArgb,
            fontSizePx = fontSizePx,
            lineHeightPx = lineHeightPx,
            typefaceStyle = typefaceStyle,
        )
    }
    val renderedMarkdown = remember(appContext, displayContent) {
        ChatMarkdownRenderer.render(appContext, displayContent)
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextView(viewContext).apply {
                includeFontPadding = false
                setTextColor(textColorArgb)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx)
                setLineSpacing((lineHeightPx - fontSizePx).coerceAtLeast(0f), 1f)
                this.typeface = typeface
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
            }
        },
        update = { textView ->
            textView.setTextColor(textColorArgb)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx)
            textView.setLineSpacing((lineHeightPx - fontSizePx).coerceAtLeast(0f), 1f)
            textView.typeface = typeface
            if (textView.getTag(R.id.markdown_render_key) != renderKey) {
                markwon.setParsedMarkdown(textView, renderedMarkdown)
                textView.setTag(R.id.markdown_render_key, renderKey)
            }
        },
    )
}

@Preview(
    name = "Markdown Rendering",
    showBackground = true,
    backgroundColor = 0xFFF7F8FA,
)
@Composable
private fun MarkdownRenderingPreview() {
    Surface(color = BuyPilotColors.SurfaceBg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            AssistantText(
                content = """
                    ---
                    这款更稳，主要因为 **预算匹配**，同时证据里没有明显风险。

                    - 适合日常通勤
                    - 可以继续补充偏好
                """.trimIndent(),
            )
        }
    }
}
