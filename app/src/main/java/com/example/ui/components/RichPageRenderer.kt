package com.example.ui.components

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.PageContent
import com.example.data.model.PageElement
import com.example.util.LocalContentProcessor

@Composable
fun RichPageRenderer(
    pageContent: PageContent,
    modifier: Modifier = Modifier,
    textSizeMultiplier: Float = 1.0f,
    textColorOverride: Color? = null,
    textAlign: TextAlign = TextAlign.Start,
    selectedElementIndex: Int? = null,
    selectionStart: Int? = null,
    selectionEnd: Int? = null,
    onElementSelected: ((Int?) -> Unit)? = null,
    onSelectionRangeChanged: ((start: Int?, end: Int?) -> Unit)? = null,
    onDeleteElementRange: ((start: Int, end: Int) -> Unit)? = null,
    onToggleControlsOverlay: (() -> Unit)? = null,
    onElementTextUpdated: ((elementIndex: Int, newText: String) -> Unit)? = null,
    onEditingStateChanged: ((Boolean) -> Unit)? = null,
    onAskAiSelectedText: ((selectedText: String, elementIndex: Int, replaceSelectedText: ((String) -> Unit)?) -> Unit)? = null,
    onConvertDiagramToSvg: ((asciiText: String, onSvgResult: (String) -> Unit) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val bodyTextColor = textColorOverride ?: (if (isDark) Color.White else Color.Black)

    var editingElementIndex by remember { mutableStateOf<Int?>(null) }
    var editingText by remember { mutableStateOf("") }

    val activeStart = selectionStart ?: selectedElementIndex
    val activeEnd = selectionEnd ?: selectedElementIndex

    val minSel = if (activeStart != null && activeEnd != null) minOf(activeStart, activeEnd) else null
    val maxSel = if (activeStart != null && activeEnd != null) maxOf(activeStart, activeEnd) else null

    LaunchedEffect(editingElementIndex, activeStart, activeEnd) {
        onEditingStateChanged?.invoke(editingElementIndex != null || activeStart != null)
    }

    val alignHoriz = when (textAlign) {
        TextAlign.Center -> Alignment.CenterHorizontally
        TextAlign.End, TextAlign.Right -> Alignment.End
        else -> Alignment.Start
    }

    SelectionContainer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp, start = 1.dp, end = 1.dp),
            horizontalAlignment = alignHoriz
        ) {
            pageContent.elements.forEachIndexed { index, element ->
                val isSelected = minSel != null && maxSel != null && index in minSel..maxSel
                val isTopHandle = minSel != null && index == minSel
                val isBottomHandle = maxSel != null && index == maxSel

                if (editingElementIndex == index) {
                    // In-Place Line-by-Line Notepad Editor
                    val targetFontSize = when (element) {
                        is PageElement.Heading -> when (element.level) {
                            1 -> (22 * textSizeMultiplier).sp
                            2 -> (18 * textSizeMultiplier).sp
                            else -> (15 * textSizeMultiplier).sp
                        }
                        else -> (14 * textSizeMultiplier).sp
                    }

                    InPlaceLineTextEditor(
                        value = editingText,
                        onValueChange = { editingText = it },
                        onDone = {
                            onElementTextUpdated?.invoke(index, editingText)
                            editingElementIndex = null
                            onEditingStateChanged?.invoke(false)
                        },
                        fontSize = targetFontSize,
                        color = bodyTextColor,
                        textAlign = textAlign,
                        onAskAiSelectedText = { selectedText, replaceSelectedText ->
                            onAskAiSelectedText?.invoke(selectedText, index, replaceSelectedText)
                        }
                    )
                } else {
                    val elementModifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(4.dp)
                            } else Modifier
                        )
                        .pointerInput(index) {
                            detectTapGestures(
                                onTap = {
                                    onSelectionRangeChanged?.invoke(null, null)
                                    onElementSelected?.invoke(null)
                                    onToggleControlsOverlay?.invoke()
                                },
                                onDoubleTap = {
                                    when (element) {
                                        is PageElement.Heading,
                                        is PageElement.Paragraph,
                                        is PageElement.Callout,
                                        is PageElement.RawText -> {
                                            onSelectionRangeChanged?.invoke(null, null)
                                            onElementSelected?.invoke(null)
                                            editingElementIndex = index
                                            editingText = stripCodeAndTags(
                                                when (element) {
                                                    is PageElement.Heading -> element.text
                                                    is PageElement.Paragraph -> element.text
                                                    is PageElement.Callout -> element.text
                                                    is PageElement.RawText -> element.text
                                                    else -> ""
                                                }
                                            )
                                        }
                                        else -> {
                                            onSelectionRangeChanged?.invoke(index, index)
                                            onElementSelected?.invoke(index)
                                        }
                                    }
                                }
                            )
                        }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // SINGLE SLEEK 1-ROW FLOATING SELECTION TOOLBAR
                        if (isTopHandle && minSel != null && maxSel != null) {
                            val currentElement = pageContent.elements.getOrNull(minSel)
                            val currentRawText = when (currentElement) {
                                is PageElement.Heading -> currentElement.text
                                is PageElement.Paragraph -> currentElement.text
                                is PageElement.Callout -> currentElement.text
                                is PageElement.RawText -> currentElement.text
                                else -> ""
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Color options for this element (EXCLUDING RED!)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val colorOptions = listOf(
                                            "DEFAULT" to (if (isDark) Color.White else Color.Black),
                                            "#1E88E5" to Color(0xFF1E88E5), // Blue
                                            "#43A047" to Color(0xFF43A047), // Green
                                            "#FBC02D" to Color(0xFFFBC02D), // Yellow
                                            "#8E24AA" to Color(0xFF8E24AA), // Purple
                                            "#FB8C00" to Color(0xFFFB8C00), // Orange
                                            "#00ACC1" to Color(0xFF00ACC1)  // Teal
                                        )
                                        colorOptions.forEach { (hex, col) ->
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 3.dp)
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(col)
                                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                                    .clickable {
                                                        val clean = stripCodeAndTags(currentRawText)
                                                        val formatted = if (hex == "DEFAULT") clean else "<font color=\"$hex\">$clean</font>"
                                                        onElementTextUpdated?.invoke(minSel, formatted)
                                                    }
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Edit Pencil
                                        if (minSel == maxSel && currentRawText.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    editingElementIndex = minSel
                                                    editingText = stripCodeAndTags(currentRawText)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Create,
                                                    contentDescription = "Edit",
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }

                                        // Ask AI
                                        IconButton(
                                            onClick = {
                                                val rangeText = pageContent.elements
                                                    .subList(minSel, maxSel + 1)
                                                    .joinToString("\n\n") { el ->
                                                        when (el) {
                                                            is PageElement.Heading -> el.text
                                                            is PageElement.Paragraph -> el.text
                                                            is PageElement.Callout -> "${el.title}\n${el.text}"
                                                            is PageElement.DiagramData -> "${el.title}\n${el.rawAscii}"
                                                            is PageElement.RawText -> el.text
                                                            else -> ""
                                                        }
                                                    }
                                                if (rangeText.isNotBlank()) {
                                                    onAskAiSelectedText?.invoke(rangeText, minSel, null)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Ask AI",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        // Delete
                                        IconButton(
                                            onClick = {
                                                onDeleteElementRange?.invoke(minSel, maxSel)
                                                onSelectionRangeChanged?.invoke(null, null)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        // Close Selection
                                        IconButton(
                                            onClick = {
                                                onSelectionRangeChanged?.invoke(null, null)
                                                onElementSelected?.invoke(null)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        when (element) {
                            is PageElement.Heading -> {
                                val (baseSize, defaultColor, topMargin, bottomMargin) = when (element.level) {
                                    1 -> Quadruple(22.sp, Color(0xFFFFC107), 8.dp, 4.dp) // H1: Yellow
                                    2 -> Quadruple(18.sp, Color(0xFF2196F3), 6.dp, 3.dp) // H2: Blue
                                    else -> Quadruple(15.sp, Color(0xFF4CAF50), 4.dp, 2.dp) // H3: Green
                                }
                                val finalFontSize = (baseSize.value * textSizeMultiplier).sp
                                val finalColor = defaultColor

                                val annotated = parseRichAnnotatedString(element.text, finalColor, finalFontSize)

                                Spacer(modifier = Modifier.height(topMargin))
                                Text(
                                    text = annotated,
                                    fontSize = finalFontSize,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = (finalFontSize.value * 1.3f).sp,
                                    textAlign = textAlign,
                                    modifier = elementModifier
                                )
                                Spacer(modifier = Modifier.height(bottomMargin))
                            }

                            is PageElement.Paragraph -> {
                                val finalFontSize = (14 * textSizeMultiplier).sp
                                val annotated = parseRichAnnotatedString(
                                    rawText = element.text,
                                    defaultColor = bodyTextColor,
                                    defaultFontSize = finalFontSize,
                                    highlightTerms = element.highlightedTerms
                                )

                                Text(
                                    text = annotated,
                                    fontSize = finalFontSize,
                                    lineHeight = (finalFontSize.value * 1.55f).sp,
                                    color = bodyTextColor,
                                    textAlign = textAlign,
                                    modifier = elementModifier.padding(vertical = 2.dp)
                                )
                            }

                            is PageElement.BulletList -> {
                                val finalFontSize = (14 * textSizeMultiplier).sp
                                Column(modifier = elementModifier.padding(vertical = 2.dp)) {
                                    element.items.forEach { item ->
                                        val annotated = parseRichAnnotatedString(item, bodyTextColor, finalFontSize)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = "•",
                                                fontWeight = FontWeight.Bold,
                                                color = bodyTextColor,
                                                fontSize = finalFontSize,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text(
                                                text = annotated,
                                                fontSize = finalFontSize,
                                                lineHeight = (finalFontSize.value * 1.5f).sp,
                                                color = bodyTextColor,
                                                textAlign = textAlign,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            is PageElement.NumberedList -> {
                                val finalFontSize = (14 * textSizeMultiplier).sp
                                Column(modifier = elementModifier.padding(vertical = 2.dp)) {
                                    element.items.forEachIndexed { idx, item ->
                                        val annotated = parseRichAnnotatedString(item, bodyTextColor, finalFontSize)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = "${idx + 1}.",
                                                fontWeight = FontWeight.Bold,
                                                color = bodyTextColor,
                                                fontSize = finalFontSize,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text(
                                                text = annotated,
                                                fontSize = finalFontSize,
                                                lineHeight = (finalFontSize.value * 1.5f).sp,
                                                color = bodyTextColor,
                                                textAlign = textAlign,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            is PageElement.DiagramData -> {
                                val boxBg = if (isDark) Color(0xFF1A1A2E) else Color(0xFFE3F2FD)
                                val borderColor = if (isDark) Color(0xFF333355) else Color(0xFF90CAF9)
                                val asciiText = if (element.rawAscii.isNotBlank()) {
                                    element.rawAscii
                                } else if (element.nodes.isNotEmpty()) {
                                    element.nodes.joinToString("\n") { "[ ${it.label} ]" }
                                } else {
                                    element.title
                                }
                                val finalFontSize = (12 * textSizeMultiplier).sp

                                var showVisualSvg by remember { mutableStateOf(false) }

                                val parsedNodes = if (element.nodes.isNotEmpty()) element.nodes else {
                                    val cleanLines = element.rawAscii.lines()
                                        .map { line ->
                                            line.replace(Regex("[┌┐└┘├┤┬┴┼│─═▼▲►◄+|\\-=>]"), " ").trim()
                                        }
                                        .filter { it.length > 1 }
                                    cleanLines.mapIndexed { idx, lbl ->
                                        com.example.data.model.DiagramNode(id = "n$idx", label = lbl)
                                    }
                                }
                                val activeDiagram = if (element.nodes.isNotEmpty()) element else element.copy(nodes = parsedNodes)

                                Box(
                                    modifier = elementModifier
                                        .padding(vertical = 4.dp)
                                        .background(boxBg, RoundedCornerShape(8.dp))
                                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (element.title.isNotBlank() && element.title != "Diagram") element.title else "Structure Diagram",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (13 * textSizeMultiplier).sp,
                                                color = textColorOverride ?: Color(0xFF2196F3)
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { showVisualSvg = !showVisualSvg },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoAwesome,
                                                        contentDescription = "Toggle Diagram View",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(2.dp))
                                                IconButton(
                                                    onClick = { onDeleteElementRange?.invoke(index, index) },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Diagram",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (showVisualSvg) {
                                            DiagramCanvasView(
                                                diagram = activeDiagram,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (isDark) Color(0xFF12121A) else Color(0xFF263238), RoundedCornerShape(6.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = asciiText,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = finalFontSize,
                                                    lineHeight = (finalFontSize.value * 1.3f).sp,
                                                    color = Color(0xFF81C784),
                                                    textAlign = textAlign,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            is PageElement.TableData -> {
                                TableView(tableData = element)
                            }

                            is PageElement.Callout -> {
                                val boxBg = when (element.type) {
                                    "WARNING" -> if (isDark) Color(0xFF3E1C1C) else Color(0xFFFFEBEE)
                                    "HIGHLIGHT" -> if (isDark) Color(0xFF3E321C) else Color(0xFFFFF8E1)
                                    else -> if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE)
                                }
                                val borderCol = when (element.type) {
                                    "WARNING" -> Color(0xFFF44336)
                                    "HIGHLIGHT" -> Color(0xFFFFC107)
                                    else -> Color(0xFF2196F3)
                                }
                                val finalFontSize = (14 * textSizeMultiplier).sp
                                val annotated = parseRichAnnotatedString(element.text, bodyTextColor, finalFontSize)

                                Box(
                                    modifier = elementModifier
                                        .padding(vertical = 4.dp)
                                        .background(boxBg, RoundedCornerShape(6.dp))
                                        .border(1.dp, borderCol, RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        if (element.title.isNotBlank()) {
                                            Text(
                                                text = element.title,
                                                fontWeight = FontWeight.Bold,
                                                color = textColorOverride ?: borderCol,
                                                fontSize = finalFontSize,
                                                textAlign = textAlign,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                        Text(
                                            text = annotated,
                                            fontSize = finalFontSize,
                                            lineHeight = (finalFontSize.value * 1.4f).sp,
                                            color = bodyTextColor,
                                            textAlign = textAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            is PageElement.RawText -> {
                                val finalFontSize = (14 * textSizeMultiplier).sp
                                val annotated = parseRichAnnotatedString(element.text, bodyTextColor, finalFontSize)
                                Text(
                                    text = annotated,
                                    fontSize = finalFontSize,
                                    lineHeight = (finalFontSize.value * 1.55f).sp,
                                    color = bodyTextColor,
                                    textAlign = textAlign,
                                    modifier = elementModifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parses rich tags including <font color="#HEX">text</font>, <size scale="X">text</size>, and **highlight/bold**
 */
fun parseRichAnnotatedString(
    rawText: String,
    defaultColor: Color,
    defaultFontSize: TextUnit,
    highlightTerms: List<String> = emptyList()
): AnnotatedString {
    return buildAnnotatedString {
        // Regex supports single/double quotes or unquoted color/scale attributes
        val tagRegex = Regex("(?i)<font\\s+color=[\"']?([^\"'>]+)[\"']?>(.*?)</font>|(?i)<size\\s+scale=[\"']?([^\"'>]+)[\"']?>(.*?)</size>|\\*\\*(.*?)\\*\\*|__(.*?)__|(?i)<h1>(.*?)</h1>|(?i)<h2>(.*?)</h2>|(?i)<h3>(.*?)</h3>")

        fun cleanHtmlTags(s: String): String = s.replace(Regex("<[^>]*>"), "")

        var currentIndex = 0
        tagRegex.findAll(rawText).forEach { match ->
            if (match.range.first > currentIndex) {
                append(cleanHtmlTags(rawText.substring(currentIndex, match.range.first)))
            }

            val fontColor = match.groupValues[1]
            val fontText = match.groupValues[2]
            val sizeScaleStr = match.groupValues[3]
            val sizeText = match.groupValues[4]
            val boldText = match.groupValues[5].ifEmpty { match.groupValues[6] }
            val h1Text = match.groupValues[7]
            val h2Text = match.groupValues[8]
            val h3Text = match.groupValues[9]

            when {
                h1Text.isNotBlank() -> {
                    withStyle(SpanStyle(color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = defaultFontSize * 1.3f)) {
                        append(cleanHtmlTags(h1Text))
                    }
                }
                h2Text.isNotBlank() -> {
                    withStyle(SpanStyle(color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = defaultFontSize * 1.15f)) {
                        append(cleanHtmlTags(h2Text))
                    }
                }
                h3Text.isNotBlank() -> {
                    withStyle(SpanStyle(color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = defaultFontSize * 1.05f)) {
                        append(cleanHtmlTags(h3Text))
                    }
                }
                fontColor.isNotBlank() -> {
                    val parsedColor = try {
                        Color(android.graphics.Color.parseColor(fontColor))
                    } catch (e: Exception) {
                        defaultColor
                    }
                    withStyle(SpanStyle(color = parsedColor, fontWeight = FontWeight.Bold)) {
                        append(cleanHtmlTags(fontText))
                    }
                }
                sizeScaleStr.isNotBlank() -> {
                    val scale = sizeScaleStr.toFloatOrNull() ?: 1.0f
                    withStyle(SpanStyle(fontSize = defaultFontSize * scale)) {
                        append(cleanHtmlTags(sizeText))
                    }
                }
                boldText.isNotBlank() -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(cleanHtmlTags(boldText))
                    }
                }
            }
            currentIndex = match.range.last + 1
        }

        if (currentIndex < rawText.length) {
            append(cleanHtmlTags(rawText.substring(currentIndex)))
        }
    }
}

@Composable
fun InPlaceLineTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    fontSize: TextUnit,
    color: Color,
    textAlign: TextAlign,
    onAskAiSelectedText: ((selectedText: String, replaceSelectedText: (String) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val isDark = isSystemInDarkTheme()
    val lineGridColor = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.15f)

    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange.Zero))
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val selectedRange = textFieldValue.selection
        val hasTextSelection = !selectedRange.collapsed && selectedRange.min >= 0 && selectedRange.max <= textFieldValue.text.length
        val selectedSubstring = if (hasTextSelection) {
            textFieldValue.text.substring(selectedRange.min, selectedRange.max)
        } else ""

        // Header Row 1: Status & Save Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasTextSelection) "Selected: \"$selectedSubstring\"" else "Editing line... (Tap checkmark to save)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        onValueChange(textFieldValue.text)
                        onDone()
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(15.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save edit",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Row 2: Contextual Customization Toolbar (for selected word or whole line)
        val colorOptions = listOf(
            "DEFAULT" to (if (isDark) Color.White else Color.Black),
            "#1E88E5" to Color(0xFF1E88E5), // Blue
            "#43A047" to Color(0xFF43A047), // Green
            "#FBC02D" to Color(0xFFFBC02D), // Yellow
            "#8E24AA" to Color(0xFF8E24AA), // Purple
            "#FB8C00" to Color(0xFFFB8C00), // Orange
            "#00ACC1" to Color(0xFF00ACC1)  // Teal
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (hasTextSelection && selectedSubstring.isNotBlank()) {
                    // Size options for selected word
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Size: ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = {
                                val before = textFieldValue.text.substring(0, selectedRange.min)
                                val after = textFieldValue.text.substring(selectedRange.max)
                                val newText = "$before<size scale=\"0.85\">$selectedSubstring</size>$after"
                                textFieldValue = TextFieldValue(text = newText)
                                onValueChange(newText)
                            },
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("A-", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        OutlinedButton(
                            onClick = {
                                val before = textFieldValue.text.substring(0, selectedRange.min)
                                val after = textFieldValue.text.substring(selectedRange.max)
                                val newText = "$before<size scale=\"1.25\">$selectedSubstring</size>$after"
                                textFieldValue = TextFieldValue(text = newText)
                                onValueChange(newText)
                            },
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("A+", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Color options for selected word
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Color: ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        colorOptions.forEach { (hex, col) ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                    .clickable {
                                        val before = textFieldValue.text.substring(0, selectedRange.min)
                                        val after = textFieldValue.text.substring(selectedRange.max)
                                        val formattedWord = if (hex == "DEFAULT") selectedSubstring else "<font color=\"$hex\">$selectedSubstring</font>"
                                        val newText = "$before$formattedWord$after"
                                        textFieldValue = TextFieldValue(text = newText, selection = TextRange(selectedRange.min, selectedRange.min + formattedWord.length))
                                        onValueChange(newText)
                                    }
                            )
                        }
                    }

                    // Ask AI about this word
                    if (onAskAiSelectedText != null) {
                        Button(
                            onClick = {
                                val start = selectedRange.min
                                val end = selectedRange.max
                                onAskAiSelectedText.invoke(selectedSubstring) { replacement ->
                                    val currentText = textFieldValue.text
                                    val before = currentText.substring(0, start.coerceIn(0, currentText.length))
                                    val after = currentText.substring(end.coerceIn(0, currentText.length))
                                    val newText = before + replacement + after
                                    textFieldValue = TextFieldValue(
                                        text = newText,
                                        selection = TextRange(before.length, before.length + replacement.length)
                                    )
                                    onValueChange(newText)
                                }
                            },
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Ask AI", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Line level color options when no specific word is highlighted
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text("Line Color: ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        colorOptions.forEach { (hex, col) ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                    .clickable {
                                        val clean = stripCodeAndTags(textFieldValue.text)
                                        val formatted = if (hex == "DEFAULT") clean else "<font color=\"$hex\">$clean</font>"
                                        textFieldValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                                        onValueChange(formatted)
                                    }
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Draw small, tiny horizontal ruling lines that are subtle and elegant
                    val lineHeightPx = fontSize.toPx() * 1.55f
                    var currentY = lineHeightPx
                    while (currentY < size.height + lineHeightPx) {
                        drawLine(
                            color = lineGridColor,
                            start = Offset(0f, currentY),
                            end = Offset(size.width, currentY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
                        )
                        currentY += lineHeightPx
                    }
                }
                .padding(vertical = 2.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    onValueChange(it.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontSize = fontSize,
                    lineHeight = (fontSize.value * 1.55f).sp,
                    color = color,
                    textAlign = textAlign
                ),
                singleLine = false,
                maxLines = Int.MAX_VALUE,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Strips raw HTML tags, LaTeX code, and markdown markup from text for clean in-place editing
 */
fun stripCodeAndTags(rawText: String): String {
    return rawText
        .replace(Regex("(?i)<font[^>]*>|</font>"), "")
        .replace(Regex("(?i)<size[^>]*>|</size>"), "")
        .replace(Regex("(?i)<h[1-6]>|</h[1-6]>|<p>|</p>|<div[^>]*>|</div>|<b>|</b>|<i>|</i>|<code>|</code>"), "")
        .replace(Regex("\\*\\*|__"), "")
        .replace(Regex("\\\\text\\{(.*?)\\}"), "$1")
        .replace("&gt;", ">")
        .replace("&lt;", "<")
        .replace("&amp;", "&")
        .replace(Regex("<[^>]*>"), "")
        .trim()
}
