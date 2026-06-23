package com.abccash.app.treasury.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abccash.app.R
import com.abccash.app.treasury.data.DocumentPdfTemplate

@Composable
fun DocumentPdfTemplateSelector(
    selected: DocumentPdfTemplate,
    onSelect: (DocumentPdfTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DocumentPdfTemplate.entries.forEach { template ->
            PdfTemplateOptionCard(
                template = template,
                selected = template == selected,
                onClick = { onSelect(template) }
            )
        }
    }
}

@Composable
private fun PdfTemplateOptionCard(
    template: DocumentPdfTemplate,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (primary, secondary, accent) = previewColors(template)
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                shape = shape
            )
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(56.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(primary)
            )
            if (template == DocumentPdfTemplate.MODERN_GREEN) {
                Row(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(accent)
                    )
                    Spacer(Modifier.weight(1f))
                }
            } else if (template == DocumentPdfTemplate.ELEGANT_SLATE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(accent)
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(secondary, RoundedCornerShape(2.dp))
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(template.titleRes()),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = stringResource(template.descriptionRes()),
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                lineHeight = 15.sp
            )
        }
    }
}

@StringRes
private fun DocumentPdfTemplate.titleRes(): Int = when (this) {
    DocumentPdfTemplate.CLASSIC_BLUE -> R.string.pdf_template_classic_blue
    DocumentPdfTemplate.MODERN_GREEN -> R.string.pdf_template_modern_green
    DocumentPdfTemplate.ELEGANT_SLATE -> R.string.pdf_template_elegant_slate
}

@StringRes
private fun DocumentPdfTemplate.descriptionRes(): Int = when (this) {
    DocumentPdfTemplate.CLASSIC_BLUE -> R.string.pdf_template_classic_blue_desc
    DocumentPdfTemplate.MODERN_GREEN -> R.string.pdf_template_modern_green_desc
    DocumentPdfTemplate.ELEGANT_SLATE -> R.string.pdf_template_elegant_slate_desc
}

private fun previewColors(template: DocumentPdfTemplate): Triple<Color, Color, Color> = when (template) {
    DocumentPdfTemplate.CLASSIC_BLUE -> Triple(Color(0xFF2196F3), Color(0xFFE3F2FD), Color(0xFF1976D2))
    DocumentPdfTemplate.MODERN_GREEN -> Triple(Color.White, Color(0xFFE8F5E9), Color(0xFF4CAF50))
    DocumentPdfTemplate.ELEGANT_SLATE -> Triple(Color(0xFF1E293B), Color(0xFFF1F5F9), Color(0xFFF59E0B))
}
