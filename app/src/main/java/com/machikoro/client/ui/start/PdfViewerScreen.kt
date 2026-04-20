package com.machikoro.client.ui.start

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import java.io.File

@Composable
fun PdfViewerScreen(
    fileName: String = "rules.pdf",
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val currentPage = remember { mutableIntStateOf(0) }
    val totalPages = remember { mutableIntStateOf(0) }
    val currentBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val pdfRendererRef = remember { mutableStateOf<PdfRenderer?>(null) }
    val fileDescriptorRef = remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    // Initialize PDF and keep renderer open
    LaunchedEffect(Unit) {
        try {
            // Copy PDF from assets to cache
            val file = File(context.cacheDir, fileName)
            if (!file.exists()) {
                context.assets.open(fileName).use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Open PDF with PdfRenderer and keep it open
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)

            fileDescriptorRef.value = fileDescriptor
            pdfRendererRef.value = pdfRenderer
            totalPages.value = pdfRenderer.pageCount

            // Render first page
            renderPage(pdfRenderer, 0, currentBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Update bitmap when page changes
    LaunchedEffect(currentPage.value) {
        pdfRendererRef.value?.let { renderer ->
            renderPage(renderer, currentPage.value, currentBitmap)
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            currentBitmap.value?.recycle()
            pdfRendererRef.value?.close()
            fileDescriptorRef.value?.close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // PDF content
            currentBitmap.value?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "PDF Page ${currentPage.value + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White),
                    contentScale = ContentScale.FillWidth
                )
            }
        }

        // Top controls - Always visible and clickable
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button with explicit click handling
            Button(
                onClick = {
                    onClose()
                },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Close")
            }

            // Page indicator
            Text(
                text = "Page ${currentPage.value + 1} of ${totalPages.value}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Bottom navigation
        if (totalPages.value > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.9f)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (currentPage.value > 0) {
                            currentPage.value--
                        }
                    },
                    enabled = currentPage.value > 0
                ) {
                    Text("Previous")
                }
                Button(
                    onClick = {
                        if (currentPage.value < totalPages.value - 1) {
                            currentPage.value++
                        }
                    },
                    enabled = currentPage.value < totalPages.value - 1,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Next")
                }
            }
        }
    }
}

private fun renderPage(
    pdfRenderer: PdfRenderer,
    pageIndex: Int,
    currentBitmap: androidx.compose.runtime.MutableState<Bitmap?>
) {
    try {
        if (pageIndex < pdfRenderer.pageCount) {
            val page = pdfRenderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            currentBitmap.value = bitmap
            page.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

