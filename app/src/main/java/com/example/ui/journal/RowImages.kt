package com.example.ui.journal

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.images.RowImageFileStore
import com.example.domain.model.RowImage
import com.example.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RowImagesSection(
    images: List<RowImage>,
    onImagesChanged: (List<RowImage>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { picked ->
        if (picked.isNotEmpty()) {
            scope.launch {
                val imported = picked.mapNotNull { uri -> RowImageFileStore.importImage(context, uri) }
                if (imported.isNotEmpty()) onImagesChanged(images + imported.map { RowImage(uri = it) })
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCaptureFile
        pendingCaptureFile = null
        if (success && file != null && file.exists()) {
            onImagesChanged(images + RowImage(uri = Uri.fromFile(file).toString()))
        } else {
            file?.delete()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AddImageZone(
                onClick = { sourceMenuExpanded = true },
                label = if (images.isEmpty()) "Ajouter une image" else "Ajouter une autre image"
            )

            DropdownMenu(
                expanded = sourceMenuExpanded,
                onDismissRequest = { sourceMenuExpanded = false },
                modifier = Modifier.background(AppColors.Panel)
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = AppColors.Accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Depuis l'appareil", color = AppColors.TextPrimary, fontSize = 13.sp)
                        }
                    },
                    onClick = {
                        sourceMenuExpanded = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = AppColors.Accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Appareil photo", color = AppColors.TextPrimary, fontSize = 13.sp)
                        }
                    },
                    onClick = {
                        sourceMenuExpanded = false
                        val file = RowImageFileStore.newCaptureFile(context)
                        pendingCaptureFile = file
                        cameraLauncher.launch(RowImageFileStore.captureUri(context, file))
                    }
                )
            }
        }

        images.forEachIndexed { index, image ->
            key(image.uri ?: image.remote ?: index) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        when {
                            image.uri != null -> ImageThumbnail(
                                uri = image.uri,
                                modifier = Modifier.fillMaxWidth()
                            )
                            image.remote != null -> CloudPlaceholder(modifier = Modifier.fillMaxWidth())
                            else -> UnavailablePlaceholder(modifier = Modifier.fillMaxWidth())
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(26.dp)
                                .background(AppColors.Background.copy(alpha = 0.85f), CircleShape)
                                .border(1.dp, AppColors.Border, CircleShape)
                                .clickable {
                                    onImagesChanged(images.filterIndexed { i, _ -> i != index })
                                }
                                .testTag("row_image_delete_${index}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Supprimer l'image",
                                tint = AppColors.TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    CaptionField(
                        caption = image.caption,
                        index = index,
                        onCaptionChanged = { newCaption ->
                            onImagesChanged(
                                images.mapIndexed { i, item ->
                                    if (i == index) item.copy(caption = newCaption) else item
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddImageZone(onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background = if (pressed) AppColors.SelectedBg else AppColors.Panel

    Surface(
        color = background,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, AppColors.Border),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag("row_images_add_zone")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = AppColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CaptionField(
    caption: String,
    index: Int,
    onCaptionChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var buffer by remember { mutableStateOf(TextFieldValue(caption, TextRange(caption.length))) }
    var isFocused by remember { mutableStateOf(false) }
    if (!isFocused && buffer.text != caption) {
        buffer = TextFieldValue(caption, TextRange(caption.length))
    }

    BasicTextField(
        value = buffer,
        onValueChange = { newValue ->
            buffer = newValue
            onCaptionChanged(newValue.text)
        },
        textStyle = TextStyle(
            color = AppColors.TextSecondary,
            fontSize = 13.sp
        ),
        cursorBrush = SolidColor(AppColors.Accent),
        singleLine = false,
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Panel, RoundedCornerShape(6.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .testTag("row_image_caption_${index}"),
        decorationBox = { innerTextField ->
            if (buffer.text.isEmpty()) {
                Text(
                    text = "Légende…",
                    color = AppColors.TextMuted,
                    fontSize = 13.sp
                )
            }
            innerTextField()
        }
    )
}

@Composable
private fun ImageThumbnail(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = RowImageFileStore.loadThumbnail(context, uri)
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded.asImageBitmap(),
            contentDescription = "Image de la ligne",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        UnavailablePlaceholder(modifier = modifier)
    }
}

@Composable
private fun CloudPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(AppColors.Panel, RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = AppColors.TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Image cloud",
                color = AppColors.TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun UnavailablePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(AppColors.Panel, RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                tint = AppColors.TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Image indisponible",
                color = AppColors.TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
