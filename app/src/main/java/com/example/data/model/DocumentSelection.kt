package com.example.data.model

import com.example.data.local.PageEntity

data class DocumentPosition(
    val blockIndex: Int,
    val localOffset: Int
)

enum class SelectionScope {
    TEXT_RANGE,
    BLOCK_RANGE,
    PAGE,
    CHAPTER,
    DOCUMENT
}

data class DocumentSelection(
    val start: DocumentPosition,
    val end: DocumentPosition,
    val scope: SelectionScope = SelectionScope.TEXT_RANGE
) {
    fun normalized(index: LogicalDocumentIndex): DocumentSelection {
        val startBlock = index.blocks.getOrNull(start.blockIndex) ?: return this
        val endBlock = index.blocks.getOrNull(end.blockIndex) ?: return this
        val startGlobal = startBlock.globalStart + start.localOffset.coerceIn(0, startBlock.text.length)
        val endGlobal = endBlock.globalStart + end.localOffset.coerceIn(0, endBlock.text.length)
        return if (startGlobal <= endGlobal) this else copy(start = end, end = start)
    }
}

data class SelectableBlock(
    val pageIndex: Int,
    val pageId: Long,
    val pageElementIndex: Int,
    val documentElementIndex: Int,
    val blockIndex: Int,
    val element: PageElement,
    val text: String,
    val globalStart: Int,
    val globalEnd: Int
)

data class LogicalDocumentIndex(
    val blocks: List<SelectableBlock>,
    val pageRanges: Map<Int, DocumentSelection>,
    val documentRange: DocumentSelection?
) {
    fun textForSelection(selection: DocumentSelection): String {
        if (blocks.isEmpty()) return ""
        val normalized = selection.normalized(this)
        val startBlock = blocks.getOrNull(normalized.start.blockIndex) ?: return ""
        val endBlock = blocks.getOrNull(normalized.end.blockIndex) ?: return ""
        val startOffset = normalized.start.localOffset.coerceIn(0, startBlock.text.length)
        val endOffset = normalized.end.localOffset.coerceIn(0, endBlock.text.length)

        return blocks
            .filter { it.blockIndex in startBlock.blockIndex..endBlock.blockIndex }
            .joinToString("\n\n") { block ->
                when (block.blockIndex) {
                    startBlock.blockIndex -> {
                        if (startBlock.blockIndex == endBlock.blockIndex) {
                            block.text.substring(startOffset, endOffset.coerceAtLeast(startOffset))
                        } else {
                            block.text.substring(startOffset)
                        }
                    }
                    endBlock.blockIndex -> block.text.substring(0, endOffset)
                    else -> block.text
                }
            }
    }

    companion object {
        fun build(
            pages: List<PageEntity>,
            parsePageContent: (String) -> PageContent
        ): LogicalDocumentIndex {
            val blocks = mutableListOf<SelectableBlock>()
            val pageRanges = mutableMapOf<Int, DocumentSelection>()
            var globalOffset = 0
            var blockIndex = 0
            var documentElementIndex = 0

            pages.sortedBy { it.pageIndex }.forEach { page ->
                val firstBlockForPage = blockIndex
                val content = parsePageContent(page.contentJson)
                content.elements.forEachIndexed { elementIndex, element ->
                    val text = element.selectableText()
                    if (text != null) {
                        val start = globalOffset
                        val end = start + text.length
                        blocks.add(
                            SelectableBlock(
                                pageIndex = page.pageIndex,
                                pageId = page.id,
                                pageElementIndex = elementIndex,
                                documentElementIndex = documentElementIndex,
                                blockIndex = blockIndex,
                                element = element,
                                text = text,
                                globalStart = start,
                                globalEnd = end
                            )
                        )
                        blockIndex++
                        globalOffset = end + DOCUMENT_BLOCK_SEPARATOR_LENGTH
                    }
                    documentElementIndex++
                }

                if (blockIndex > firstBlockForPage) {
                    val lastBlock = blockIndex - 1
                    pageRanges[page.pageIndex] = DocumentSelection(
                        start = DocumentPosition(firstBlockForPage, 0),
                        end = DocumentPosition(lastBlock, blocks[lastBlock].text.length),
                        scope = SelectionScope.PAGE
                    )
                }
            }

            val documentRange = blocks.firstOrNull()?.let {
                val last = blocks.last()
                DocumentSelection(
                    start = DocumentPosition(it.blockIndex, 0),
                    end = DocumentPosition(last.blockIndex, last.text.length),
                    scope = SelectionScope.DOCUMENT
                )
            }

            return LogicalDocumentIndex(blocks, pageRanges, documentRange)
        }
    }
}

const val DOCUMENT_BLOCK_SEPARATOR_LENGTH = 2

fun PageElement.selectableText(): String? = when (this) {
    is PageElement.Heading -> text
    is PageElement.Paragraph -> text
    is PageElement.BulletList -> items.joinToString("\n")
    is PageElement.NumberedList -> items.joinToString("\n")
    is PageElement.Callout -> text
    is PageElement.RawText -> text
    else -> null
}

fun PageElement.withSelectableText(newText: String): PageElement = when (this) {
    is PageElement.Heading -> copy(text = newText)
    is PageElement.Paragraph -> copy(text = newText)
    is PageElement.BulletList -> copy(items = newText.lines().filter { it.isNotBlank() })
    is PageElement.NumberedList -> copy(items = newText.lines().filter { it.isNotBlank() })
    is PageElement.Callout -> copy(text = newText)
    is PageElement.RawText -> copy(text = newText)
    else -> this
}
