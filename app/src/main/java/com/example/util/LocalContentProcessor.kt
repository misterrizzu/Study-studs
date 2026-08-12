package com.example.util

import com.example.data.model.DiagramConnection
import com.example.data.model.DiagramNode
import com.example.data.model.PageContent
import com.example.data.model.PageElement
import com.example.data.remote.ExtractedQuestion
import com.example.data.remote.ProcessedChapterResult

object LocalContentProcessor {

    fun processRawContentNoParsing(
        rawContent: String,
        stylePreset: String = "standard"
    ): ProcessedChapterResult {
        val paragraphs = rawContent.split("\n\n").filter { it.isNotBlank() }
        val elements = if (paragraphs.isNotEmpty()) {
            paragraphs.map { PageElement.Paragraph(it.trim()) }
        } else {
            listOf(PageElement.Paragraph(rawContent.trim()))
        }
        val pages = paginateElements(elements, maxPointsPerPage = 780)
        val rawPageTexts = pages.map { pageContent ->
            pageContent.elements.mapNotNull {
                when (it) {
                    is PageElement.Paragraph -> it.text
                    is PageElement.RawText -> it.text
                    else -> null
                }
            }.joinToString("\n\n")
        }
        return ProcessedChapterResult(
            pages = pages,
            rawPageTexts = rawPageTexts,
            questions = emptyList(),
            topics = listOf("Raw Notes")
        )
    }

    private fun isAsciiDiagramLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false
        val asciiChars = listOf("┌", "┐", "└", "┘", "├", "┤", "┬", "┴", "┼", "│", "─", "═", "▼", "▲", "►", "◄", "+---", "+===", "--->", "===>")
        if (asciiChars.any { trimmed.contains(it) }) return true
        if (trimmed.startsWith("|") || trimmed.count { it == '|' } >= 2) return true
        if (trimmed.startsWith("|—") || trimmed.startsWith("|-")) return true
        return false
    }

    fun createEducationalDiagramIfUseful(
        content: String,
        instruction: String? = null
    ): PageElement.DiagramData? {
        val normalized = content.lowercase()
        val forcedType = instruction?.let { inferDiagramType(it.lowercase(), allowWeakSignal = true) }
        val diagramType = forcedType ?: inferDiagramType(normalized, allowWeakSignal = false) ?: return null
        val items = extractDiagramItems(content, diagramType)
        if (items.size < 3 && forcedType == null) return null

        val nodes = items.take(8).mapIndexed { idx, label ->
            val shape = when (diagramType) {
                "CYCLE" -> "CIRCLE"
                "HIERARCHY", "CONCEPT_MAP" -> if (idx == 0) "DIAMOND" else "RECT"
                else -> "RECT"
            }
            DiagramNode(id = "N${idx + 1}", label = label, type = shape)
        }
        if (nodes.size < 2) return null

        val connections = when (diagramType) {
            "HIERARCHY", "CONCEPT_MAP" -> nodes.drop(1).map { node ->
                DiagramConnection(fromId = nodes.first().id, toId = node.id, label = "")
            }
            "RELATIONSHIP" -> nodes.windowed(2).mapIndexed { idx, pair ->
                DiagramConnection(fromId = pair[0].id, toId = pair[1].id, label = if (idx == 0) "relates to" else "")
            }
            "CYCLE" -> nodes.windowed(2).map { pair ->
                DiagramConnection(fromId = pair[0].id, toId = pair[1].id, label = "")
            } + DiagramConnection(fromId = nodes.last().id, toId = nodes.first().id, label = "repeats")
            "COMPARISON" -> nodes.drop(1).map { node ->
                DiagramConnection(fromId = nodes.first().id, toId = node.id, label = "compare")
            }
            else -> nodes.windowed(2).map { pair ->
                DiagramConnection(fromId = pair[0].id, toId = pair[1].id, label = "")
            }
        }

        return PageElement.DiagramData(
            title = educationalDiagramTitle(diagramType),
            diagramType = diagramType,
            nodes = nodes,
            connections = connections,
            rawAscii = nodes.joinToString(" -> ") { it.label }
        )
    }

    private fun inferDiagramType(text: String, allowWeakSignal: Boolean): String? {
        if (Regex("flowchart|flow chart|process|algorithm|steps?|sequence|input.*output").containsMatchIn(text)) return "FLOWCHART"
        if (Regex("hierarchy|tree|classification|types of|parts of|components of|consists of|family|father|mother|sister|brother|son|daughter").containsMatchIn(text)) return "HIERARCHY"
        if (Regex("relationship|related to|connected to|depends on|interacts with|between").containsMatchIn(text)) return "RELATIONSHIP"
        if (Regex("cycle|water cycle|repeats|loop|evaporation|condensation|precipitation").containsMatchIn(text)) return "CYCLE"
        if (Regex("timeline|history|chronology|first|then|finally|before|after").containsMatchIn(text)) return "TIMELINE"
        if (Regex("compare|comparison|versus| vs |advantages|disadvantages|difference between").containsMatchIn(text)) return "COMPARISON"
        if (Regex("architecture|cpu|alu|control unit|memory|storage|network|operating system|input unit|output unit").containsMatchIn(text)) return "ARCHITECTURE"
        if (Regex("labelled|labeled|label|parts shown|structure of").containsMatchIn(text)) return "LABELED_DIAGRAM"
        return if (allowWeakSignal) "FLOWCHART" else null
    }

    private fun extractDiagramItems(content: String, diagramType: String): List<String> {
        val explicitList = content
            .split(Regex("\\n|;|,|â†’|->|=>"))
            .map { it.trim().replace(Regex("^\\d+[\\.\\)]\\s+|^[\\-*â€¢]\\s+"), "") }
            .filter { it.length in 2..42 }
        if (explicitList.size >= 3) return explicitList

        val keywordItems = when (diagramType) {
            "ARCHITECTURE" -> listOf("Input Unit", "Control Unit", "ALU", "Memory", "Storage", "Output Unit")
                .filter { content.contains(it, ignoreCase = true) }
            "CYCLE" -> listOf("Evaporation", "Condensation", "Precipitation", "Collection")
                .filter { content.contains(it, ignoreCase = true) }
            "FLOWCHART" -> listOf("Input", "Processing", "Storage", "Output")
                .filter { content.contains(it, ignoreCase = true) }
            else -> emptyList()
        }
        if (keywordItems.size >= 2) return keywordItems

        return content
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim().take(42).trim() }
            .filter { it.length in 8..42 }
            .take(5)
    }

    private fun educationalDiagramTitle(diagramType: String): String {
        return when (diagramType) {
            "FLOWCHART" -> "Flowchart"
            "HIERARCHY" -> "Hierarchy Diagram"
            "RELATIONSHIP" -> "Relationship Map"
            "CYCLE" -> "Cycle Diagram"
            "CONCEPT_MAP" -> "Concept Map"
            "TIMELINE" -> "Timeline"
            "COMPARISON" -> "Comparison Diagram"
            "ARCHITECTURE" -> "Architecture Diagram"
            "LABELED_DIAGRAM" -> "Labeled Diagram"
            else -> "Study Diagram"
        }
    }

    fun processContentLocally(
        rawContent: String,
        extractQuestions: Boolean
    ): ProcessedChapterResult {
        val lines = rawContent.lines()
        val allElements = mutableListOf<PageElement>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) {
                i++
                continue
            }

            // 1. Check for Table (| col1 | col2 |)
            if (line.startsWith("|") && line.endsWith("|") && line.length > 2) {
                val tableHeaders = mutableListOf<String>()
                val tableRows = mutableListOf<List<String>>()

                val headerCells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                tableHeaders.addAll(headerCells)
                i++

                if (i < lines.size && lines[i].contains("---")) {
                    i++
                }

                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    val rowCells = lines[i].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    tableRows.add(rowCells)
                    i++
                }
                allElements.add(PageElement.TableData(tableHeaders, tableRows))
                continue
            }

            // 2. Check for ASCII Diagram
            if (isAsciiDiagramLine(line)) {
                val asciiLines = mutableListOf<String>()
                while (i < lines.size) {
                    val current = lines[i]
                    if (isAsciiDiagramLine(current)) {
                        asciiLines.add(current)
                        i++
                    } else if (current.isBlank()) {
                        var j = i + 1
                        while (j < lines.size && lines[j].isBlank()) {
                            j++
                        }
                        if (j < lines.size && isAsciiDiagramLine(lines[j])) {
                            while (i < j) {
                                asciiLines.add(lines[i])
                                i++
                            }
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }

                if (asciiLines.isNotEmpty()) {
                    val rawAscii = asciiLines.joinToString("\n")
                    val nodes = mutableListOf<DiagramNode>()
                    val connections = mutableListOf<DiagramConnection>()
                    var nodeCount = 1
                    for (aLine in asciiLines) {
                        val cleanText = aLine.replace("+", "").replace("-", "").replace("|", "")
                            .replace("┌", "").replace("┐", "").replace("└", "").replace("┘", "")
                            .replace("│", "").replace("─", "").replace("═", "").replace("▼", "").replace("▲", "").trim()
                        if (cleanText.isNotBlank() && !cleanText.contains(">")) {
                            nodes.add(DiagramNode("N$nodeCount", cleanText))
                            if (nodeCount > 1) {
                                connections.add(DiagramConnection("N${nodeCount - 1}", "N$nodeCount", "Next"))
                            }
                            nodeCount++
                        }
                    }
                    allElements.add(PageElement.DiagramData("Structure Diagram", "FLOWCHART", nodes, connections, rawAscii))
                }
                continue
            }

            // 3. Headings (H1, H2, H3 detection)
            val trimmedLine = line.trim()
            val cleanNoBold = trimmedLine.replace("**", "").replace("__", "").trim()

            // Rule H3: Any line ending with ":" (e.g., "Language Translators:", "Assembler:", "Compiler:", "Interpreter:", "Device Drivers:")
            if ((trimmedLine.endsWith(":") || cleanNoBold.endsWith(":")) && cleanNoBold.length in 2..70 &&
                !trimmedLine.startsWith("-") && !trimmedLine.startsWith("*") && !trimmedLine.startsWith("•") && !trimmedLine.startsWith("|")
            ) {
                allElements.add(PageElement.Heading(cleanNoBold, 3))
                i++
                continue
            }

            // Rule H2: Any bold section title (e.g., "**Device Drivers**", "**Operating System**")
            if ((trimmedLine.startsWith("**") && trimmedLine.endsWith("**") && trimmedLine.length in 5..80) ||
                (trimmedLine.startsWith("__") && trimmedLine.endsWith("__") && trimmedLine.length in 5..80)
            ) {
                allElements.add(PageElement.Heading(cleanNoBold, 2))
                i++
                continue
            }

            // Rule H3 with inline text (e.g., "Brief: The primary...", "Core Functions: Manages memory...")
            val inlineLabelMatch = Regex("^([A-Za-z0-9\\.\\s\\-\\(\\)]{1,30}:)\\s+(.+)").find(line)
            if (inlineLabelMatch != null) {
                val label = inlineLabelMatch.groupValues[1]
                val restText = inlineLabelMatch.groupValues[2]
                allElements.add(PageElement.Heading(label, 3))
                val boldTerms = mutableListOf<String>()
                Regex("\\*\\*(.*?)\\*\\*|__(.*?)__").findAll(restText).forEach { match ->
                    val term = match.groupValues[1].ifEmpty { match.groupValues[2] }
                    if (term.isNotBlank()) boldTerms.add(term)
                }
                allElements.add(PageElement.Paragraph(restText, boldTerms))
                i++
                continue
            }

            // Rule H1: Markdown #
            if (line.startsWith("#")) {
                val level = line.takeWhile { it == '#' }.length.coerceIn(1, 3)
                val headingText = line.removePrefix("#".repeat(level)).trim()
                allElements.add(PageElement.Heading(headingText, level))
                i++
                continue
            }

            // Rule H1: ALL CAPS line
            if (line == line.uppercase() && line.length in 3..80 && line.any { it.isLetter() } && !line.startsWith("-") && !line.startsWith("|")) {
                allElements.add(PageElement.Heading(line, 1))
                i++
                continue
            }

            // Rule H2: Letter/Number section headings e.g. "A. Operating System (OS)", "C. Device Drivers"
            if (line.matches(Regex("^[A-Z0-9][\\.\\)]\\s+[A-Za-z0-9\\s\\-\\(\\)]{2,60}$"))) {
                allElements.add(PageElement.Heading(line, 2))
                i++
                continue
            }

            // Rule H2: Short section topic line (under 65 chars), starts with capital, no trailing punctuation like . ? ! ;
            if (line.length in 3..65 && line.first().isUpperCase() &&
                !line.endsWith(".") && !line.endsWith(",") && !line.endsWith(";") && !line.contains("?") && !line.contains("!") &&
                !line.startsWith("- ") && !line.startsWith("* ") && !line.startsWith("• ") && !line.matches(Regex("^\\d+[\\.\\)]\\s+.*"))
            ) {
                allElements.add(PageElement.Heading(line.removeSuffix(":"), 2))
                i++
                continue
            }

            // 4. Bullet List (•, -, *)
            if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")) {
                val listItems = mutableListOf<String>()
                while (i < lines.size && (lines[i].trim().startsWith("- ") || lines[i].trim().startsWith("* ") || lines[i].trim().startsWith("• "))) {
                    val itemText = lines[i].trim().substring(2).trim()
                    listItems.add(itemText)
                    i++
                }
                allElements.add(PageElement.BulletList(listItems))
                createEducationalDiagramIfUseful(listItems.joinToString(". "), null)?.let { allElements.add(it) }
                continue
            }

            // 5. Numbered List (1., 1), 2.)
            if (line.matches(Regex("^\\d+[\\.\\)]\\s+.*"))) {
                val listItems = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().matches(Regex("^\\d+[\\.\\)]\\s+.*"))) {
                    val itemText = lines[i].trim().replaceFirst(Regex("^\\d+[\\.\\)]\\s+"), "")
                    listItems.add(itemText)
                    i++
                }
                allElements.add(PageElement.NumberedList(listItems))
                createEducationalDiagramIfUseful(listItems.joinToString(". "), null)?.let { allElements.add(it) }
                continue
            }

            // 6. Callouts (Note:, Warning:, Important:)
            if (line.startsWith("Note:", ignoreCase = true) || line.startsWith("Warning:", ignoreCase = true) || line.startsWith("Important:", ignoreCase = true)) {
                val colonIndex = line.indexOf(":")
                val title = line.substring(0, colonIndex).trim()
                val text = line.substring(colonIndex + 1).trim()
                val type = when {
                    title.equals("Warning", ignoreCase = true) -> "WARNING"
                    title.equals("Important", ignoreCase = true) -> "HIGHLIGHT"
                    else -> "INFO"
                }
                allElements.add(PageElement.Callout(title, text, type))
                i++
                continue
            }

            // 7. Regular Paragraph
            val paraText = StringBuilder(line)
            i++
            while (i < lines.size && lines[i].trim().isNotEmpty() &&
                !lines[i].trim().startsWith("#") &&
                !lines[i].trim().startsWith("|") &&
                !lines[i].trim().startsWith("- ") &&
                !lines[i].trim().startsWith("* ") &&
                !lines[i].trim().startsWith("• ") &&
                !lines[i].trim().matches(Regex("^\\d+[\\.\\)]\\s+.*")) &&
                !(lines[i].trim() == lines[i].trim().uppercase() && lines[i].trim().length in 3..80 && lines[i].trim().any { it.isLetter() })
            ) {
                paraText.append(" ").append(lines[i].trim())
                i++
            }

            val fullPara = paraText.toString()
            // Extract bold terms from **text**
            val boldTerms = mutableListOf<String>()
            val regex = Regex("\\*\\*(.*?)\\*\\*|__(.*?)__")
            regex.findAll(fullPara).forEach { match ->
                val term = match.groupValues[1].ifEmpty { match.groupValues[2] }
                if (term.isNotBlank()) boldTerms.add(term)
            }

            allElements.add(PageElement.Paragraph(fullPara, boldTerms))
            createEducationalDiagramIfUseful(fullPara, null)?.let { allElements.add(it) }
        }

        // Split elements into screen-height fitting pages (Max ~420 height points per page)
        val pages = paginateElements(allElements)
        val rawPageTexts = pages.map { page ->
            page.elements.joinToString("\n") { elem ->
                when (elem) {
                    is PageElement.Heading -> elem.text
                    is PageElement.Paragraph -> elem.text
                    is PageElement.BulletList -> elem.items.joinToString("\n") { "• $it" }
                    is PageElement.NumberedList -> elem.items.mapIndexed { idx, it -> "${idx + 1}. $it" }.joinToString("\n")
                    is PageElement.DiagramData -> elem.rawAscii
                    is PageElement.Callout -> "${elem.title}: ${elem.text}"
                    else -> ""
                }
            }
        }

        // Extract questions locally if needed
        val questions = mutableListOf<ExtractedQuestion>()
        if (extractQuestions) {
            val paragraphs = allElements.filterIsInstance<PageElement.Paragraph>()
            val headings = allElements.filterIsInstance<PageElement.Heading>()

            for (idx in 0 until minOf(5, paragraphs.size)) {
                val pText = paragraphs[idx].text.replace("**", "")
                val hText = headings.getOrNull(idx)?.text ?: "Study Topic"
                questions.add(
                    ExtractedQuestion(
                        questionText = "Regarding $hText: What is the key concept discussed?",
                        optionA = if (pText.length > 50) pText.take(50) + "..." else pText,
                        optionB = "Incorrect option statement for option B",
                        optionC = "Alternative incorrect answer C",
                        optionD = "None of the above",
                        correctOption = 0,
                        explanation = "Directly referenced from the reading material under $hText."
                    )
                )
            }
        }

        // Extract topics locally
        val autoTopics = mutableListOf<String>()
        allElements.forEach { elem ->
            when (elem) {
                is PageElement.Heading -> {
                    val clean = elem.text.trim().removeSuffix(":")
                    if (clean.length in 3..35 && !autoTopics.any { it.equals(clean, ignoreCase = true) }) {
                        autoTopics.add(clean)
                    }
                }
                is PageElement.Callout -> {
                    if (elem.title.length in 3..25 && !autoTopics.any { it.equals(elem.title, ignoreCase = true) }) {
                        autoTopics.add(elem.title)
                    }
                }
                else -> {}
            }
        }

        if (autoTopics.isEmpty()) {
            lines.map { it.trim() }
                .filter { it.isNotBlank() && it.length in 3..30 && !it.contains(".") && !it.contains("{") }
                .take(3)
                .forEach { line ->
                    val clean = line.removePrefix("#").trim()
                    if (clean.isNotBlank() && !autoTopics.any { it.equals(clean, ignoreCase = true) }) {
                        autoTopics.add(clean)
                    }
                }
        }

        return ProcessedChapterResult(pages, rawPageTexts, questions, autoTopics.take(5))
    }

    /**
     * Splits list of PageElements into screen-fitting pages so no vertical scrolling occurs.
     */
    fun paginateElements(elements: List<PageElement>, maxPointsPerPage: Int = 780): List<PageContent> {
        val pages = mutableListOf<PageContent>()
        var currentElements = mutableListOf<PageElement>()
        var currentPoints = 0

        for (elem in elements) {
            val elemPoints = estimateElementPoints(elem)

            // If a paragraph alone or combined exceeds maxPointsPerPage, break into sentence chunks
            if (elem is PageElement.Paragraph && (elemPoints > 150 || currentPoints + elemPoints > maxPointsPerPage)) {
                val sentences = elem.text.split(Regex("(?<=[.!?])\\s+"))
                var currentChunk = StringBuilder()

                for (sentence in sentences) {
                    if (sentence.isBlank()) continue
                    val testChunk = if (currentChunk.isEmpty()) sentence else "$currentChunk $sentence"
                    val testElem = PageElement.Paragraph(testChunk, elem.highlightedTerms)
                    val testPoints = estimateElementPoints(testElem)

                    if (currentPoints + testPoints > maxPointsPerPage && currentElements.isNotEmpty()) {
                        // Complete current page with chunk collected so far
                        val chunkText = currentChunk.toString().trim()
                        if (chunkText.isNotEmpty()) {
                            currentElements.add(PageElement.Paragraph(chunkText, elem.highlightedTerms))
                        }
                        pages.add(PageContent(currentElements))
                        currentElements = mutableListOf()
                        currentPoints = 0
                        currentChunk = StringBuilder(sentence)
                    } else {
                        if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                        currentChunk.append(sentence)
                    }
                }

                if (currentChunk.isNotEmpty()) {
                    val chunkText = currentChunk.toString().trim()
                    val chunkElem = PageElement.Paragraph(chunkText, elem.highlightedTerms)
                    val chunkPoints = estimateElementPoints(chunkElem)
                    if (currentPoints + chunkPoints > maxPointsPerPage && currentElements.isNotEmpty()) {
                        pages.add(PageContent(currentElements))
                        currentElements = mutableListOf()
                        currentPoints = 0
                    }
                    currentElements.add(chunkElem)
                    currentPoints += chunkPoints
                }
                continue
            }

            // If adding this element exceeds page capacity, wrap to next page
            if (currentPoints + elemPoints > maxPointsPerPage && currentElements.isNotEmpty()) {
                pages.add(PageContent(currentElements))
                currentElements = mutableListOf()
                currentPoints = 0
            }

            currentElements.add(elem)
            currentPoints += elemPoints
        }

        if (currentElements.isNotEmpty()) {
            pages.add(PageContent(currentElements))
        }

        return if (pages.isEmpty()) listOf(PageContent(listOf(PageElement.Paragraph("No content", emptyList())))) else pages
    }

    private fun estimateElementPoints(elem: PageElement): Int {
        return when (elem) {
            is PageElement.Heading -> when (elem.level) {
                1 -> 45
                2 -> 35
                else -> 25
            }
            is PageElement.Paragraph -> (elem.text.length / 45 * 18) + 15
            is PageElement.BulletList -> elem.items.sumOf { (it.length / 45 * 18) + 15 } + 10
            is PageElement.NumberedList -> elem.items.sumOf { (it.length / 45 * 18) + 15 } + 10
            is PageElement.DiagramData -> (elem.rawAscii.lines().size * 16) + 30
            is PageElement.Callout -> (elem.text.length / 45 * 18) + 30
            is PageElement.TableData -> (elem.rows.size * 25) + 30
            is PageElement.RawText -> (elem.text.length / 45 * 18) + 15
        }
    }

    /**
     * Generates a clean fallback SVG diagram for DiagramData elements.
     */
    fun generateFallbackSvg(diagram: PageElement.DiagramData): String {
        val title = if (diagram.title.isNotBlank()) diagram.title else "Structure Diagram"
        val lines = diagram.rawAscii.lines().filter { it.isNotBlank() }
        val boxCount = lines.size.coerceIn(1, 8)
        val svgHeight = (boxCount * 50) + 60

        val boxesHtml = StringBuilder()
        lines.forEachIndexed { i, line ->
            val y = 40 + (i * 50)
            val cleanLine = line.replace(Regex("[+|\\-=>]"), "").trim().ifBlank { "Node ${i + 1}" }
            boxesHtml.append("""
                <rect x="20" y="$y" width="280" height="36" rx="8" fill="#1E293B" stroke="#3B82F6" stroke-width="2"/>
                <text x="160" y="${y + 22}" font-family="sans-serif" font-size="13" font-weight="bold" fill="#FFFFFF" text-anchor="middle">$cleanLine</text>
            """.trimIndent())
            if (i < lines.size - 1) {
                boxesHtml.append("""
                    <line x1="160" y1="${y + 36}" x2="160" y2="${y + 50}" stroke="#60A5FA" stroke-width="2" stroke-dasharray="3,3"/>
                """.trimIndent())
            }
        }

        return """
            <svg width="320" height="$svgHeight" xmlns="http://www.w3.org/2000/svg">
              <rect width="100%" height="100%" fill="#0F172A" rx="12"/>
              <text x="160" y="24" font-family="sans-serif" font-size="14" font-weight="bold" fill="#60A5FA" text-anchor="middle">$title</text>
              $boxesHtml
            </svg>
        """.trimIndent()
    }
}

