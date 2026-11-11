package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.models.GroceryItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility to export active shopping lists as formatted TXT or Cosmic-themed PNG.
 * Required file naming convention:
 * grocery-list[date]_[timestamp].[extension]
 * where [date] = YYYY-MM-DD and [timestamp] = HH-mm-ss
 */
object GroceryExportUtil {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH-mm-ss", Locale.US)
    private val displayHeaderFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.US)

    /**
     * Generates a filename adhering strictly to:
     * grocery-list[date]_[timestamp].[extension]
     * e.g. grocery-list2026-11-11_10-13-00.png or grocery-list2026-11-11_10-13-00.txt
     */
    fun generateExportFilename(extension: String, timestamp: Long = System.currentTimeMillis()): String {
        val date = Date(timestamp)
        val ext = extension.removePrefix(".")
        return "grocery-list${dateFormat.format(date)}_${timeFormat.format(date)}.$ext"
    }

    /**
     * Exports active (unpurchased) grocery items as a human-readable UTF-8 checklist TXT file.
     */
    fun exportShoppingListAsTxt(
        context: Context,
        activeItems: List<GroceryItem>,
        hubName: String = "Cosmic Family Hub"
    ): File {
        val filename = generateExportFilename("txt")
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val targetFile = File(exportDir, filename)

        val now = Date()
        val builder = StringBuilder()
        builder.append("====================================================\n")
        builder.append(" $hubName - Active Shopping List\n")
        builder.append(" Generated: ${displayHeaderFormat.format(now)}\n")
        builder.append(" Total Items Needed: ${activeItems.size}\n")
        builder.append("====================================================\n\n")

        if (activeItems.isEmpty()) {
            builder.append("[ ] No active shopping list items! The pantry is fully stocked.\n")
        } else {
            // Group by store then by category
            val byStore = activeItems.groupBy { it.store.ifBlank { "Kroger" } }
            for ((store, storeItems) in byStore) {
                builder.append("STORE: ${store.uppercase(Locale.US)} (${storeItems.size} items)\n")
                builder.append("----------------------------------------------------\n")

                val byCategory = storeItems.groupBy { it.category.ifBlank { "Produce" } }
                for ((category, catItems) in byCategory) {
                    builder.append("  [Category: $category]\n")
                    for (item in catItems) {
                        val qtyStr = "${item.quantity} ${item.unit}".trim()
                        val noteStr = if (item.notes.isNotBlank()) " | Note: ${item.notes}" else ""
                        val assignStr = if (item.assignedTo.isNotBlank()) " | For: ${item.assignedTo}" else ""
                        builder.append("    [ ] ${item.title} ($qtyStr)$assignStr$noteStr\n")
                    }
                    builder.append("\n")
                }
                builder.append("\n")
            }
        }

        builder.append("====================================================\n")
        builder.append(" Cosmic Family Hub • Real-time Household Sync\n")
        builder.append("====================================================\n")

        targetFile.writeText(builder.toString(), Charsets.UTF_8)
        return targetFile
    }

    /**
     * Renders active (unpurchased) grocery items into a high-resolution, Cosmic Vaporwave
     * styled PNG checklist.
     */
    fun exportShoppingListAsPng(
        context: Context,
        activeItems: List<GroceryItem>,
        hubName: String = "Cosmic Family Hub"
    ): File {
        val filename = generateExportFilename("png")
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val targetFile = File(exportDir, filename)

        val width = 1080
        val padding = 48f
        val contentWidth = width - (padding * 2)

        // Pre-calculate line layout and height
        var estimatedHeight = 360f // Header and footer space
        val byStore = activeItems.groupBy { it.store.ifBlank { "Kroger" } }
        for ((_, storeItems) in byStore) {
            estimatedHeight += 70f // Store title
            val byCategory = storeItems.groupBy { it.category.ifBlank { "Produce" } }
            for ((_, catItems) in byCategory) {
                estimatedHeight += 48f // Category header
                estimatedHeight += catItems.size * 64f // Each item
                estimatedHeight += 20f // Category spacer
            }
            estimatedHeight += 30f // Store spacer
        }
        if (activeItems.isEmpty()) {
            estimatedHeight += 120f
        }

        val totalHeight = estimatedHeight.coerceAtLeast(800f).toInt()
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Deep cosmic dark background
        canvas.drawColor(Color.parseColor("#080114"))

        // Paints
        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F5FF") // Neon Cyan
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintSubtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0FF")
            textSize = 22f
            alpha = 200
        }
        val paintStoreHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF007F") // Neon Pink
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintCategoryHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B026FF") // Neon Purple
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintItemTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintItemDetails = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A3A3D0")
            textSize = 19f
        }
        val paintCheckbox = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F5FF")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val paintDivider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#261447")
            strokeWidth = 2f
        }
        val paintBadgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#17092E")
            style = Paint.Style.FILL
        }

        var y = padding + 40f

        // Top Banner / Header
        canvas.drawText(hubName.uppercase(Locale.US), padding, y, paintTitle)
        y += 36f
        canvas.drawText("Active Shopping List • ${activeItems.size} items needed", padding, y, paintSubtitle)
        y += 28f
        canvas.drawText("Generated ${displayHeaderFormat.format(Date())}", padding, y, paintSubtitle)
        y += 32f

        // Top divider
        canvas.drawLine(padding, y, width - padding, y, paintDivider)
        y += 36f

        if (activeItems.isEmpty()) {
            paintSubtitle.textSize = 26f
            canvas.drawText("No items needed! Your shopping list is clear.", padding, y + 40f, paintSubtitle)
            y += 80f
        } else {
            for ((store, storeItems) in byStore) {
                // Store Header Card Badge
                val storeBadgeRect = RectF(padding, y - 26f, width - padding, y + 16f)
                canvas.drawRoundRect(storeBadgeRect, 10f, 10f, paintBadgeBg)
                canvas.drawText("STORE: ${store.uppercase(Locale.US)} (${storeItems.size})", padding + 16f, y, paintStoreHeader)
                y += 42f

                val byCategory = storeItems.groupBy { it.category.ifBlank { "Produce" } }
                for ((category, catItems) in byCategory) {
                    canvas.drawText("• $category", padding + 8f, y, paintCategoryHeader)
                    y += 32f

                    for (item in catItems) {
                        // Draw empty checkbox square
                        val boxLeft = padding + 12f
                        val boxTop = y - 20f
                        val boxSize = 22f
                        canvas.drawRoundRect(
                            RectF(boxLeft, boxTop, boxLeft + boxSize, boxTop + boxSize),
                            4f,
                            4f,
                            paintCheckbox
                        )

                        // Draw Item Title
                        val textLeft = boxLeft + boxSize + 16f
                        val title = item.title.take(38)
                        canvas.drawText(title, textLeft, y, paintItemTitle)

                        // Draw details (quantity, unit, store, notes)
                        val qtyPart = "${item.quantity} ${item.unit}".trim()
                        val notePart = if (item.notes.isNotBlank()) " • Note: ${item.notes.take(24)}" else ""
                        val assignPart = if (item.assignedTo.isNotBlank()) " • For: ${item.assignedTo}" else ""
                        val details = "($qtyPart)$assignPart$notePart"
                        val detailsX = textLeft + paintItemTitle.measureText(title) + 14f

                        if (detailsX + paintItemDetails.measureText(details) < width - padding) {
                            canvas.drawText(details, detailsX, y - 2f, paintItemDetails)
                        } else {
                            canvas.drawText(details, textLeft, y + 24f, paintItemDetails)
                            y += 24f
                        }

                        y += 40f
                    }
                    y += 12f
                }
                y += 18f
            }
        }

        // Footer
        canvas.drawLine(padding, y, width - padding, y, paintDivider)
        y += 30f
        val paintFooter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F5FF")
            textSize = 18f
            alpha = 180
        }
        canvas.drawText("⚡ COSMIC FAMILY HUB • REAL-TIME HOUSEHOLD SYNCHRONIZATION", padding, y, paintFooter)

        // Write bitmap to PNG file
        FileOutputStream(targetFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return targetFile
    }

    /**
     * Dispatches an Intent to share or save the file via Android's chooser.
     */
    fun shareExportedFile(context: Context, file: File, mimeType: String, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Here is our Cosmic Family Hub active shopping list.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
