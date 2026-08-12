package com.example.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class PageContent(
    val elements: List<PageElement> = emptyList()
)

sealed class PageElement {
    data class Heading(
        val text: String,
        val level: Int = 1
    ) : PageElement()

    data class Paragraph(
        val text: String,
        val highlightedTerms: List<String> = emptyList()
    ) : PageElement()

    data class BulletList(
        val items: List<String>
    ) : PageElement()

    data class NumberedList(
        val items: List<String>
    ) : PageElement()

    data class TableData(
        val headers: List<String>,
        val rows: List<List<String>>
    ) : PageElement()

    data class DiagramData(
        val title: String,
        val diagramType: String = "FLOWCHART", // FLOWCHART, BLOCK, TREE, SEQUENCE
        val nodes: List<DiagramNode> = emptyList(),
        val connections: List<DiagramConnection> = emptyList(),
        val rawAscii: String = ""
    ) : PageElement()

    data class Callout(
        val title: String,
        val text: String,
        val type: String = "INFO" // INFO, NOTE, WARNING, HIGHLIGHT
    ) : PageElement()

    data class RawText(
        val text: String,
        val stylePreset: String = "samsung" // samsung, monospace, standard
    ) : PageElement()

    data class ImageData(
        val caption: String = "",
        val imageUriOrBase64: String = "",
        val isAiGenerated: Boolean = false
    ) : PageElement()
}

@JsonClass(generateAdapter = true)
data class DiagramNode(
    val id: String,
    val label: String,
    val type: String = "RECT" // RECT, CIRCLE, DIAMOND, CLOUD
)

@JsonClass(generateAdapter = true)
data class DiagramConnection(
    val fromId: String,
    val toId: String,
    val label: String = ""
)

@JsonClass(generateAdapter = true)
data class UserAnswerRecord(
    val questionId: Long,
    val questionText: String,
    val selectedOption: Int,
    val correctOption: Int,
    val isCorrect: Boolean,
    val explanation: String
)

class PageElementAdapter : JsonAdapter<PageElement>() {
    @FromJson
    override fun fromJson(reader: JsonReader): PageElement? {
        val map = reader.readJsonValue() as? Map<*, *> ?: return null
        val type = map["type"] as? String ?: "PARAGRAPH"
        return when (type) {
            "HEADING" -> {
                val text = map["text"] as? String ?: ""
                val level = (map["level"] as? Number)?.toInt() ?: 1
                PageElement.Heading(text, level)
            }
            "PARAGRAPH" -> {
                val text = map["text"] as? String ?: ""
                val highlighted = (map["highlightedTerms"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                PageElement.Paragraph(text, highlighted)
            }
            "BULLET_LIST" -> {
                val items = (map["items"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                PageElement.BulletList(items)
            }
            "NUMBERED_LIST" -> {
                val items = (map["items"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                PageElement.NumberedList(items)
            }
            "TABLE" -> {
                val headers = (map["headers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val rawRows = map["rows"] as? List<*> ?: emptyList<Any>()
                val rows = rawRows.mapNotNull { row ->
                    (row as? List<*>)?.mapNotNull { it as? String }
                }
                PageElement.TableData(headers, rows)
            }
            "DIAGRAM" -> {
                val title = map["title"] as? String ?: "Diagram"
                val diagramType = map["diagramType"] as? String ?: "FLOWCHART"
                val rawNodes = map["nodes"] as? List<*> ?: emptyList<Any>()
                val nodes = rawNodes.mapNotNull { nodeMap ->
                    if (nodeMap is Map<*, *>) {
                        DiagramNode(
                            id = nodeMap["id"] as? String ?: "",
                            label = nodeMap["label"] as? String ?: "",
                            type = nodeMap["type"] as? String ?: "RECT"
                        )
                    } else null
                }
                val rawConns = map["connections"] as? List<*> ?: emptyList<Any>()
                val conns = rawConns.mapNotNull { connMap ->
                    if (connMap is Map<*, *>) {
                        DiagramConnection(
                            fromId = connMap["fromId"] as? String ?: "",
                            toId = connMap["toId"] as? String ?: "",
                            label = connMap["label"] as? String ?: ""
                        )
                    } else null
                }
                val rawAscii = map["rawAscii"] as? String ?: ""
                PageElement.DiagramData(title, diagramType, nodes, conns, rawAscii)
            }
            "CALLOUT" -> {
                val title = map["title"] as? String ?: "Note"
                val text = map["text"] as? String ?: ""
                val calloutType = map["calloutType"] as? String ?: map["callout_type"] as? String ?: "INFO"
                PageElement.Callout(title, text, calloutType)
            }
            "RAW_TEXT" -> {
                val text = map["text"] as? String ?: ""
                val stylePreset = map["stylePreset"] as? String ?: map["style_preset"] as? String ?: "samsung"
                PageElement.RawText(text, stylePreset)
            }
            "IMAGE" -> {
                val caption = map["caption"] as? String ?: ""
                val imageUriOrBase64 = map["imageUriOrBase64"] as? String ?: map["image_uri_or_base64"] as? String ?: ""
                val isAiGenerated = map["isAiGenerated"] as? Boolean ?: false
                PageElement.ImageData(caption, imageUriOrBase64, isAiGenerated)
            }
            else -> {
                val text = map["text"] as? String ?: ""
                PageElement.Paragraph(text, emptyList())
            }
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: PageElement?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        when (value) {
            is PageElement.Heading -> {
                writer.name("type").value("HEADING")
                writer.name("text").value(value.text)
                writer.name("level").value(value.level)
            }
            is PageElement.Paragraph -> {
                writer.name("type").value("PARAGRAPH")
                writer.name("text").value(value.text)
                writer.name("highlightedTerms")
                writer.beginArray()
                for (term in value.highlightedTerms) writer.value(term)
                writer.endArray()
            }
            is PageElement.BulletList -> {
                writer.name("type").value("BULLET_LIST")
                writer.name("items")
                writer.beginArray()
                for (item in value.items) writer.value(item)
                writer.endArray()
            }
            is PageElement.NumberedList -> {
                writer.name("type").value("NUMBERED_LIST")
                writer.name("items")
                writer.beginArray()
                for (item in value.items) writer.value(item)
                writer.endArray()
            }
            is PageElement.TableData -> {
                writer.name("type").value("TABLE")
                writer.name("headers")
                writer.beginArray()
                for (header in value.headers) writer.value(header)
                writer.endArray()

                writer.name("rows")
                writer.beginArray()
                for (row in value.rows) {
                    writer.beginArray()
                    for (cell in row) writer.value(cell)
                    writer.endArray()
                }
                writer.endArray()
            }
            is PageElement.DiagramData -> {
                writer.name("type").value("DIAGRAM")
                writer.name("title").value(value.title)
                writer.name("diagramType").value(value.diagramType)
                writer.name("rawAscii").value(value.rawAscii)

                writer.name("nodes")
                writer.beginArray()
                for (node in value.nodes) {
                    writer.beginObject()
                    writer.name("id").value(node.id)
                    writer.name("label").value(node.label)
                    writer.name("type").value(node.type)
                    writer.endObject()
                }
                writer.endArray()

                writer.name("connections")
                writer.beginArray()
                for (conn in value.connections) {
                    writer.beginObject()
                    writer.name("fromId").value(conn.fromId)
                    writer.name("toId").value(conn.toId)
                    writer.name("label").value(conn.label)
                    writer.endObject()
                }
                writer.endArray()
            }
            is PageElement.Callout -> {
                writer.name("type").value("CALLOUT")
                writer.name("title").value(value.title)
                writer.name("text").value(value.text)
                writer.name("calloutType").value(value.type)
            }
            is PageElement.RawText -> {
                writer.name("type").value("RAW_TEXT")
                writer.name("text").value(value.text)
                writer.name("stylePreset").value(value.stylePreset)
            }
            is PageElement.ImageData -> {
                writer.name("type").value("IMAGE")
                writer.name("caption").value(value.caption)
                writer.name("imageUriOrBase64").value(value.imageUriOrBase64)
                writer.name("isAiGenerated").value(value.isAiGenerated)
            }
        }
        writer.endObject()
    }
}

object MoshiProvider {
    val moshi: Moshi = Moshi.Builder()
        .add(PageElementAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()
}
