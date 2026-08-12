package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PageElement

@Composable
fun DiagramCanvasView(
    diagram: PageElement.DiagramData,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "📊 ${diagram.title}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (diagram.nodes.isNotEmpty()) {
            val nodeCount = diagram.nodes.size
            val diagramType = diagram.diagramType.uppercase()
            val canvasHeight = when (diagramType) {
                "HIERARCHY", "TREE", "CONCEPT_MAP", "COMPARISON", "ARCHITECTURE" -> 260.dp
                "CYCLE" -> 280.dp
                "TIMELINE", "PROCESS", "FLOWCHART" -> ((nodeCount.coerceAtLeast(2) * 72) + 30).dp
                else -> (nodeCount * 90 + 20).dp
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
            ) {
                val width = size.width
                val boxWidth = (width * 0.7f).coerceAtMost(320f)
                val boxHeight = 56f

                fun drawNodeBox(label: String, x: Float, y: Float, w: Float, h: Float, accent: Color = primaryColor) {
                    drawRoundRect(
                        color = accent.copy(alpha = 0.15f),
                        topLeft = Offset(x, y),
                        size = Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(x, y),
                        size = Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                        style = Stroke(width = 3f)
                    )
                    val measuredText = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            color = onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    drawText(
                        measuredText,
                        topLeft = Offset(
                            x + (w - measuredText.size.width) / 2f,
                            y + (h - measuredText.size.height) / 2f
                        )
                    )
                }

                fun drawArrow(from: Offset, to: Offset) {
                    drawLine(color = primaryColor, start = from, end = to, strokeWidth = 4f)
                    val path = Path().apply {
                        moveTo(to.x - 9f, to.y - 11f)
                        lineTo(to.x + 9f, to.y - 11f)
                        lineTo(to.x, to.y)
                        close()
                    }
                    drawPath(path, color = primaryColor)
                }

                if (diagramType == "HIERARCHY" || diagramType == "TREE" || diagramType == "CONCEPT_MAP" || diagramType == "COMPARISON" || diagramType == "ARCHITECTURE") {
                    val root = diagram.nodes.first()
                    val rootWidth = (width * 0.6f).coerceAtMost(280f)
                    val rootX = (width - rootWidth) / 2f
                    drawNodeBox(root.label, rootX, 18f, rootWidth, boxHeight)

                    val children = diagram.nodes.drop(1).take(6)
                    val columns = if (children.size <= 3) children.size.coerceAtLeast(1) else 3
                    val childWidth = (width / columns - 18f).coerceIn(92f, 170f)
                    children.forEachIndexed { idx, node ->
                        val row = idx / columns
                        val col = idx % columns
                        val x = 8f + col * (width / columns) + ((width / columns) - childWidth) / 2f
                        val y = 128f + row * 78f
                        drawNodeBox(node.label, x, y, childWidth, 52f, if (diagramType == "ARCHITECTURE") Color(0xFF2196F3) else primaryColor)
                        drawLine(
                            color = primaryColor,
                            start = Offset(width / 2f, 74f),
                            end = Offset(x + childWidth / 2f, y),
                            strokeWidth = 3f
                        )
                    }
                    return@Canvas
                }

                if (diagramType == "CYCLE") {
                    val center = Offset(width / 2f, size.height / 2f)
                    val radius = minOf(width, size.height) * 0.32f
                    diagram.nodes.take(6).forEachIndexed { idx, node ->
                        val angle = (-90.0 + idx * (360.0 / diagram.nodes.take(6).size)).toFloat()
                        val radians = Math.toRadians(angle.toDouble())
                        val x = center.x + kotlin.math.cos(radians).toFloat() * radius - 58f
                        val y = center.y + kotlin.math.sin(radians).toFloat() * radius - 26f
                        drawNodeBox(node.label, x, y, 116f, 52f)
                    }
                    return@Canvas
                }

                val startX = (width - boxWidth) / 2f

                for (idx in diagram.nodes.indices) {
                    val node = diagram.nodes[idx]
                    val startY = idx * 90f + 20f

                    // Draw Node Box
                    drawNodeBox(node.label, startX, startY, boxWidth, boxHeight)

                    // Draw Arrow connection to next node
                    if (idx < nodeCount - 1) {
                        val arrowStartY = startY + boxHeight
                        val arrowEndY = arrowStartY + 34f
                        val midX = width / 2f

                        // Vertical Line
                        drawLine(
                            color = primaryColor,
                            start = Offset(midX, arrowStartY),
                            end = Offset(midX, arrowEndY),
                            strokeWidth = 4f
                        )

                        // Arrowhead
                        val path = Path().apply {
                            moveTo(midX - 10f, arrowEndY - 12f)
                            lineTo(midX + 10f, arrowEndY - 12f)
                            lineTo(midX, arrowEndY)
                            close()
                        }
                        drawPath(path, color = primaryColor)

                        // Connection Label
                        val connLabel = diagram.connections.getOrNull(idx)?.label ?: ""
                        if (connLabel.isNotBlank()) {
                            val connMeasured = textMeasurer.measure(
                                text = connLabel,
                                style = TextStyle(
                                    color = primaryColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            drawText(
                                connMeasured,
                                topLeft = Offset(midX + 12f, arrowStartY + 6f)
                            )
                        }
                    }
                }
            }
        } else if (diagram.rawAscii.isNotBlank()) {
            // Display Raw ASCII in stylized preformatted block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = diagram.rawAscii,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50) // Hacker Green ASCII
                    )
                )
            }
        }
    }
}
