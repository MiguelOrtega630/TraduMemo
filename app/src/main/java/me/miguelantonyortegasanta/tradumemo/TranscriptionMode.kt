package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class TranscriptionMode { TRANSCRIBE, TRANSLATE }

@Composable
fun ModeSegmentedControl(
    mode: TranscriptionMode,
    onModeChange: (TranscriptionMode) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedText = MaterialTheme.colorScheme.onPrimary
    val unselectedText = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .height(40.dp),
        ) {
            val commonModifier = Modifier
                .weight(1f)
                .fillMaxHeight()

            // Left segment: Transcribir
            Box(
                modifier = commonModifier
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onModeChange(TranscriptionMode.TRANSCRIBE)
                    }
                    .background(
                        color = if (mode == TranscriptionMode.TRANSCRIBE) selectedColor else unselectedColor,
                        shape = MaterialTheme.shapes.extraLarge.copy(
                            topEnd = CornerSize(0.dp),
                            bottomEnd = CornerSize(0.dp)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Transcribir",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (mode == TranscriptionMode.TRANSCRIBE) selectedText else unselectedText
                )
            }

            // Right segment: Traducir
            Box(
                modifier = commonModifier
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onModeChange(TranscriptionMode.TRANSLATE)
                    }
                    .background(
                        color = if (mode == TranscriptionMode.TRANSLATE) selectedColor else unselectedColor,
                        shape = MaterialTheme.shapes.extraLarge.copy(
                            topStart = CornerSize(0.dp),
                            bottomStart = CornerSize(0.dp)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Traducir",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (mode == TranscriptionMode.TRANSLATE) selectedText else unselectedText
                )
            }
        }
    }
}
