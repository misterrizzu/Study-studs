package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PageElement

@Composable
fun TableView(
    tableData: PageElement.TableData,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, outlineColor, RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            // Header Row
            if (tableData.headers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .background(primaryColor.copy(alpha = 0.15f))
                        .border(1.dp, outlineColor)
                ) {
                    tableData.headers.forEach { header ->
                        Text(
                            text = header,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            ),
                            modifier = Modifier
                                .widthIn(min = 120.dp)
                                .padding(12.dp)
                        )
                    }
                }
            }

            // Data Rows
            tableData.rows.forEachIndexed { rowIndex, row ->
                val bg = if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface else surfaceVariant.copy(alpha = 0.3f)
                Row(
                    modifier = Modifier
                        .background(bg)
                        .border(1.dp, outlineColor.copy(alpha = 0.5f))
                ) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .widthIn(min = 120.dp)
                                .padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
