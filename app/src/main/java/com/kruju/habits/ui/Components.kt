package com.kruju.habits.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NoDrinks
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kruju.habits.data.Category

/** Icons a category can use, stored by key in the data file. */
val CategoryIcons: List<Pair<String, ImageVector>> = listOf(
    "block" to Icons.Filled.Block,
    "brush" to Icons.Filled.Brush,
    "meditation" to Icons.Filled.SelfImprovement,
    "school" to Icons.Filled.School,
    "bike" to Icons.Filled.DirectionsBike,
    "movie" to Icons.Filled.Movie,
    "forum" to Icons.Filled.Forum,
    "money" to Icons.Filled.AttachMoney,
    "heart" to Icons.Filled.Favorite,
    "work" to Icons.Filled.Work,
    "restaurant" to Icons.Filled.Restaurant,
    "home" to Icons.Filled.Home,
    "terrain" to Icons.Filled.Terrain,
    "task" to Icons.Filled.TaskAlt,
    "category" to Icons.Filled.Category,
    "fitness" to Icons.Filled.FitnessCenter,
    "water" to Icons.Filled.WaterDrop,
    "book" to Icons.Filled.AutoStories,
    "sleep" to Icons.Filled.Bedtime,
    "music" to Icons.Filled.MusicNote,
    "pets" to Icons.Filled.Pets,
    "shopping" to Icons.Filled.ShoppingCart,
    "spa" to Icons.Filled.Spa,
    "flower" to Icons.Filled.LocalFlorist,
    "code" to Icons.Filled.Code,
    "savings" to Icons.Filled.Savings,
    "smokefree" to Icons.Filled.SmokeFree,
    "nodrinks" to Icons.Filled.NoDrinks,
    "trophy" to Icons.Filled.EmojiEvents,
    "language" to Icons.Filled.Language,
    "cleaning" to Icons.Filled.CleaningServices,
    "medication" to Icons.Filled.Medication,
    "laptop" to Icons.Filled.Laptop,
    "psychology" to Icons.Filled.Psychology,
    "hiking" to Icons.Filled.Hiking,
    "pool" to Icons.Filled.Pool,
)

private val IconByKey = CategoryIcons.toMap()

fun iconFor(key: String): ImageVector = IconByKey[key] ?: Icons.Filled.Category

fun Category.composeColor(): Color = Color(color)

/** The rounded, tinted square that marks an item's category everywhere in the app. */
@Composable
fun CategoryBadge(category: Category, size: Dp = 40.dp) {
    val color = category.composeColor()
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(iconFor(category.icon), contentDescription = category.name, tint = color, modifier = Modifier.size(size * 0.55f))
    }
}

/** Small colored label such as "Habit" or "Task". */
@Composable
fun Tag(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * Status on the right of each row: an empty ring, a partial progress ring,
 * a green check when done, or a red ring for a missed past day.
 */
@Composable
fun StatusCircle(progress: Float, done: Boolean, missed: Boolean, onClick: () -> Unit, size: Dp = 32.dp) {
    Box(
        Modifier
            .size(size + 12.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Palette.Success),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(size * 0.65f))
            }
        } else {
            val track = if (missed) Palette.Fail else Palette.TextFaint
            Canvas(Modifier.size(size)) {
                val stroke = 2.5.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (progress > 0f) {
                    drawArc(
                        Palette.Accent, -90f, 360f * progress, false, Offset(inset, inset), arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}

/** A list row in the style of the Today/Habits lists: badge, title, subtitle line, trailing content. */
@Composable
fun ItemRow(
    category: Category,
    title: String,
    onClick: () -> Unit,
    subtitle: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
    dim: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (dim) Palette.TextSoft else Palette.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                subtitle()
            }
        }
        trailing()
    }
}

@Composable
fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 70.dp)
            .height(1.dp)
            .background(Palette.Divider)
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Palette.Card),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextSoft,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** Pill-shaped choice used for weekday pickers, presets and segmented controls. */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Palette.Accent else Palette.Card)
            .then(if (selected) Modifier else Modifier.border(1.dp, Palette.Divider, RoundedCornerShape(50)))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) Color.White else Palette.Text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun priorityLabel(priority: Int): String = when (priority) {
    0 -> "Low"
    2 -> "High"
    else -> "Normal"
}
