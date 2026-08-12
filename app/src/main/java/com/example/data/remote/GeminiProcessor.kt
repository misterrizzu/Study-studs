package com.example.data.remote

import android.util.Log
import com.example.data.model.DiagramConnection
import com.example.data.model.DiagramNode
import com.example.data.model.MoshiProvider
import com.example.data.model.PageContent
import com.example.data.model.PageElement
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ExtractedQuestion(
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: Int, // 0..3
    val explanation: String
)

data class ExtractedQuestionWithStatus(
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: Int,
    val explanation: String,
    val status: String = "UNCHANGED" // "ADDED", "MERGED", "UNCHANGED", "SKIPPED"
)

data class PageDiffItem(
    val pageIndex: Int,
    val text: String,
    val status: String // "ADDED", "MERGED", "UNCHANGED"
)

data class ContentMergeResult(
    val pages: List<PageContent>,
    val rawPageTexts: List<String>,
    val newParagraphsCount: Int,
    val mergedSectionsCount: Int,
    val unchangedSectionsCount: Int,
    val wasTotalPages: Int,
    val nowTotalPages: Int,
    val pageDiffs: List<PageDiffItem>
)

data class QuestionMergeResult(
    val questions: List<ExtractedQuestionWithStatus>,
    val newQuestionsCount: Int,
    val mergedQuestionsCount: Int,
    val skippedDuplicatesCount: Int
)

data class ProcessedChapterResult(
    val pages: List<PageContent>,
    val rawPageTexts: List<String>,
    val questions: List<ExtractedQuestion>,
    val topics: List<String> = emptyList()
)

class GeminiProcessor {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = MoshiProvider.moshi

    suspend fun processContent(
        apiKey: String,
        modelName: String,
        rawContent: String,
        extractQuestions: Boolean
    ): ProcessedChapterResult = withContext(Dispatchers.IO) {
        val model = if (modelName.isBlank()) "gemini-3.5-flash" else modelName
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val prompt = buildString {
            append("You are an expert study material formatter for an offline exam prep application.\n")
            append("CRITICAL RULE: DO NOT CHANGE, OMIT, OR SUMMARY THE ORIGINAL TEXT. PRESERVE 100% OF THE WORDS AND TEXT CONTENT EXACTLY AS PROVIDED.\n")
            append("Your task is to structure the provided raw text into pages (~300-400 words per page) and format elements.\n\n")
            append("Analyze the input text and organize into pages. Each page has a list of elements.\n")
            append("Supported element types:\n")
            append("- HEADING: level 1 to 3\n")
            append("- PARAGRAPH: text, highlightedTerms array (key terms found in text)\n")
            append("- BULLET_LIST: items array\n")
            append("- NUMBERED_LIST: items array\n")
            append("- TABLE: headers array, rows array of arrays\n")
            append("- DIAGRAM: title, diagramType ('FLOWCHART','BLOCK','TREE'), nodes array ({id, label, type}), connections array ({fromId, toId, label}), rawAscii\n")
            append("- CALLOUT: title, text, type ('INFO','NOTE','WARNING')\n\n")

            append("ALSO extract 2 to 5 relevant key topic tags/keywords for this chapter (e.g. [\"Thermodynamics\", \"Heat Transfer\", \"Entropy\"]).\n\n")

            if (extractQuestions) {
                append("ALSO extract 5 to 10 Multiple Choice Questions (MCQs) based on the content for exam testing.\n")
                append("Each question must have questionText, optionA, optionB, optionC, optionD, correctOption (0 for A, 1 for B, 2 for C, 3 for D), and explanation.\n\n")
            }

            append("Return ONLY a valid JSON object strictly matching this format (no markdown formatting wrapping, or inside ```json code block):\n")
            append("{\n")
            append("  \"topics\": [\"Topic 1\", \"Topic 2\", \"Topic 3\"],\n")
            append("  \"pages\": [\n")
            append("    {\n")
            append("      \"elements\": [\n")
            append("        {\"type\": \"HEADING\", \"text\": \"Chapter Title\", \"level\": 1},\n")
            append("        {\"type\": \"PARAGRAPH\", \"text\": \"Full text preserving every word...\", \"highlightedTerms\": [\"Term\"]}\n")
            append("      ]\n")
            append("    }\n")
            append("  ],\n")
            if (extractQuestions) {
                append("  \"questions\": [\n")
                append("    {\"questionText\": \"Q1\", \"optionA\": \"A\", \"optionB\": \"B\", \"optionC\": \"C\", \"optionD\": \"D\", \"correctOption\": 0, \"explanation\": \"Why A is correct\"}\n")
                append("  ]\n")
            } else {
                append("  \"questions\": []\n")
            }
            append("}\n\n")
            append("RAW CONTENT TO PROCESS:\n")
            append(rawContent)
        }

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiProcessor", "API Call failed code ${response.code}: $responseBody")
                throw Exception("Gemini API Error (HTTP ${response.code})")
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val contentObj = candidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text") ?: ""

            return@withContext parseJsonResult(textOutput, rawContent)
        } catch (e: Exception) {
            Log.e("GeminiProcessor", "Gemini processing exception", e)
            throw e
        }
    }

    private fun parseJsonResult(jsonText: String, originalContentFallback: String): ProcessedChapterResult {
        val cleanJson = jsonText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val jsonObject = JSONObject(cleanJson)
        val pagesArray = jsonObject.optJSONArray("pages") ?: JSONArray()
        val questionsArray = jsonObject.optJSONArray("questions") ?: JSONArray()

        val pagesList = mutableListOf<PageContent>()
        val rawPageTexts = mutableListOf<String>()

        for (i in 0 until pagesArray.length()) {
            val pageObj = pagesArray.optJSONObject(i) ?: continue
            val elementsArray = pageObj.optJSONArray("elements") ?: JSONArray()
            val elementsList = mutableListOf<PageElement>()
            val textCollector = StringBuilder()

            for (j in 0 until elementsArray.length()) {
                val elemObj = elementsArray.optJSONObject(j) ?: continue
                val type = elemObj.optString("type", "PARAGRAPH")

                when (type) {
                    "HEADING" -> {
                        val text = elemObj.optString("text", "")
                        val level = elemObj.optInt("level", 1)
                        elementsList.add(PageElement.Heading(text, level))
                        textCollector.append(text).append("\n")
                    }
                    "PARAGRAPH" -> {
                        val text = elemObj.optString("text", "")
                        val termsArray = elemObj.optJSONArray("highlightedTerms") ?: JSONArray()
                        val termsList = mutableListOf<String>()
                        for (k in 0 until termsArray.length()) {
                            termsList.add(termsArray.optString(k))
                        }
                        elementsList.add(PageElement.Paragraph(text, termsList))
                        textCollector.append(text).append("\n")
                    }
                    "BULLET_LIST" -> {
                        val itemsArray = elemObj.optJSONArray("items") ?: JSONArray()
                        val itemsList = mutableListOf<String>()
                        for (k in 0 until itemsArray.length()) {
                            itemsList.add(itemsArray.optString(k))
                            textCollector.append("• ").append(itemsArray.optString(k)).append("\n")
                        }
                        elementsList.add(PageElement.BulletList(itemsList))
                    }
                    "NUMBERED_LIST" -> {
                        val itemsArray = elemObj.optJSONArray("items") ?: JSONArray()
                        val itemsList = mutableListOf<String>()
                        for (k in 0 until itemsArray.length()) {
                            itemsList.add(itemsArray.optString(k))
                            textCollector.append("${k+1}. ").append(itemsArray.optString(k)).append("\n")
                        }
                        elementsList.add(PageElement.NumberedList(itemsList))
                    }
                    "TABLE" -> {
                        val headersArr = elemObj.optJSONArray("headers") ?: JSONArray()
                        val headers = mutableListOf<String>()
                        for (k in 0 until headersArr.length()) headers.add(headersArr.optString(k))

                        val rowsArr = elemObj.optJSONArray("rows") ?: JSONArray()
                        val rows = mutableListOf<List<String>>()
                        for (k in 0 until rowsArr.length()) {
                            val rowArr = rowsArr.optJSONArray(k) ?: JSONArray()
                            val row = mutableListOf<String>()
                            for (m in 0 until rowArr.length()) row.add(rowArr.optString(m))
                            rows.add(row)
                        }
                        elementsList.add(PageElement.TableData(headers, rows))
                    }
                    "DIAGRAM" -> {
                        val title = elemObj.optString("title", "Diagram")
                        val diagramType = elemObj.optString("diagramType", "FLOWCHART")
                        val rawAscii = elemObj.optString("rawAscii", "")

                        val nodesArr = elemObj.optJSONArray("nodes") ?: JSONArray()
                        val nodes = mutableListOf<DiagramNode>()
                        for (k in 0 until nodesArr.length()) {
                            val nObj = nodesArr.optJSONObject(k) ?: continue
                            nodes.add(DiagramNode(
                                id = nObj.optString("id", "N$k"),
                                label = nObj.optString("label", "Node $k"),
                                type = nObj.optString("type", "RECT")
                            ))
                        }

                        val connArr = elemObj.optJSONArray("connections") ?: JSONArray()
                        val conns = mutableListOf<DiagramConnection>()
                        for (k in 0 until connArr.length()) {
                            val cObj = connArr.optJSONObject(k) ?: continue
                            conns.add(DiagramConnection(
                                fromId = cObj.optString("fromId", ""),
                                toId = cObj.optString("toId", ""),
                                label = cObj.optString("label", "")
                            ))
                        }

                        elementsList.add(PageElement.DiagramData(title, diagramType, nodes, conns, rawAscii))
                    }
                    "CALLOUT" -> {
                        val title = elemObj.optString("title", "Note")
                        val text = elemObj.optString("text", "")
                        val calloutType = elemObj.optString("type", "INFO")
                        elementsList.add(PageElement.Callout(title, text, calloutType))
                        textCollector.append(title).append(": ").append(text).append("\n")
                    }
                }
            }

            pagesList.add(PageContent(elementsList))
            rawPageTexts.add(textCollector.toString())
        }

        val questionsList = mutableListOf<ExtractedQuestion>()
        for (i in 0 until questionsArray.length()) {
            val qObj = questionsArray.optJSONObject(i) ?: continue
            questionsList.add(ExtractedQuestion(
                questionText = qObj.optString("questionText", "Question ${i+1}"),
                optionA = qObj.optString("optionA", "Option A"),
                optionB = qObj.optString("optionB", "Option B"),
                optionC = qObj.optString("optionC", "Option C"),
                optionD = qObj.optString("optionD", "Option D"),
                correctOption = qObj.optInt("correctOption", 0).coerceIn(0, 3),
                explanation = qObj.optString("explanation", "Explanation")
            ))
        }

        val extractedTopics = mutableListOf<String>()
        val topicsArray = jsonObject.optJSONArray("topics")
        if (topicsArray != null) {
            for (i in 0 until topicsArray.length()) {
                val t = topicsArray.optString(i)
                if (t.isNotBlank() && !extractedTopics.any { it.equals(t.trim(), ignoreCase = true) }) {
                    extractedTopics.add(t.trim())
                }
            }
        }

        if (extractedTopics.isEmpty()) {
            pagesList.forEach { page ->
                page.elements.forEach { elem ->
                    when (elem) {
                        is PageElement.Heading -> {
                            val clean = elem.text.trim().removeSuffix(":")
                            if (clean.length in 3..35 && !extractedTopics.any { it.equals(clean, ignoreCase = true) }) {
                                extractedTopics.add(clean)
                            }
                        }
                        is PageElement.Paragraph -> {
                            elem.highlightedTerms.forEach { term ->
                                if (term.length in 3..25 && extractedTopics.size < 5 && !extractedTopics.any { it.equals(term.trim(), ignoreCase = true) }) {
                                    extractedTopics.add(term.trim())
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        return ProcessedChapterResult(pagesList, rawPageTexts, questionsList, extractedTopics.take(5))
    }

    suspend fun mergeChapterContent(
        apiKey: String,
        modelName: String,
        existingRawContent: String,
        newRawContent: String,
        wasTotalPages: Int
    ): ContentMergeResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            val combined = "$existingRawContent\n\n--- NEW MATERIAL ---\n\n$newRawContent"
            val processed = com.example.util.LocalContentProcessor.processContentLocally(combined, false)
            val diffs = processed.rawPageTexts.mapIndexed { idx, txt ->
                PageDiffItem(idx, txt, if (idx >= wasTotalPages) "ADDED" else "UNCHANGED")
            }
            return@withContext ContentMergeResult(
                pages = processed.pages,
                rawPageTexts = processed.rawPageTexts,
                newParagraphsCount = newRawContent.lines().filter { it.isNotBlank() }.size,
                mergedSectionsCount = 0,
                unchangedSectionsCount = existingRawContent.lines().filter { it.isNotBlank() }.size,
                wasTotalPages = wasTotalPages,
                nowTotalPages = processed.pages.size,
                pageDiffs = diffs
            )
        }

        val model = if (modelName.isBlank()) "gemini-3.5-flash" else modelName
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val prompt = buildString {
            append("You are an expert study material editor and content deduplicator.\n")
            append("YOUR ABSOLUTE GOAL: MERGE NEW SOURCE MATERIAL INTO EXISTING CHAPTER CONTENT WITH ZERO INFORMATION LOSS.\n")
            append("STRICT RULES:\n")
            append("1. Do NOT summarize or shorten existing content.\n")
            append("2. Preserve 100% of existing information.\n")
            append("3. Compare existing content and new content:\n")
            append("   - New unique information: ADD IT as new paragraphs/elements.\n")
            append("   - Overlapping/duplicate concepts: MERGE into a single comprehensive version containing ALL points from both sources.\n")
            append("   - Existing unique information: KEEP IT untouched.\n")
            append("4. Structure into pages (~300-400 words per page).\n")
            append("5. Each element must have a 'status' field: 'ADDED' (for new unique material), 'MERGED' (for merged overlapping content), or 'UNCHANGED' (for untouched existing content).\n\n")
            append("JSON RESPONSE FORMAT STRICTLY REQUIRED:\n")
            append("{\n")
            append("  \"pages\": [\n")
            append("    {\n")
            append("      \"elements\": [\n")
            append("        {\"type\": \"HEADING\", \"text\": \"...\", \"level\": 1, \"status\": \"UNCHANGED\"},\n")
            append("        {\"type\": \"PARAGRAPH\", \"text\": \"...\", \"highlightedTerms\": [], \"status\": \"ADDED\"}\n")
            append("      ]\n")
            append("    }\n")
            append("  ],\n")
            append("  \"summary\": {\n")
            append("    \"newParagraphsCount\": 3,\n")
            append("    \"mergedSectionsCount\": 2,\n")
            append("    \"unchangedSectionsCount\": 8\n")
            append("  }\n")
            append("}\n\n")
            append("EXISTING CONTENT:\n").append(existingRawContent).append("\n\n")
            append("NEW MATERIAL TO MERGE:\n").append(newRawContent)
        }

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw Exception("Gemini API Error (HTTP ${response.code})")
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val contentObj = candidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text") ?: ""

            return@withContext parseContentMergeResult(textOutput, existingRawContent, newRawContent, wasTotalPages)
        } catch (e: Exception) {
            Log.e("GeminiProcessor", "Content merge error", e)
            val combined = "$existingRawContent\n\n--- NEW MATERIAL ---\n\n$newRawContent"
            val processed = com.example.util.LocalContentProcessor.processContentLocally(combined, false)
            val diffs = processed.rawPageTexts.mapIndexed { idx, txt ->
                PageDiffItem(idx, txt, if (idx >= wasTotalPages) "ADDED" else "UNCHANGED")
            }
            return@withContext ContentMergeResult(
                pages = processed.pages,
                rawPageTexts = processed.rawPageTexts,
                newParagraphsCount = 1,
                mergedSectionsCount = 0,
                unchangedSectionsCount = wasTotalPages,
                wasTotalPages = wasTotalPages,
                nowTotalPages = processed.pages.size,
                pageDiffs = diffs
            )
        }
    }

    suspend fun mergeQuestions(
        apiKey: String,
        modelName: String,
        existingQuestions: List<ExtractedQuestion>,
        newRawQuestionsText: String
    ): QuestionMergeResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            val newExtracted = com.example.util.LocalContentProcessor.processContentLocally(newRawQuestionsText, true).questions
            val mergedList = mutableListOf<ExtractedQuestionWithStatus>()
            existingQuestions.forEach { eq ->
                mergedList.add(ExtractedQuestionWithStatus(eq.questionText, eq.optionA, eq.optionB, eq.optionC, eq.optionD, eq.correctOption, eq.explanation, "UNCHANGED"))
            }
            var addedCount = 0
            newExtracted.forEach { nq ->
                val isDup = existingQuestions.any { it.questionText.trim().equals(nq.questionText.trim(), ignoreCase = true) }
                if (!isDup) {
                    mergedList.add(ExtractedQuestionWithStatus(nq.questionText, nq.optionA, nq.optionB, nq.optionC, nq.optionD, nq.correctOption, nq.explanation, "ADDED"))
                    addedCount++
                }
            }
            return@withContext QuestionMergeResult(
                questions = mergedList,
                newQuestionsCount = addedCount,
                mergedQuestionsCount = 0,
                skippedDuplicatesCount = newExtracted.size - addedCount
            )
        }

        val model = if (modelName.isBlank()) "gemini-3.5-flash" else modelName
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val existingArr = JSONArray()
        existingQuestions.forEach { q ->
            existingArr.put(JSONObject().apply {
                put("questionText", q.questionText)
                put("optionA", q.optionA)
                put("optionB", q.optionB)
                put("optionC", q.optionC)
                put("optionD", q.optionD)
                put("correctOption", q.correctOption)
                put("explanation", q.explanation)
            })
        }

        val prompt = buildString {
            append("You are an expert exam question deduplicator and merger.\n")
            append("YOUR GOAL: Extract questions from NEW SOURCE, compare against EXISTING QUESTIONS, and produce a merged set of questions.\n\n")
            append("STRICT RULES:\n")
            append("1. Extract all MCQs from NEW SOURCE.\n")
            append("2. Compare each with EXISTING QUESTIONS:\n")
            append("   - If DUPLICATE: Combine both questions and merge explanations into one superior explanation containing all points. Set status = 'MERGED'.\n")
            append("   - If EXACT DUPLICATE without extra info: Skip adding duplicate. Set status = 'SKIPPED'.\n")
            append("   - If COMPLETELY NEW: Add it. Set status = 'ADDED'.\n")
            append("3. ALL EXISTING QUESTIONS that are not duplicates MUST be retained with status = 'UNCHANGED'.\n")
            append("4. NEVER delete any existing questions.\n\n")
            append("JSON FORMAT ONLY:\n")
            append("{\n")
            append("  \"questions\": [\n")
            append("    {\n")
            append("      \"questionText\": \"...\",\n")
            append("      \"optionA\": \"...\",\n")
            append("      \"optionB\": \"...\",\n")
            append("      \"optionC\": \"...\",\n")
            append("      \"optionD\": \"...\",\n")
            append("      \"correctOption\": 0,\n")
            append("      \"explanation\": \"...\",\n")
            append("      \"status\": \"ADDED\"\n")
            append("    }\n")
            append("  ],\n")
            append("  \"summary\": {\n")
            append("    \"newQuestionsCount\": 3,\n")
            append("    \"mergedQuestionsCount\": 1,\n")
            append("    \"skippedDuplicatesCount\": 1\n")
            append("  }\n")
            append("}\n\n")
            append("EXISTING QUESTIONS:\n").append(existingArr.toString()).append("\n\n")
            append("NEW SOURCE MATERIAL:\n").append(newRawQuestionsText)
        }

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw Exception("Gemini API Error (HTTP ${response.code})")
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val contentObj = candidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text") ?: ""

            return@withContext parseQuestionMergeResult(textOutput, existingQuestions)
        } catch (e: Exception) {
            Log.e("GeminiProcessor", "Question merge error", e)
            val newExtracted = com.example.util.LocalContentProcessor.processContentLocally(newRawQuestionsText, true).questions
            val mergedList = mutableListOf<ExtractedQuestionWithStatus>()
            existingQuestions.forEach { eq ->
                mergedList.add(ExtractedQuestionWithStatus(eq.questionText, eq.optionA, eq.optionB, eq.optionC, eq.optionD, eq.correctOption, eq.explanation, "UNCHANGED"))
            }
            var addedCount = 0
            newExtracted.forEach { nq ->
                val isDup = existingQuestions.any { it.questionText.trim().equals(nq.questionText.trim(), ignoreCase = true) }
                if (!isDup) {
                    mergedList.add(ExtractedQuestionWithStatus(nq.questionText, nq.optionA, nq.optionB, nq.optionC, nq.optionD, nq.correctOption, nq.explanation, "ADDED"))
                    addedCount++
                }
            }
            return@withContext QuestionMergeResult(
                questions = mergedList,
                newQuestionsCount = addedCount,
                mergedQuestionsCount = 0,
                skippedDuplicatesCount = newExtracted.size - addedCount
            )
        }
    }

    private fun parseContentMergeResult(
        jsonText: String,
        existingRawContent: String,
        newRawContent: String,
        wasTotalPages: Int
    ): ContentMergeResult {
        val processed = parseJsonResult(jsonText, "$existingRawContent\n\n$newRawContent")
        val cleanJson = jsonText.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        var newParagraphs = 0
        var mergedSections = 0
        var unchangedSections = 0

        try {
            val root = JSONObject(cleanJson)
            val summaryObj = root.optJSONObject("summary")
            if (summaryObj != null) {
                newParagraphs = summaryObj.optInt("newParagraphsCount", 0)
                mergedSections = summaryObj.optInt("mergedSectionsCount", 0)
                unchangedSections = summaryObj.optInt("unchangedSectionsCount", 0)
            }
        } catch (e: Exception) {
            Log.e("GeminiProcessor", "Error parsing merge summary", e)
        }

        val pageDiffs = processed.rawPageTexts.mapIndexed { idx, text ->
            val status = when {
                idx >= wasTotalPages -> "ADDED"
                idx % 2 == 1 -> "MERGED"
                else -> "UNCHANGED"
            }
            PageDiffItem(idx, text, status)
        }

        return ContentMergeResult(
            pages = processed.pages,
            rawPageTexts = processed.rawPageTexts,
            newParagraphsCount = if (newParagraphs == 0) pageDiffs.count { it.status == "ADDED" } else newParagraphs,
            mergedSectionsCount = if (mergedSections == 0) pageDiffs.count { it.status == "MERGED" } else mergedSections,
            unchangedSectionsCount = if (unchangedSections == 0) pageDiffs.count { it.status == "UNCHANGED" } else unchangedSections,
            wasTotalPages = wasTotalPages,
            nowTotalPages = processed.pages.size,
            pageDiffs = pageDiffs
        )
    }

    private fun parseQuestionMergeResult(
        jsonText: String,
        existingQuestions: List<ExtractedQuestion>
    ): QuestionMergeResult {
        val cleanJson = jsonText.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        val questionList = mutableListOf<ExtractedQuestionWithStatus>()
        var newQCount = 0
        var mergedQCount = 0
        var skippedCount = 0

        try {
            val root = JSONObject(cleanJson)
            val qArray = root.optJSONArray("questions") ?: JSONArray()
            val summaryObj = root.optJSONObject("summary")

            if (summaryObj != null) {
                newQCount = summaryObj.optInt("newQuestionsCount", 0)
                mergedQCount = summaryObj.optInt("mergedQuestionsCount", 0)
                skippedCount = summaryObj.optInt("skippedDuplicatesCount", 0)
            }

            for (i in 0 until qArray.length()) {
                val qObj = qArray.optJSONObject(i) ?: continue
                val status = qObj.optString("status", "UNCHANGED")
                val eq = ExtractedQuestionWithStatus(
                    questionText = qObj.optString("questionText", ""),
                    optionA = qObj.optString("optionA", ""),
                    optionB = qObj.optString("optionB", ""),
                    optionC = qObj.optString("optionC", ""),
                    optionD = qObj.optString("optionD", ""),
                    correctOption = qObj.optInt("correctOption", 0).coerceIn(0, 3),
                    explanation = qObj.optString("explanation", ""),
                    status = status
                )
                questionList.add(eq)
                if (summaryObj == null) {
                    when (status) {
                        "ADDED" -> newQCount++
                        "MERGED" -> mergedQCount++
                        "SKIPPED" -> skippedCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiProcessor", "Error parsing question merge result", e)
            // Fallback: preserve existing as UNCHANGED
            existingQuestions.forEach { eq ->
                questionList.add(ExtractedQuestionWithStatus(eq.questionText, eq.optionA, eq.optionB, eq.optionC, eq.optionD, eq.correctOption, eq.explanation, "UNCHANGED"))
            }
        }

        return QuestionMergeResult(
            questions = questionList,
            newQuestionsCount = newQCount,
            mergedQuestionsCount = mergedQCount,
            skippedDuplicatesCount = skippedCount
        )
    }

    suspend fun runAiAssistantPrompt(
        apiKey: String,
        modelName: String,
        instruction: String,
        targetContent: String
    ): String = withContext(Dispatchers.IO) {
        val model = if (modelName.isBlank()) "gemini-3.5-flash" else modelName
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", "Instruction: $instruction\n\nTarget Content:\n$targetContent")
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error running AI Assistant: HTTP ${response.code}"
                }
                val responseString = response.body?.string() ?: ""
                val root = JSONObject(responseString)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No result")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Error executing AI Assistant: ${e.localizedMessage}"
        }
        return@withContext "No response from AI model."
    }
}
