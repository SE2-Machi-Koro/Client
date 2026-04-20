package com.machikoro.client.ui.start

import android.webkit.WebView
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
import java.io.File

@Composable
fun PdfViewerScreen(
    fileName: String = "rules.pdf",
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // PDF Viewer using WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    val file = File(ctx.cacheDir, fileName)

                    // Copy from assets if not exists
                    if (!file.exists()) {
                        ctx.assets.open(fileName).use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    @Suppress("SetJavaScriptEnabled")
                    settings.apply {
                        javaScriptEnabled = true
                        @Suppress("DeprecatedCall")
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    // Load PDF using Google Docs Viewer for better horizontal scaling
                    loadUrl("https://docs.google.com/gview?embedded=true&url=file://${file.absolutePath}")
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
                .background(Color.White.copy(alpha = 0.8f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                contentDescription = "Close PDF",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

