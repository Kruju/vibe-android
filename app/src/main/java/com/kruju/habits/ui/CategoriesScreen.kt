package com.kruju.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kruju.habits.HabitViewModel
import com.kruju.habits.data.AppData
import com.kruju.habits.data.Category

@Composable
fun CategoriesScreen(data: AppData, vm: HabitViewModel, contentPadding: PaddingValues) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Palette.Accent, RoundedCornerShape(14.dp))
                    .clickable { creating = true }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Palette.Accent)
                Text("  New category", color = Palette.Accent, fontWeight = FontWeight.SemiBold)
            }
        }
        items(data.categories, key = { it.id }) { category ->
            val count = data.habits.count { it.categoryId == category.id } + data.tasks.count { it.categoryId == category.id }
            Column(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.Card)
                    .clickable {
                        if (category.custom) editing = category
                        else toast(context, "Built-in categories can't be edited")
                    }
                    .padding(vertical = 14.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CategoryBadge(category, 48.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (count == 1) "1 entry" else "$count entries",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextSoft,
                )
            }
        }
    }

    if (creating) {
        CategoryDialog(null, onDismiss = { creating = false }, onSave = {
            vm.saveCategory(it)
            creating = false
        })
    }
    editing?.let { category ->
        CategoryDialog(
            category,
            onDismiss = { editing = null },
            onSave = {
                vm.saveCategory(it)
                editing = null
            },
            onDelete = {
                vm.deleteCategory(category.id)
                editing = null
            },
        )
    }
}

@Composable
private fun CategoryDialog(
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: CategoryIcons[15].first) }
    var color by rememberSaveable { mutableLongStateOf(initial?.color ?: CategoryColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        title = { Text(if (initial == null) "New category" else "Edit category") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(Category("preview", name, icon, color), 48.dp)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(24) },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Text("Color", style = MaterialTheme.typography.labelLarge, color = Palette.TextSoft)
                for (row in CategoryColors.chunked(6)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (option in row) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(option))
                                    .clickable { color = option },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (option == color) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White)
                            }
                        }
                    }
                }
                Text("Icon", style = MaterialTheme.typography.labelLarge, color = Palette.TextSoft)
                for (row in CategoryIcons.chunked(6)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for ((key, vector) in row) {
                            val selected = key == icon
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) Color(color).copy(alpha = 0.25f) else Color.Transparent)
                                    .clickable { icon = key },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(vector, contentDescription = key, tint = if (selected) Color(color) else Palette.TextSoft)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        Category(
                            id = initial?.id ?: "c${System.currentTimeMillis()}",
                            name = name.trim(),
                            icon = icon,
                            color = color,
                            custom = true,
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = Palette.Fail) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.TextSoft) }
            }
        },
    )
}
