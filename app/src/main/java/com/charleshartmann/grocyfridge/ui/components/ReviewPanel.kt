package com.charleshartmann.grocyfridge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.charleshartmann.grocyfridge.model.ProposedChange
import com.charleshartmann.grocyfridge.model.ScanState
import com.charleshartmann.grocyfridge.ui.theme.NegativeDelta
import com.charleshartmann.grocyfridge.ui.theme.NeutralDelta
import com.charleshartmann.grocyfridge.ui.theme.PositiveDelta
import java.io.File

@Composable
fun ReviewPanel(
    review: ScanState.Review,
    isSaving: Boolean,
    onChange: (Int, (ProposedChange) -> ProposedChange) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(File(review.imagePath)),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Review detected items",
                        style = MaterialTheme.typography.titleLarge,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }

        item {
            Text(
                "${review.changes.count { it.included }} of ${review.changes.size} items selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        itemsIndexed(review.changes) { index, change ->
            ChangeCard(
                change = change,
                onChange = { updated -> onChange(index) { updated } }
            )
        }

        item {
            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onApply,
                    enabled = !isSaving && review.changes.any { it.included },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isSaving) {
                        Text("Syncing...")
                    } else {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Sync to Grocy")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeCard(
    change: ProposedChange,
    onChange: (ProposedChange) -> Unit
) {
    val deltaColor = when {
        change.delta > 0 -> PositiveDelta
        change.delta < 0 -> NegativeDelta
        else -> NeutralDelta
    }

    val borderColor by animateColorAsState(
        targetValue = if (change.included) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outlineVariant,
        label = "border"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                Modifier.background(
                    borderColor,
                    RoundedCornerShape(16.dp)
                )
            )
            .then(Modifier.padding(1.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            change.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (change.isNewProduct) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.NewReleases,
                                contentDescription = "New product",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "${change.currentAmount.clean()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            if (change.delta >= 0) Icons.Filled.Add else Icons.Filled.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = deltaColor
                        )
                        Text(
                            change.delta.signedClean(),
                            style = MaterialTheme.typography.labelLarge,
                            color = deltaColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "= ${change.count.clean()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (change.isNewProduct) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "New product will be created in Grocy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Switch(
                    checked = change.included,
                    onCheckedChange = { onChange(change.copy(included = it)) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            if (change.included) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = change.displayName,
                        onValueChange = { onChange(change.copy(displayName = it)) },
                        label = { Text("Product name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = change.count.clean(),
                        onValueChange = { text ->
                            val amount = text.toDoubleOrNull()
                            if (amount != null) onChange(change.copy(count = amount))
                        },
                        label = { Text("Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

private fun Double.clean(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else "%.2f".format(this)
}

private fun Double.signedClean(): String {
    val cleaned = kotlin.math.abs(this).clean()
    return if (this >= 0) "+$cleaned" else "-$cleaned"
}
