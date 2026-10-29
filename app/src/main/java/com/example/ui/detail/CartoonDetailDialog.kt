package com.example.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.download.DownloadHelper
import com.example.data.model.CartoonItem
import com.example.ui.theme.CinemaDark
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.RetroGold
import com.example.ui.theme.RetroRuby

@Composable
fun CartoonDetailDialog(
    cartoon: CartoonItem,
    onDismiss: () -> Unit,
    onPlay: (CartoonItem) -> Unit,
    onToggleWatchlist: (CartoonItem) -> Unit,
    onDownload: ((CartoonItem) -> Unit)? = null,
    onCopyStreamUrl: ((CartoonItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val downloadHelper = remember { DownloadHelper(context) }
    val playFocusRequester = remember { FocusRequester() }

    LaunchedEffect(cartoon.id) {
        kotlinx.coroutines.delay(140)
        try {
            playFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val isWideOrLandscape = maxWidth >= 600.dp || maxHeight <= 520.dp
            var closeFocused by remember { mutableStateOf(false) }
            var playFocused by remember { mutableStateOf(false) }
            var watchlistFocused by remember { mutableStateOf(false) }
            var downloadFocused by remember { mutableStateOf(false) }
            var sourceFocused by remember { mutableStateOf(false) }
            var copyFocused by remember { mutableStateOf(false) }

            val playScale by animateFloatAsState(
                targetValue = if (playFocused) 1.04f else 1.0f,
                animationSpec = spring(),
                label = "detail_play_scale"
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CinemaDark,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .widthIn(max = if (isWideOrLandscape) 640.dp else 520.dp)
                    .fillMaxWidth(if (isWideOrLandscape) 0.88f else 0.94f)
                    .heightIn(max = (maxHeight * 0.94f).coerceAtMost(640.dp))
                    .border(1.5.dp, RetroGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .testTag("cartoon_detail_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with adaptive thumbnail height & close button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isWideOrLandscape) {
                                    Modifier.height(175.dp)
                                } else {
                                    Modifier.aspectRatio(16f / 9f)
                                }
                            )
                            .background(CinemaSurface)
                    ) {
                        AsyncImage(
                            model = cartoon.thumbnailUrl,
                            contentDescription = cartoon.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, CinemaDark)
                                    )
                                )
                        )

                        // Close button with D-Pad focus highlight
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(42.dp)
                                .onFocusChanged { closeFocused = it.isFocused }
                                .background(
                                    if (closeFocused) RetroGold else Color.Black.copy(alpha = 0.65f),
                                    RoundedCornerShape(21.dp)
                                )
                                .border(
                                    width = if (closeFocused) 2.dp else 1.dp,
                                    color = if (closeFocused) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(21.dp)
                                )
                                .testTag("detail_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (closeFocused) CinemaDark else Color.White
                            )
                        }

                        // Duration / Year badge
                        Surface(
                            color = Color.Black.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "${cartoon.year} • ${cartoon.runtime}",
                                color = RetroGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Text(
                            text = cartoon.title,
                            color = Color.White,
                            fontSize = if (isWideOrLandscape) 19.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 25.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = cartoon.creator,
                            color = RetroGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = RetroRuby.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RetroRuby)
                            ) {
                                Text(
                                    text = "PUBLIC DOMAIN (PRE-1960)",
                                    color = RetroRuby,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

                            Surface(
                                color = CinemaSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "INTERNET ARCHIVE",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Synopsis
                        Text(
                            text = cartoon.description.ifBlank {
                                "Classic restored vintage animation preserved from the Internet Archive public domain collection."
                            },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            maxLines = if (isWideOrLandscape) 3 else 5,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons (Play & Watchlist)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onPlay(cartoon) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RetroGold,
                                    contentColor = CinemaDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .graphicsLayer {
                                        scaleX = playScale
                                        scaleY = playScale
                                    }
                                    .focusRequester(playFocusRequester)
                                    .onFocusChanged { playFocused = it.isFocused }
                                    .then(
                                        if (playFocused) {
                                            Modifier.border(2.5.dp, Color.White, RoundedCornerShape(8.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .testTag("detail_play_button")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Stream", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                            }

                            OutlinedButton(
                                onClick = { onToggleWatchlist(cartoon) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (cartoon.isFavorite || watchlistFocused) RetroGold else Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (watchlistFocused) 2.dp else 1.dp,
                                    color = if (watchlistFocused) RetroGold else Color.White.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(46.dp)
                                    .onFocusChanged { watchlistFocused = it.isFocused }
                                    .testTag("detail_watchlist_button")
                            ) {
                                Icon(
                                    imageVector = if (cartoon.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (cartoon.isFavorite || watchlistFocused) RetroGold else Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (cartoon.isFavorite) "Saved" else "Watchlist", maxLines = 1, softWrap = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary Actions: Download Offline, Archive Link, Copy Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (onDownload != null) {
                                        onDownload(cartoon)
                                    } else {
                                        downloadHelper.downloadCartoon(cartoon, cartoon.getResolvedStreamUrl())
                                    }
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (downloadFocused) 2.dp else 1.dp,
                                    color = if (downloadFocused) RetroGold else Color.White.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { downloadFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = if (downloadFocused) RetroGold else Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Download",
                                    color = if (downloadFocused) RetroGold else Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cartoon.archiveDetailsUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clip.setPrimaryClip(ClipData.newPlainText("Archive Page", cartoon.archiveDetailsUrl))
                                        Toast.makeText(context, "Archive page URL copied", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (sourceFocused) 2.dp else 1.dp,
                                    color = if (sourceFocused) RetroGold else Color.White.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { sourceFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    tint = if (sourceFocused) RetroGold else Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Source",
                                    color = if (sourceFocused) RetroGold else Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (onCopyStreamUrl != null) {
                                        onCopyStreamUrl(cartoon)
                                    } else {
                                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clip.setPrimaryClip(ClipData.newPlainText("Stream URL", cartoon.getResolvedStreamUrl()))
                                        Toast.makeText(context, "Stream link copied", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (copyFocused) 2.dp else 1.dp,
                                    color = if (copyFocused) RetroGold else Color.White.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { copyFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (copyFocused) RetroGold else Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Copy",
                                    color = if (copyFocused) RetroGold else Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
