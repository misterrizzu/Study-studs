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
            val canvasHeight = (nodeCount * 90 + 20).dp

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
            ) {
                val width = size.width
                val boxWidth = (width * 0.7f).coerceAtMost(320f)
                val boxHeight = 56f
                val startX = (width - boxWidth) / 2f

                for (idx in diagram.nodes.indices) {
                    val node = diagram.nodes[idx]
                    val startY = idx * 90f + 20f

                    // Draw Node Box
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.15f),
                        topLeft = Offset(startX, startY),
                        size = Size(boxWidth, boxHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(startX, startY),
                        size = Size(boxWidth, boxHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                        style = Stroke(width = 3f)
                    )

                    // Draw Text inside box
                    val measuredText = textMeasurer.measure(
                        text = node.label,
                        style = TextStyle(
                            color = onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    val textX = startX + (boxWidth - measuredText.size.width) / 2f
                    val textY = startY + (boxHeight - measuredText.size.height) / 2f
                    drawText(measuredText, topLeft = Offset(textX, textY))

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
