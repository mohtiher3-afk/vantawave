package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.ExportRecordEntity
import com.example.data.VisualizerProjectEntity
import com.example.ui.theme.VantaBackground
import com.example.ui.theme.VantaPrimary
import com.example.ui.theme.VantaSecondary
import com.example.ui.theme.VantaSurface
import com.example.ui.theme.VantaSurfaceElevated
import com.example.ui.theme.VantaSurfaceVariant
import com.example.ui.theme.VantaTertiary
import com.example.ui.theme.VantaTextMuted
import com.example.ui.theme.VantaTextPrimary
import com.example.ui.theme.VantaTextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(
    projects: List<VisualizerProjectEntity>,
    exports: List<ExportRecordEntity>,
    onOpenProject: (VisualizerProjectEntity) -> Unit,
    onCloneProject: (VisualizerProjectEntity) -> Unit,
    onDeleteProject: (VisualizerProjectEntity) -> Unit,
    onDeleteExport: (ExportRecordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Saved Projects, 1: Exported Videos
    var projectToDelete by remember { mutableStateOf<VisualizerProjectEntity?>(null) }
    var exportToDelete by remember { mutableStateOf<ExportRecordEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
            .testTag("projects_screen")
    ) {
        // Title
        Text(
            text = "Projects & Exports",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = VantaTextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = VantaPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = VantaSecondary,
                    height = 3.dp
                )
            },
            divider = {}
        ) {
            listOf("Saved Projects (${projects.size})", "Exported Videos (${exports.size})").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) VantaTextPrimary else VantaTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Saved Projects Tab
            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved projects yet.\nTap the Bookmark button in Visualizer Studio to save!",
                        color = VantaTextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(projects) { project ->
                        val primColor = try {
                            Color(android.graphics.Color.parseColor(project.primaryColorHex))
                        } catch (e: Exception) {
                            VantaPrimary
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenProject(project) }
                                .testTag("project_item_${project.id}"),
                            color = VantaSurface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(primColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = primColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VantaTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${project.mode} • ${project.trackTitle}",
                                        fontSize = 12.sp,
                                        color = VantaTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(onClick = { onCloneProject(project) }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Duplicate",
                                        tint = VantaSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = { projectToDelete = project }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = VantaTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(90.dp)) }
                }
            }
        } else {
            // Exported Videos Tab
            if (exports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No exported videos yet.\nCreate your first video in Visualizer Studio!",
                        color = VantaTextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(exports) { export ->
                        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(export.createdAt))
                        val sizeMb = "%.1f MB".format(export.fileSizeBytes / (1024f * 1024f))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            color = VantaSurface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(VantaSecondary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = VantaSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = export.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VantaTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${export.aspectRatio} • ${export.resolution} • $sizeMb",
                                        fontSize = 12.sp,
                                        color = VantaTextSecondary
                                    )
                                    Text(
                                        text = dateStr,
                                        fontSize = 10.sp,
                                        color = VantaTextMuted
                                    )
                                }

                                // Play button
                                IconButton(
                                    onClick = {
                                        playExportedVideo(context, File(export.filePath))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = VantaPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Share button
                                IconButton(
                                    onClick = {
                                        shareExportedVideo(context, File(export.filePath))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = VantaSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Delete button
                                IconButton(onClick = { exportToDelete = export }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = VantaTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(90.dp)) }
                }
            }
        }
    }

    // Confirmation dialog: Delete Project
    projectToDelete?.let { prj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project?", color = VantaTextPrimary) },
            text = { Text("Are you sure you want to delete '${prj.name}'?", color = VantaTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(prj)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VantaTertiary)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = { projectToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = VantaSurfaceVariant)
                ) {
                    Text("Cancel", color = VantaTextPrimary)
                }
            },
            containerColor = VantaSurfaceElevated
        )
    }

    // Confirmation dialog: Delete Export
    exportToDelete?.let { exp ->
        AlertDialog(
            onDismissRequest = { exportToDelete = null },
            title = { Text("Delete Exported Video?", color = VantaTextPrimary) },
            text = { Text("Are you sure you want to delete '${exp.title}'?", color = VantaTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExport(exp)
                        exportToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VantaTertiary)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = { exportToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = VantaSurfaceVariant)
                ) {
                    Text("Cancel", color = VantaTextPrimary)
                }
            },
            containerColor = VantaSurfaceElevated
        )
    }
}

private fun playExportedVideo(context: Context, file: File) {
    if (!file.exists()) return
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

private fun shareExportedVideo(context: Context, file: File) {
    if (!file.exists()) return
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Music Video"))
    } catch (e: Exception) {
        // Fallback
    }
}
