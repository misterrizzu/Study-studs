package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.webkit.WebView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.ContentCopy
import com.example.data.local.ChapterEntity
import com.example.data.local.PageEntity
import com.example.data.model.DocumentPosition
import com.example.data.model.DocumentSelection
import com.example.data.model.LogicalDocumentIndex
import com.example.data.model.PageContent
import com.example.data.model.PageElement
import com.example.data.model.SelectionScope
import android.app.Activity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.components.DiagramCanvasView
import com.example.ui.components.RichPageRenderer
import com.example.util.LocalContentProcessor
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    chapter: ChapterEntity,
    pages: List<PageEntity>,
    hasQuestions: Boolean,
    onBackClick: () -> Unit,
    onProgressUpdate: (pageIndex: Int, totalPages: Int) -> Unit,
    onParsePageContent: (String) -> PageContent,
    onTakeTestClick: () -> Unit,
    onUpdatePageText: (chapterId: Long, pageIndex: Int, newText: String) -> Unit = { _, _, _ -> },
    onUpdateElementText: (chapterId: Long, globalIndex: Int, oldText: String, newText: String) -> Unit = { _, _, _, _ -> },
    onInsertElementAfter: (chapterId: Long, globalIndex: Int, newElem: PageElement) -> Unit = { _, _, _ -> },
    onReplaceElementAt: (chapterId: Long, globalIndex: Int, newElem: PageElement) -> Unit = { _, _, _ -> },
    onReplaceElementRange: (chapterId: Long, startIndex: Int, endIndex: Int, newElem: PageElement) -> Unit = { _, _, _, _ -> },
    onDeleteElementRange: (chapterId: Long, startIndex: Int, endIndex: Int) -> Unit = { _, _, _ -> },
    onReplaceDocumentSelection: (chapterId: Long, selection: DocumentSelection, replacement: String) -> Unit = { _, _, _ -> },
    onFormatDocumentSelection: (chapterId: Long, selection: DocumentSelection, prefix: String, suffix: String) -> Unit = { _, _, _, _ -> },
    onRestoreChapterPages: (chapterId: Long, pageSnapshots: List<PageEntity>) -> Unit = { _, _ -> },
    onAppendCallout: (chapterId: Long, pageIndex: Int, title: String, text: String) -> Unit = { _, _, _, _ -> },
    onRunAiAction: (instruction: String, pageText: String, onResult: (String) -> Unit) -> Unit = { _, _, _ -> },
    backgroundProcessingStatus: String? = null
) {
    // Text Formatting & Arrangement States
    var textSizeMultiplier by remember { mutableFloatStateOf(1.0f) }
    var selectedColorKey by remember { mutableStateOf("DEFAULT") }
    var selectedTextAlign by remember { mutableStateOf(TextAlign.Start) }
    var showFormattingDialog by remember { mutableStateOf(false) }
    var selectedElementIndex by remember { mutableStateOf<Int?>(null) }
    var selectionStart by remember { mutableStateOf<Int?>(null) }
    var selectionEnd by remember { mutableStateOf<Int?>(null) }
    var formattingScopeMode by remember { mutableIntStateOf(0) } // 0 = Entire Page, 1 = Targeted Selected Line

    // Automatically hide system status bar when in reading mode
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    val insetsController = remember(window) {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }

    // Process all page elements across the chapter and paginate into screen-fitting pages
    val displayPages: List<PageContent> = remember(pages, textSizeMultiplier) {
        val allElements = mutableListOf<PageElement>()
        for (pageEntity in pages) {
            val content = onParsePageContent(pageEntity.contentJson)
            allElements.addAll(content.elements)
        }
        if (allElements.isEmpty()) {
            listOf(PageContent(listOf(PageElement.Paragraph("No content available for this chapter.", emptyList()))))
        } else {
            val targetPoints = (780 / textSizeMultiplier).toInt().coerceAtLeast(300)
            LocalContentProcessor.paginateElements(allElements, maxPointsPerPage = targetPoints)
        }
    }

    val totalDisplayPages = displayPages.size.coerceAtLeast(1)
    val initialPage = chapter.lastReadPage.coerceIn(0, totalDisplayPages - 1)

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalDisplayPages }
    )
    val coroutineScope = rememberCoroutineScope()
    val documentIndex = remember(pages) {
        LogicalDocumentIndex.build(pages, onParsePageContent)
    }

    var showControlsOverlay by remember { mutableStateOf(false) }
    var showStatusBarAlways by remember { mutableStateOf(false) }

    // Control status bar visibility in reading mode based on user setting and controls overlay
    DisposableEffect(showControlsOverlay, showStatusBarAlways) {
        if (insetsController != null) {
            if (showStatusBarAlways || showControlsOverlay) {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    val textColorOverride: Color? = remember(selectedColorKey) {
        when (selectedColorKey) {
            "SEPIA_DARK" -> Color(0xFF3E2723)
            "SEPIA_GOLD" -> Color(0xFF5D4037)
            "DARK_SLATE" -> Color(0xFFE0E0E0)
            "OCEAN_BLUE" -> Color(0xFF1565C0)
            "EMERALD_GREEN" -> Color(0xFF2E7D32)
            "ROYAL_VIOLET" -> Color(0xFF6A1B9A)
            "AMBER_GOLD" -> Color(0xFFFF8F00)
            "CRIMSON" -> Color(0xFFC62828)
            else -> null
        }
    }

    // Dialog States for Editing & AI Assistant
    var showEditDialog by remember { mutableStateOf(false) }
    var currentEditingText by remember { mutableStateOf("") }

    var showAiDialog by remember { mutableStateOf(false) }
    var customAiPrompt by remember { mutableStateOf("Explain the main concept on this page in simple terms.") }
    var isAiRunning by remember { mutableStateOf(false) }
    var aiOutputText by remember { mutableStateOf("") }

    var showAskAiSheet by remember { mutableStateOf(false) }
    var selectedTextForAi by remember { mutableStateOf("") }
    var selectedElementIndexForAi by remember { mutableStateOf<Int?>(null) }
    var replaceSelectedTextFromAi by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var askAiInitialPrompt by remember { mutableStateOf("") }
    var activeDocumentSelection by remember { mutableStateOf<DocumentSelection?>(null) }
    var undoSnapshots by remember { mutableStateOf<List<List<PageEntity>>>(emptyList()) }
    var redoSnapshots by remember { mutableStateOf<List<List<PageEntity>>>(emptyList()) }

    var isInteracting by remember { mutableStateOf(false) }
    var isScrollMode by remember { mutableStateOf(true) }

    // Intercept Back button during text selection or line editing to clear selection without navigating away
    BackHandler(enabled = isInteracting || selectedElementIndex != null) {
        isInteracting = false
        selectedElementIndex = null
        activeDocumentSelection = null
    }

    fun selectDocumentScope(selection: DocumentSelection?) {
        activeDocumentSelection = selection
        if (selection == null) {
            selectionStart = null
            selectionEnd = null
            selectedElementIndex = null
        } else {
            val normalized = selection.normalized(documentIndex)
            selectionStart = normalized.start.blockIndex
            selectionEnd = normalized.end.blockIndex
            selectedElementIndex = normalized.start.blockIndex
        }
    }

    fun selectionLabel(selection: DocumentSelection): String {
        return when (selection.scope) {
            SelectionScope.TEXT_RANGE -> "Text range"
            SelectionScope.BLOCK_RANGE -> "Blocks ${selection.start.blockIndex + 1}-${selection.end.blockIndex + 1}"
            SelectionScope.PAGE -> "Page ${documentIndex.blocks.getOrNull(selection.start.blockIndex)?.pageIndex?.plus(1) ?: pagerState.currentPage + 1}"
            SelectionScope.CHAPTER -> "Chapter"
            SelectionScope.DOCUMENT -> "Entire document"
        }
    }

    fun selectionForElementRange(startElementIndex: Int, endElementIndex: Int): DocumentSelection? {
        val minElement = minOf(startElementIndex, endElementIndex)
        val maxElement = maxOf(startElementIndex, endElementIndex)
        val selectedBlocks = documentIndex.blocks.filter { it.documentElementIndex in minElement..maxElement }
        val first = selectedBlocks.firstOrNull() ?: return null
        val last = selectedBlocks.last()
        return DocumentSelection(
            start = DocumentPosition(first.blockIndex, 0),
            end = DocumentPosition(last.blockIndex, last.text.length),
            scope = SelectionScope.BLOCK_RANGE
        )
    }

    fun selectionForCurrentDisplayPage(): DocumentSelection? {
        val currentPage = displayPages.getOrNull(pagerState.currentPage) ?: return null
        if (currentPage.elements.isEmpty()) return null
        val startElementIndex = displayPages.take(pagerState.currentPage).sumOf { it.elements.size }
        val endElementIndex = startElementIndex + currentPage.elements.lastIndex
        return selectionForElementRange(startElementIndex, endElementIndex)?.copy(scope = SelectionScope.PAGE)
    }

    fun runUndoableEdit(edit: () -> Unit) {
        undoSnapshots = undoSnapshots + listOf(pages)
        redoSnapshots = emptyList()
        edit()
    }

    fun undoLastEdit() {
        val snapshot = undoSnapshots.lastOrNull() ?: return
        undoSnapshots = undoSnapshots.dropLast(1)
        redoSnapshots = redoSnapshots + listOf(pages)
        onRestoreChapterPages(chapter.id, snapshot)
        selectDocumentScope(null)
    }

    fun redoLastEdit() {
        val snapshot = redoSnapshots.lastOrNull() ?: return
        redoSnapshots = redoSnapshots.dropLast(1)
        undoSnapshots = undoSnapshots + listOf(pages)
        onRestoreChapterPages(chapter.id, snapshot)
        selectDocumentScope(null)
    }

    fun openAiForDocumentSelection(selection: DocumentSelection, instruction: String? = null) {
        val normalized = selection.normalized(documentIndex)
        val selectedText = documentIndex.textForSelection(normalized)
        if (selectedText.isBlank()) return
        selectedTextForAi = selectedText
        selectedElementIndexForAi = normalized.start.blockIndex
        askAiInitialPrompt = instruction ?: when (normalized.scope) {
            SelectionScope.PAGE -> "Rewrite this page clearly while preserving meaning."
            SelectionScope.CHAPTER -> "Rewrite this chapter clearly for study notes."
            SelectionScope.DOCUMENT -> "Improve this document while preserving its structure and meaning."
            else -> "Rewrite the selected text clearly while preserving meaning."
        }
        replaceSelectedTextFromAi = { replacement ->
            runUndoableEdit {
                onReplaceDocumentSelection(chapter.id, normalized, replacement)
            }
            selectDocumentScope(null)
        }
        showAskAiSheet = true
    }

    fun getRawTextForCurrentPage(): String {
        val page = displayPages.getOrNull(pagerState.currentPage) ?: return ""
        return page.elements.joinToString("\n\n") { el ->
            when (el) {
                is PageElement.Paragraph -> el.text
                is PageElement.Heading -> el.text
                is PageElement.RawText -> el.text
                is PageElement.Callout -> "${el.title}: ${el.text}"
                is PageElement.BulletList -> el.items.joinToString("\n• ", prefix = "• ")
                is PageElement.NumberedList -> el.items.mapIndexed { i, item -> "${i + 1}. $item" }.joinToString("\n")
                else -> ""
            }
        }.ifBlank { "Sample content" }
    }

    // Save exact page location whenever page changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { currPage ->
            onProgressUpdate(currPage, totalDisplayPages)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isScrollMode) {
                // Continuous Vertical Scroll Mode
                val allChapterElements = remember(displayPages) { displayPages.flatMap { it.elements } }
                val lazyListState = rememberLazyListState()

                Column(modifier = Modifier.fillMaxSize()) {
                    val totalItems = allChapterElements.size.coerceAtLeast(1)
                    val progressPct = ((lazyListState.firstVisibleItemIndex + 1) * 100) / totalItems

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📜 Continuous Scroll Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$progressPct% complete",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (lazyListState.firstVisibleItemIndex.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 2.dp, vertical = 4.dp)
                    ) {
                        RichPageRenderer(
                            pageContent = PageContent(allChapterElements),
                            textSizeMultiplier = textSizeMultiplier,
                            textColorOverride = textColorOverride,
                            textAlign = selectedTextAlign,
                            selectedElementIndex = selectedElementIndex,
                            selectionStart = selectionStart,
                            selectionEnd = selectionEnd,
                            onElementSelected = { selectedElementIndex = it },
                            onSelectionRangeChanged = { start, end ->
                                selectionStart = start
                                selectionEnd = end
                                selectedElementIndex = start
                                activeDocumentSelection = if (start != null && end != null) {
                                    selectionForElementRange(start, end)
                                } else {
                                    null
                                }
                            },
                            onDeleteElementRange = { start, end ->
                                onDeleteElementRange(chapter.id, start, end)
                                selectionStart = null
                                selectionEnd = null
                                selectedElementIndex = null
                            },
                            onToggleControlsOverlay = { showControlsOverlay = !showControlsOverlay },
                            onElementTextUpdated = { elemIdx, newText ->
                                val elem = allChapterElements.getOrNull(elemIdx)
                                val oldText = when (elem) {
                                    is PageElement.Heading -> elem.text
                                    is PageElement.Paragraph -> elem.text
                                    is PageElement.Callout -> elem.text
                                    is PageElement.RawText -> elem.text
                                    else -> ""
                                }
                                onUpdateElementText(chapter.id, elemIdx, oldText, newText)
                            },
                            onEditingStateChanged = { isInteracting = it },
                            onAskAiSelectedText = { selText, elemIdx, replaceSelectedText ->
                                selectedTextForAi = selText
                                selectedElementIndexForAi = elemIdx
                                replaceSelectedTextFromAi = replaceSelectedText
                                askAiInitialPrompt = ""
                                showAskAiSheet = true
                            },
                            onConvertDiagramToSvg = { asciiText, onResult ->
                                onRunAiAction(
                                    "Convert this ASCII diagram into clean, self-contained SVG code. Output ONLY <svg>...</svg>.",
                                    asciiText,
                                    onResult
                                )
                            }
                        )
                    }
                }
            } else {
                // Horizontal Page Swipe Mode
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isInteracting,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val currentPageContent = displayPages.getOrElse(pageIndex) { PageContent() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isInteracting) {
                                    showControlsOverlay = !showControlsOverlay
                                }
                            }
                            .padding(1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (backgroundProcessingStatus != null) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = backgroundProcessingStatus,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            val isEmptyChapter = currentPageContent.elements.isEmpty() ||
                                (currentPageContent.elements.size == 1 &&
                                 (currentPageContent.elements[0] as? PageElement.Paragraph)?.text?.contains("No content available", ignoreCase = true) == true)

                            if (isEmptyChapter) {
                                var syllabusTopicInput by remember { mutableStateOf(chapter.title) }
                                var isGeneratingChapterNotes by remember { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "✨ AI Syllabus & Notes Generator",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "No source notes uploaded yet for '${chapter.title}'. Type a topic name or paste your full syllabus, and AI will generate complete notes for you!",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedTextField(
                                            value = syllabusTopicInput,
                                            onValueChange = { syllabusTopicInput = it },
                                            label = { Text("Topic or Full Syllabus") },
                                            placeholder = { Text("E.g. Newton's Laws, Chapter 1 Intro, or paste entire syllabus...") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (isGeneratingChapterNotes) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("AI is generating notes...", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    if (syllabusTopicInput.isNotBlank()) {
                                                        isGeneratingChapterNotes = true
                                                        val prompt = "You are an expert tutor writing study material for '${chapter.title}'. User provided topic/syllabus: '$syllabusTopicInput'. Generate comprehensive, highly practical study notes. Use bold headings (# Heading), clear bullet points, key terms, and examples."
                                                        onRunAiAction(prompt, "") { generatedNotes ->
                                                            isGeneratingChapterNotes = false
                                                            if (generatedNotes.isNotBlank()) {
                                                                onUpdatePageText(chapter.id, pageIndex, generatedNotes)
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Generate Topic Notes with AI", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                RichPageRenderer(
                                    pageContent = currentPageContent,
                                    textSizeMultiplier = textSizeMultiplier,
                                    textColorOverride = textColorOverride,
                                    textAlign = selectedTextAlign,
                                    selectedElementIndex = selectedElementIndex,
                                    selectionStart = selectionStart,
                                    selectionEnd = selectionEnd,
                                    onElementSelected = { selectedElementIndex = it },
                                    onSelectionRangeChanged = { start, end ->
                                        selectionStart = start
                                        selectionEnd = end
                                        selectedElementIndex = start
                                        val pageOffset = displayPages.take(pageIndex).sumOf { it.elements.size }
                                        activeDocumentSelection = if (start != null && end != null) {
                                            val globalStart = pageOffset + minOf(start, end)
                                            val globalEnd = pageOffset + maxOf(start, end)
                                            selectionForElementRange(globalStart, globalEnd)
                                        } else {
                                            null
                                        }
                                    },
                                    onDeleteElementRange = { startLocal, endLocal ->
                                        val pageOffset = displayPages.take(pageIndex).sumOf { it.elements.size }
                                        onDeleteElementRange(chapter.id, pageOffset + startLocal, pageOffset + endLocal)
                                        selectionStart = null
                                        selectionEnd = null
                                        selectedElementIndex = null
                                    },
                                    onToggleControlsOverlay = { showControlsOverlay = !showControlsOverlay },
                                    onElementTextUpdated = { elemIdx, newText ->
                                        val globalIndex = displayPages.take(pageIndex).sumOf { it.elements.size } + elemIdx
                                        val elem = currentPageContent.elements.getOrNull(elemIdx)
                                        val oldText = when (elem) {
                                            is PageElement.Heading -> elem.text
                                            is PageElement.Paragraph -> elem.text
                                            is PageElement.Callout -> elem.text
                                            is PageElement.RawText -> elem.text
                                            else -> ""
                                        }
                                        onUpdateElementText(chapter.id, globalIndex, oldText, newText)
                                    },
                                    onEditingStateChanged = { isInteracting = it },
                                    onAskAiSelectedText = { selText, elemIdx, replaceSelectedText ->
                                        selectedTextForAi = selText
                                        val pageOffset = displayPages.take(pageIndex).sumOf { it.elements.size }
                                        selectedElementIndexForAi = pageOffset + elemIdx
                                        replaceSelectedTextFromAi = replaceSelectedText
                                        askAiInitialPrompt = ""
                                        showAskAiSheet = true
                                    },
                                    onConvertDiagramToSvg = { asciiText, onResult ->
                                        onRunAiAction(
                                            "Convert this ASCII diagram into clean, self-contained SVG code. Output ONLY <svg>...</svg>.",
                                            asciiText,
                                            onResult
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(top = 2.dp)
                                )
                            }
                            // Page number footer cleanly visible at the bottom
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Page ${pageIndex + 1} of $totalDisplayPages",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            if (!isScrollMode) {
                // Left Edge Tap Area (Prev Page)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(16.dp)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isInteracting && pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        }
                )

                // Right Edge Tap Area (Next Page)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(16.dp)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isInteracting && pagerState.currentPage < totalDisplayPages - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
                )
            }

            // Floating Action Button for AI (Always Visible in Reader)
            FloatingActionButton(
                onClick = {
                    aiOutputText = ""
                    showAiDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 60.dp, end = 16.dp)
                    .size(50.dp)
                    .testTag("floating_ai_button"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Ask AI Assistant",
                    modifier = Modifier.size(22.dp)
                )
            }

            activeDocumentSelection?.let { selection ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    tonalElevation = 6.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectionLabel(selection),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = { openAiForDocumentSelection(selection) },
                            contentPadding = ButtonDefaults.ContentPadding
                        ) {
                            Text("AI Edit", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { openAiForDocumentSelection(selection, "Summarize the selected scope into concise study notes.") }
                        ) {
                            Text("Summarize", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val selectedText = documentIndex.textForSelection(selection)
                                val diagram = LocalContentProcessor.createEducationalDiagramIfUseful(
                                    content = selectedText,
                                    instruction = "Generate Diagram"
                                )
                                if (diagram != null) {
                                    val normalized = selection.normalized(documentIndex)
                                    val targetIndex = documentIndex.blocks
                                        .getOrNull(normalized.end.blockIndex)
                                        ?.documentElementIndex
                                        ?: normalized.end.blockIndex
                                    runUndoableEdit {
                                        onInsertElementAfter(chapter.id, targetIndex, diagram)
                                    }
                                }
                            }
                        ) {
                            Text("Diagram", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                runUndoableEdit {
                                    onFormatDocumentSelection(chapter.id, selection, "<font color=\"#1E88E5\">", "</font>")
                                }
                            }
                        ) {
                            Text("Blue", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                runUndoableEdit {
                                    onFormatDocumentSelection(chapter.id, selection, "**", "**")
                                }
                            }
                        ) {
                            Text("Bold", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                runUndoableEdit {
                                    onReplaceDocumentSelection(chapter.id, selection, "")
                                }
                                selectDocumentScope(null)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete", fontSize = 12.sp)
                        }
                        TextButton(onClick = { selectDocumentScope(null) }) {
                            Text("Clear", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Minimal Controls Overlay
            AnimatedVisibility(
                visible = showControlsOverlay,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showControlsOverlay = false }
                ) {
                    // Top Bar Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = chapter.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val progressPct = ((pagerState.currentPage + 1) * 100) / totalDisplayPages
                            Text(
                                text = "Page ${pagerState.currentPage + 1} of $totalDisplayPages ($progressPct%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Toggle Continuous Scroll / Page Mode
                            IconButton(
                                onClick = { isScrollMode = !isScrollMode }
                            ) {
                                Icon(
                                    imageVector = if (isScrollMode) Icons.Default.ViewDay else Icons.Default.VerticalSplit,
                                    contentDescription = "Toggle Scroll/Page Mode",
                                    tint = if (isScrollMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Text Formatting & Alignment Button
                            IconButton(
                                onClick = { showFormattingDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = "Format Text & Alignment",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Pencil Icon for editing word/text
                            IconButton(
                                onClick = {
                                    currentEditingText = getRawTextForCurrentPage()
                                    showEditDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Word/Page Text",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // AI Icon for explaining/rewriting
                            IconButton(
                                onClick = {
                                    aiOutputText = ""
                                    showAiDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Reader Assistant",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (hasQuestions) {
                                IconButton(
                                    onClick = onTakeTestClick,
                                    modifier = Modifier.testTag("take_test_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Quiz,
                                        contentDescription = "Take Test",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(
                                onClick = { undoLastEdit() },
                                enabled = undoSnapshots.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Undo global edit",
                                    tint = if (undoSnapshots.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                )
                            }
                            IconButton(
                                onClick = { redoLastEdit() },
                                enabled = redoSnapshots.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Redo global edit",
                                    tint = if (redoSnapshots.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 78.dp, start = 12.dp, end = 12.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                selectionForCurrentDisplayPage()?.let { pageSelection ->
                                    selectDocumentScope(pageSelection)
                                    showControlsOverlay = false
                                }
                            }
                        ) {
                            Text("Select Page")
                        }
                        TextButton(
                            onClick = {
                                documentIndex.documentRange?.let { chapterSelection ->
                                    selectDocumentScope(chapterSelection.copy(scope = SelectionScope.CHAPTER))
                                    showControlsOverlay = false
                                }
                            }
                        ) {
                            Text("Select Chapter")
                        }
                        TextButton(
                            onClick = {
                                documentIndex.documentRange?.let { documentSelection ->
                                    selectDocumentScope(documentSelection.copy(scope = SelectionScope.DOCUMENT))
                                    showControlsOverlay = false
                                }
                            }
                        ) {
                            Text("Select Document")
                        }
                    }

                    // Bottom Navigation Bar Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                        }

                        Text(
                            text = "Tap center to toggle controls",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < totalDisplayPages - 1) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < totalDisplayPages - 1
                        ) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Page")
                        }
                    }
                }
            }

            // Edit Page Text Dialog (Pencil Icon)
            if (showEditDialog) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Page Content", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column {
                            Text(
                                "Edit, replace, or delete any word or text on this page:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = currentEditingText,
                                onValueChange = { currentEditingText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onUpdatePageText(chapter.id, pagerState.currentPage, currentEditingText)
                                showEditDialog = false
                            }
                        ) {
                            Text("Save Changes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // AI Reader Assistant Dialog (AI Icon)
            if (showAiDialog) {
                val currentPageText = getRawTextForCurrentPage()
                AlertDialog(
                    onDismissRequest = { if (!isAiRunning) showAiDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Reader Assistant", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column {
                            Text(
                                "Select an AI action or enter a prompt for this page:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val quickPrompts = listOf(
                                "🎓 Syllabus Notes" to "You are an AI study tutor for '${chapter.title}'. Generate comprehensive, practical study notes with bold headings, clear bullet points, key terms, and real-world examples.",
                                "🤖 AI Study Buddy" to "Let's study together on '${chapter.title}'. Tell me which syllabus topic to learn next, explain it in simple practical terms, and generate study notes for it.",
                                "💡 Explain Concept" to "Explain the main concept on this page in simple terms with an example.",
                                "📝 Rewrite & Clear" to "Rewrite this page content to be concise, clear and easy to study.",
                                "🔍 Expand & Detail" to "Provide more context, background facts and examples for this topic.",
                                "🎨 Reformat & Bullet" to "Reformat this text into bullet points with bold key terms."
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = customAiPrompt == quickPrompts[0].second,
                                        onClick = { customAiPrompt = quickPrompts[0].second },
                                        label = { Text(quickPrompts[0].first, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                    FilterChip(
                                        selected = customAiPrompt == quickPrompts[1].second,
                                        onClick = { customAiPrompt = quickPrompts[1].second },
                                        label = { Text(quickPrompts[1].first, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = customAiPrompt == quickPrompts[2].second,
                                        onClick = { customAiPrompt = quickPrompts[2].second },
                                        label = { Text(quickPrompts[2].first, fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = customAiPrompt == quickPrompts[3].second,
                                        onClick = { customAiPrompt = quickPrompts[3].second },
                                        label = { Text(quickPrompts[3].first, fontSize = 11.sp) }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = customAiPrompt == quickPrompts[4].second,
                                        onClick = { customAiPrompt = quickPrompts[4].second },
                                        label = { Text(quickPrompts[4].first, fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = customAiPrompt == quickPrompts[5].second,
                                        onClick = { customAiPrompt = quickPrompts[5].second },
                                        label = { Text(quickPrompts[5].first, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = customAiPrompt,
                                onValueChange = { customAiPrompt = it },
                                placeholder = { Text("Custom AI prompt...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                shape = RoundedCornerShape(10.dp)
                            )

                            if (isAiRunning) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI is processing page text...", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            if (aiOutputText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("✨ AI Result:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(aiOutputText, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Column {
                            if (aiOutputText.isBlank()) {
                                Button(
                                    onClick = {
                                        if (customAiPrompt.isNotBlank()) {
                                            isAiRunning = true
                                            onRunAiAction(customAiPrompt, currentPageText) { res ->
                                                aiOutputText = res
                                                isAiRunning = false
                                            }
                                        }
                                    },
                                    enabled = !isAiRunning && customAiPrompt.isNotBlank()
                                ) {
                                    Text("Run AI Action")
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            onAppendCallout(chapter.id, pagerState.currentPage, "AI Insight", aiOutputText)
                                            showAiDialog = false
                                        }
                                    ) {
                                        Text("Add Callout Box")
                                    }
                                    Button(
                                        onClick = {
                                            onUpdatePageText(chapter.id, pagerState.currentPage, aiOutputText)
                                            showAiDialog = false
                                        }
                                    ) {
                                        Text("Replace Page")
                                    }
                                }
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showAiDialog = false },
                            enabled = !isAiRunning
                        ) {
                            Text("Close")
                        }
                    }
                )
            }

            // Text Reformatting & Alignment Dialog
            if (showFormattingDialog) {
                AlertDialog(
                    onDismissRequest = { showFormattingDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reformat & Arrange Text", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            // Section 0: Scope Selection
                            Text(
                                text = "Apply Formatting To:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = formattingScopeMode == 0,
                                    onClick = { formattingScopeMode = 0 },
                                    label = { Text("🌐 Entire Page", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                                val selectedLineLabel = selectedElementIndex?.let { "🎯 Selected Line #${it + 1}" } ?: "🎯 Selected Line"
                                FilterChip(
                                    selected = formattingScopeMode == 1,
                                    onClick = { formattingScopeMode = 1 },
                                    label = { Text(selectedLineLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }

                            if (formattingScopeMode == 1) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (selectedElementIndex != null)
                                            "Targeting Line #${selectedElementIndex!! + 1}. Size & Color below will apply specifically to this line."
                                        else
                                            "Tap any line on the page to select it for targeted formatting.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Helper Lambda for Size selection
                            val applySizeToSelectedLine: (Float) -> Unit = { scale ->
                                textSizeMultiplier = scale
                                if (formattingScopeMode == 1 && selectedElementIndex != null) {
                                    val pageContent = displayPages.getOrNull(pagerState.currentPage)
                                    val elem = pageContent?.elements?.getOrNull(selectedElementIndex!!)
                                    if (elem != null) {
                                        val rawText = when (elem) {
                                            is PageElement.Heading -> elem.text
                                            is PageElement.Paragraph -> elem.text
                                            is PageElement.Callout -> elem.text
                                            is PageElement.RawText -> elem.text
                                            else -> ""
                                        }
                                        val globalIndex = displayPages.take(pagerState.currentPage).sumOf { it.elements.size } + selectedElementIndex!!
                                        val clean = rawText.replace(Regex("(?i)<size scale=\"[^\"]+\">|</size>"), "")
                                        val formatted = "<size scale=\"$scale\">$clean</size>"
                                        onUpdateElementText(chapter.id, globalIndex, rawText, formatted)
                                    }
                                }
                            }

                            // Helper Lambda for Color selection
                            val applyColorToSelectedLine: (String) -> Unit = { key ->
                                selectedColorKey = key
                                if (formattingScopeMode == 1 && selectedElementIndex != null) {
                                    val pageContent = displayPages.getOrNull(pagerState.currentPage)
                                    val elem = pageContent?.elements?.getOrNull(selectedElementIndex!!)
                                    if (elem != null) {
                                        val rawText = when (elem) {
                                            is PageElement.Heading -> elem.text
                                            is PageElement.Paragraph -> elem.text
                                            is PageElement.Callout -> elem.text
                                            is PageElement.RawText -> elem.text
                                            else -> ""
                                        }
                                        val globalIndex = displayPages.take(pagerState.currentPage).sumOf { it.elements.size } + selectedElementIndex!!
                                        val clean = rawText.replace(Regex("(?i)<font color=\"[^\"]+\">|</font>"), "")
                                        val hex = when (key) {
                                            "OCEAN_BLUE" -> "#1565C0"
                                            "EMERALD_GREEN" -> "#2E7D32"
                                            "ROYAL_VIOLET" -> "#6A1B9A"
                                            "AMBER_GOLD" -> "#FF8F00"
                                            "CRIMSON" -> "#C62828"
                                            "SEPIA_DARK" -> "#3E2723"
                                            "DARK_SLATE" -> "#E0E0E0"
                                            else -> "DEFAULT"
                                        }
                                        val formatted = if (hex == "DEFAULT") clean else "<font color=\"$hex\">$clean</font>"
                                        onUpdateElementText(chapter.id, globalIndex, rawText, formatted)
                                    }
                                }
                            }

                            // Section 1: Text Size
                            Text(
                                text = "Text Size",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val sizes = listOf(
                                    "Small" to 0.85f,
                                    "Normal" to 1.0f,
                                    "Medium" to 1.2f,
                                    "Large" to 1.4f,
                                    "XL" to 1.7f
                                )
                                sizes.forEach { (label, scale) ->
                                    val isSelected = Math.abs(textSizeMultiplier - scale) < 0.05f
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { applySizeToSelectedLine(scale) },
                                        label = { Text(label, fontSize = 10.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = textSizeMultiplier,
                                    onValueChange = { applySizeToSelectedLine(it) },
                                    valueRange = 0.7f..2.0f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                )
                                Text("A+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Section 2: Text Color
                            Text(
                                text = "Text Color",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val colors = listOf(
                                Triple("DEFAULT", "Default", Color.Unspecified),
                                Triple("SEPIA_DARK", "Sepia Coffee", Color(0xFF3E2723)),
                                Triple("DARK_SLATE", "Slate", Color(0xFFE0E0E0)),
                                Triple("OCEAN_BLUE", "Ocean Blue", Color(0xFF1565C0)),
                                Triple("EMERALD_GREEN", "Emerald", Color(0xFF2E7D32)),
                                Triple("ROYAL_VIOLET", "Violet", Color(0xFF6A1B9A)),
                                Triple("AMBER_GOLD", "Amber", Color(0xFFFF8F00)),
                                Triple("CRIMSON", "Crimson", Color(0xFFC62828))
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    colors.take(4).forEach { (key, label, _) ->
                                        FilterChip(
                                            selected = selectedColorKey == key,
                                            onClick = { applyColorToSelectedLine(key) },
                                            label = { Text(label, fontSize = 10.sp) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    colors.drop(4).forEach { (key, label, _) ->
                                        FilterChip(
                                            selected = selectedColorKey == key,
                                            onClick = { applyColorToSelectedLine(key) },
                                            label = { Text(label, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Section 3: Arrange Text / Position (Side vs Center)
                            Text(
                                text = "Text Position & Alignment",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val alignments = listOf(
                                    Triple(TextAlign.Start, "Side (Left)", Icons.Default.FormatAlignLeft),
                                    Triple(TextAlign.Center, "Center", Icons.Default.FormatAlignCenter),
                                    Triple(TextAlign.End, "Side (Right)", Icons.Default.FormatAlignRight),
                                    Triple(TextAlign.Justify, "Justify", Icons.Default.FormatAlignJustify)
                                )

                                alignments.forEach { (align, label, icon) ->
                                    val isSelected = selectedTextAlign == align
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedTextAlign = align },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(label, fontSize = 10.sp)
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Section 4: System Status Bar Visibility (Located below front camera cutout)
                            Text(
                                text = "Status Bar Visibility",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Show System Status Bar", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Keep status bar visible below front camera cutout (approx 5-6px space)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = showStatusBarAlways,
                                    onCheckedChange = { showStatusBarAlways = it }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showFormattingDialog = false }) {
                            Text("Done")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                textSizeMultiplier = 1.0f
                                selectedColorKey = "DEFAULT"
                                selectedTextAlign = TextAlign.Start
                            }
                        ) {
                            Text("Reset")
                        }
                    }
                )
            }
            if (showAskAiSheet && selectedTextForAi.isNotBlank()) {
                AskAiBottomSheet(
                    selectedText = selectedTextForAi,
                    selectedElementIndex = selectedElementIndexForAi,
                    onDismissRequest = {
                        showAskAiSheet = false
                        replaceSelectedTextFromAi = null
                        askAiInitialPrompt = ""
                    },
                    onRunAiAction = onRunAiAction,
                    onInsertElementAfter = { newElem ->
                        val targetIdx = selectedElementIndexForAi ?: 0
                        onInsertElementAfter(chapter.id, targetIdx, newElem)
                    },
                    onReplaceElementAt = { newElem ->
                        val targetIdx = selectedElementIndexForAi ?: 0
                        onReplaceElementAt(chapter.id, targetIdx, newElem)
                    },
                    onReplaceSelectedText = replaceSelectedTextFromAi,
                    initialPrompt = askAiInitialPrompt
                )
            }
        }
    }
}

fun cleanAiResponseForNotes(text: String): String {
    var cleaned = text.trim()
    val introRegexes = listOf(
        Regex("(?i)^(sure|certainly|here is|here's|below is|i have|hope this|as requested)[^\n]*:\n*"),
        Regex("(?i)^(sure|certainly|here is|here's|below is|i have|hope this|as requested)[^\n]*\n+"),
        Regex("(?i)^here (is|are) (a|the) (simplified|summary|explanation|notes|key points|bullet points)[^\n]*:\n*")
    )
    for (regex in introRegexes) {
        cleaned = cleaned.replace(regex, "").trim()
    }
    val outroRegex = Regex("(?i)\n+(hope this helps|let me know if you need|feel free to ask|good luck|happy studying|all the best)[^\n]*$")
    cleaned = cleaned.replace(outroRegex, "").trim()
    return cleaned
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskAiBottomSheet(
    selectedText: String,
    selectedElementIndex: Int?,
    onDismissRequest: () -> Unit,
    onRunAiAction: (prompt: String, contextText: String, callback: (String) -> Unit) -> Unit,
    onInsertElementAfter: (PageElement) -> Unit,
    onReplaceElementAt: (PageElement) -> Unit,
    onReplaceSelectedText: ((String) -> Unit)? = null,
    initialPrompt: String = ""
) {
    var userQuestion by remember(initialPrompt) { mutableStateOf(initialPrompt) }
    var isAiRunning by remember { mutableStateOf(false) }
    var aiResponseText by remember { mutableStateOf("") }
    var generatedStructuredDiagram by remember { mutableStateOf<PageElement.DiagramData?>(null) }
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ask AI About Selected Text 🤖",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 100.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = userQuestion,
                onValueChange = { userQuestion = it },
                label = { Text("What do you want to know?") },
                placeholder = { Text("Explain this... Simplify... Convert to Visual Diagram 🖼️...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val suggestions = listOf(
                    "Explain this simply",
                    "Simplify for exam",
                    "Bullet points summary",
                    "Convert to Visual Diagram 🖼️"
                )
                suggestions.forEach { suggestion ->
                    FilterChip(
                        selected = userQuestion == suggestion,
                        onClick = { userQuestion = suggestion },
                        label = { Text(suggestion, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isAiRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI is generating response...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Button(
                    onClick = {
                        val isDiagramPrompt = userQuestion.contains("Diagram", ignoreCase = true) || userQuestion.contains("Visual", ignoreCase = true)
                        val promptToUse = if (userQuestion.isNotBlank()) userQuestion else "Explain this concept clearly and concisely for exam study."
                        if (isDiagramPrompt) {
                            val structuredDiagram = LocalContentProcessor.createEducationalDiagramIfUseful(
                                content = selectedText,
                                instruction = promptToUse
                            )
                            if (structuredDiagram != null) {
                                generatedStructuredDiagram = structuredDiagram
                                aiResponseText = structuredDiagram.rawAscii
                                isAiRunning = false
                                return@Button
                            }
                        }
                        isAiRunning = true
                        val systemContext = if (isDiagramPrompt) {
                            "Convert the selected text/concept into clean SVG visual diagram code. Return ONLY valid <svg>...</svg> markup without explanations. Selected text: $selectedText"
                        } else {
                            "You are a helpful exam study assistant. Modify ONLY the selected text/scope below and do not assume permission to change anything outside it. Return replacement-ready text with simple bullet points and clean headings where useful. DO NOT add highlight background tags or colored spans. Format headings cleanly as H1, H2, or H3. Selected text: $selectedText"
                        }
                        onRunAiAction(promptToUse, systemContext) { result ->
                            generatedStructuredDiagram = null
                            aiResponseText = result
                            isAiRunning = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask AI", fontWeight = FontWeight.Bold)
                }
            }

            if (aiResponseText.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                val isSvg = aiResponseText.contains("<svg", ignoreCase = true)
                val structuredDiagram = generatedStructuredDiagram
                val cleanContent = cleanAiResponseForNotes(aiResponseText)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isSvg) "🎨 AI Generated Visual Diagram:" else "🤖 AI Note Output:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (structuredDiagram != null) {
                            DiagramCanvasView(
                                diagram = structuredDiagram,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (isSvg) {
                            val svgMarkup = if (aiResponseText.contains("<svg")) {
                                aiResponseText.substring(aiResponseText.indexOf("<svg"), aiResponseText.lastIndexOf("</svg>") + 6)
                            } else aiResponseText
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    }
                                },
                                update = { webView ->
                                    val html = """
                                        <html><head><style>body { margin: 0; padding: 4px; display: flex; justify-content: center; align-items: center; background: transparent; }</style></head>
                                        <body>$svgMarkup</body></html>
                                    """.trimIndent()
                                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        } else {
                            Text(
                                text = cleanContent,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val newElem = if (structuredDiagram != null) {
                                structuredDiagram
                            } else if (isSvg) {
                                val svgMarkup = if (aiResponseText.contains("<svg")) {
                                    aiResponseText.substring(aiResponseText.indexOf("<svg"), aiResponseText.lastIndexOf("</svg>") + 6)
                                } else aiResponseText
                                PageElement.DiagramData(title = "Visual Diagram 🖼️", rawAscii = svgMarkup)
                            } else {
                                PageElement.Callout(title = "📌 Note", text = cleanContent)
                            }
                            onInsertElementAfter(newElem)
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Insert Below ⬇️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (structuredDiagram == null && !isSvg && onReplaceSelectedText != null) {
                                onReplaceSelectedText.invoke(cleanContent)
                                onDismissRequest()
                                return@OutlinedButton
                            }

                            val newElem = if (structuredDiagram != null) {
                                structuredDiagram
                            } else if (isSvg) {
                                val svgMarkup = if (aiResponseText.contains("<svg")) {
                                    aiResponseText.substring(aiResponseText.indexOf("<svg"), aiResponseText.lastIndexOf("</svg>") + 6)
                                } else aiResponseText
                                PageElement.DiagramData(title = "Visual Diagram 🖼️", rawAscii = svgMarkup)
                            } else {
                                PageElement.Paragraph(text = cleanContent)
                            }
                            onReplaceElementAt(newElem)
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Replace Text 🔄", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(cleanContent))
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}



