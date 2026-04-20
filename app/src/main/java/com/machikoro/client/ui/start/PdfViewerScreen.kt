package com.machikoro.client.ui.start

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.machikoro.client.R
import com.github.barteksc.pdfviewer.PDFView
import java.io.File

@Composable
fun PdfViewerScreen(
    context: Context,
    fileName: String = "rules.pdf",
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // PDF Viewer
        AndroidView(
            factory = { ctx ->
                PDFView(ctx, null).apply {
                    val file = File(ctx.cacheDir, fileName)

                    // Copy from assets if not exists
                    if (!file.exists()) {
                        ctx.assets.open(fileName).use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    fromFile(file)
                        .enableSwipe(true)
                        .swipeHorizontal(true)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .enableAnnotationRendering(false)
                        .enableAntialiasing(true)
                        .spacing(0)
                        .autoSpacing(false)
                        .pageFitPolicy(com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle.FITTED_TO_WIDTH)
                        .fitEachPage(false)
                        .pageSnap(true)
                        .pageFling(true)
                        .load()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                contentDescription = "Close PDF",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

