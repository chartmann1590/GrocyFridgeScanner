package com.charleshartmann.grocyfridge.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.charleshartmann.grocyfridge.model.GrocyStockItem
import com.charleshartmann.grocyfridge.ui.GrocyFridgeViewModel
import com.charleshartmann.grocyfridge.ui.theme.NegativeDelta
import com.charleshartmann.grocyfridge.ui.theme.PositiveDelta

@Composable
fun InventoryScreen(viewModel: GrocyFridgeViewModel) {
    val stock by viewModel.inventoryStock.collectAsState()
    val locations by viewModel.inventoryLocations.collectAsState()
    val units by viewModel.inventoryUnits.collectAsState()
    val isLoading by viewModel.inventoryLoading.collectAsState()
    val error by viewModel.inventoryError.collectAsState()
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var deleteTarget by remember { mutableStateOf<GrocyStockItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadInventory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Inventory",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isLoading) "Loading..."
                else "${stock.size} product${if (stock.size != 1) "s" else ""} in stock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }

        error?.let { msg ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearInventoryError() }) {
                        Text("Dismiss")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (isLoading && stock.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (stock.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No products in stock",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Scan your fridge or cupboards to add inventory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(stock, key = { it.productId }) { item ->
                    StockItemCard(
                        item = item,
                        locationName = item.product?.locationId?.let { locations[it] },
                        unitName = item.product?.stockUnitId?.let { units[it] },
                        isExpanded = expandedId == item.productId,
                        onToggle = {
                            expandedId = if (expandedId == item.productId) null else item.productId
                        },
                        onSetAmount = { newAmount ->
                            val locId = item.product?.locationId ?: return@StockItemCard
                            viewModel.updateProductAmount(item.productId, newAmount, locId)
                        },
                        onConsume = { amount ->
                            viewModel.consumeProduct(item.productId, amount)
                        },
                        onDelete = { deleteTarget = item }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${item.product?.name ?: "product"}?") },
            text = { Text("This will remove the product and all its stock from Grocy. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProduct(item.productId)
                    deleteTarget = null
                    expandedId = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StockItemCard(
    item: GrocyStockItem,
    locationName: String?,
    unitName: String?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSetAmount: (Double) -> Unit,
    onConsume: (Double) -> Unit,
    onDelete: () -> Unit
) {
    val productName = item.product?.name ?: "Unknown Product"
    val locationIcon = when {
        locationName.equals("Fridge", ignoreCase = true) -> Icons.Filled.Kitchen
        else -> Icons.Filled.ShoppingCart
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        productName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            locationIcon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            locationName ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    "${item.amount.clean()} ${unitName ?: ""}".trim(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(8.dp))

                Icon(
                    if (isExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    QuickActions(
                        currentAmount = item.amount,
                        onSetAmount = onSetAmount,
                        onConsume = onConsume
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SetAmountRow(
                            currentAmount = item.amount,
                            onSetAmount = onSetAmount
                        )

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete product",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    currentAmount: Double,
    onSetAmount: (Double) -> Unit,
    onConsume: (Double) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { if (currentAmount >= 1.0) onConsume(1.0) },
            shape = RoundedCornerShape(12.dp),
            enabled = currentAmount >= 1.0,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Use 1")
        }

        OutlinedButton(
            onClick = { onSetAmount(currentAmount + 1.0) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add 1")
        }

        if (currentAmount > 0) {
            OutlinedButton(
                onClick = { onConsume(currentAmount) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(16.dp), tint = NegativeDelta)
                Spacer(Modifier.width(4.dp))
                Text("Use all", color = NegativeDelta)
            }
        }
    }
}

@Composable
private fun SetAmountRow(
    currentAmount: Double,
    onSetAmount: (Double) -> Unit
) {
    var editText by remember(currentAmount) { mutableStateOf(currentAmount.clean()) }
    var isEditing by remember { mutableStateOf(false) }

    if (isEditing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(100.dp),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                val newAmount = editText.toDoubleOrNull()
                if (newAmount != null && newAmount >= 0) {
                    onSetAmount(newAmount)
                    isEditing = false
                }
            }) {
                Text("Set", color = PositiveDelta)
            }
            TextButton(onClick = { isEditing = false }) {
                Text("Cancel")
            }
        }
    } else {
        TextButton(onClick = { isEditing = true }) {
            Text("Set amount...")
        }
    }
}

private fun Double.clean(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else "%.2f".format(this)
}
