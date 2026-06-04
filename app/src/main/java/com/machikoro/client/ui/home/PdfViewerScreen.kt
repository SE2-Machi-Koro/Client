package com.machikoro.client.ui.home

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.machikoro.client.R
import com.machikoro.client.ui.shared.Background
import com.machikoro.client.ui.theme.ButtonBlueDark
import com.machikoro.client.ui.theme.ButtonColor
import com.machikoro.client.ui.theme.ButtonTextColor
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.PrimaryOrange
import com.machikoro.client.ui.theme.TextBlueDark
import com.machikoro.client.ui.theme.TextWhite
import java.io.File

@Composable
fun PdfViewerScreen(
    fileName: String = "rules.pdf",
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val currentPage = remember { mutableIntStateOf(0) }
    val totalPages = remember { mutableIntStateOf(0) }
    val currentBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val pdfRendererRef = remember { mutableStateOf<PdfRenderer?>(null) }
    val fileDescriptorRef = remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    // Check if in landscape mode
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Scroll state for portrait mode
    val verticalScrollState = rememberScrollState()

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
            totalPages.intValue = pdfRenderer.pageCount

            // Render first page
            renderPage(pdfRenderer, 0, currentBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Update bitmap when page changes
    LaunchedEffect(currentPage.intValue) {
        pdfRendererRef.value?.let { renderer ->
            renderPage(renderer, currentPage.intValue, currentBitmap)
        }
    }

    // Re-render when orientation changes
    LaunchedEffect(configuration.orientation) {
        pdfRendererRef.value?.let { renderer ->
            renderPage(renderer, currentPage.intValue, currentBitmap)
        }
    }

    // Cleanup on composable dispose
    DisposableEffect(Unit) {
        onDispose {
            currentBitmap.value?.recycle()
            pdfRendererRef.value?.close()
            fileDescriptorRef.value?.close()
        }
    }

    // Get system bars insets
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Background(R.drawable.background_wood)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp, bottom = if (totalPages.intValue > 1) 56.dp else 0.dp)
        ) {
            // PDF content
            if (isLandscape) {
                // Landscape: fit to height, centered
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    currentBitmap.value?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPage.intValue + 1}",
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                        )
                    }
                }
            } else {
                // Portrait: scrollable, fill width
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                ) {
                    currentBitmap.value?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPage.intValue + 1}",
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.9f),
                            contentScale = ContentScale.Fit                        )
                    }
                }
            }
        }

        // Top controls - Close button and page indicator
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button
            Row(
                modifier = Modifier
                    .clickable { onClose() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    color = PrimaryOrange,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-3).dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Close",
                    color = PrimaryOrange,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Page indicator
            Text(
                text = "Page ${currentPage.intValue + 1} of ${totalPages.intValue}",
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryOrange,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom navigation
        if (totalPages.intValue > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (currentPage.intValue > 0) {
                            currentPage.intValue--
                        }
                    },
                    enabled = currentPage.intValue > 0,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonBlueDark,
                        contentColor = TextWhite,
                        disabledContainerColor = ButtonBlueDark.copy(alpha = 0.45f),
                        disabledContentColor = TextWhite.copy(alpha = 0.45f)
                    )
                ) {
                    Text("Previous", fontWeight = FontWeight.ExtraBold)
                }
                Button(
                    onClick = {
                        if (currentPage.intValue < totalPages.intValue - 1) {
                            currentPage.intValue++
                        }
                    },
                    enabled = currentPage.intValue < totalPages.intValue - 1,
                    modifier = Modifier.padding(start = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor,
                        contentColor = ButtonTextColor,
                        disabledContainerColor = ButtonColor.copy(alpha = 0.45f),
                        disabledContentColor = ButtonTextColor.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        "Next",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

private fun renderPage(
    pdfRenderer: PdfRenderer,
    pageIndex: Int,
    currentBitmap: MutableState<Bitmap?>,
    scaleFactor: Float = 6f
) {
    try {
        if (pageIndex < pdfRenderer.pageCount) {
            val page = pdfRenderer.openPage(pageIndex)
            // Scale up the bitmap for better quality
            val width = (page.width * scaleFactor).toInt()
            val height = (page.height * scaleFactor).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            currentBitmap.value = bitmap
            page.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
