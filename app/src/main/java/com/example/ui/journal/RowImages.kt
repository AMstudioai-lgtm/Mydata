package com.example.ui.journal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
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
import androidx.core.content.FileProvider
import com.example.data.images.RowImage
import com.example.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Zone de saisie libre d'une ligne, héritière de l'ancien bloc de notes. */
const val NOTES_KEY = "__notes__"

/** Clé notes utilisée avant la suppression du bloc dédié (v1.2.0). */
const val LEGACY_NOTES_KEY = "_page_notes"

/** Texte libre de la ligne. Les anciennes notes `_page_notes` sont relues tant qu'elles existent. */
fun readNotes(valuesJson: String): String {
    return try {
        val obj = JSONObject(valuesJson)
        when {
            obj.has(NOTES_KEY) -> obj.optString(NOTES_KEY, "")
            else -> obj.optString(LEGACY_NOTES_KEY, "")
        }
    } catch (_: Exception) {
        ""
    }
}

/**
 * Import et affichage des images d'une ligne, sans dépendance externe (pas de Coil).
 *
 * Stratégie : le sélecteur système ne donne qu'un accès temporaire à l'image, donc chaque
 * image est copiée dans le stockage interne de l'application (`filesDir/row_images`) au
 * moment du choix. Ce qui est stocké dans `valuesJson` est une URI `file://` interne :
 * elle survit à la fermeture de l'écran, au redémarrage et ne demande aucune permission
 * `READ_MEDIA_IMAGES`. Seul le sélecteur système lit le média original.
 */
private object RowImageStore {

    private const val DIRECTORY = "row_images"

    /** Plus grand côté conservé à l'import, en pixels : au-delà, le fichier est rééchantillonné. */
    private const val IMPORT_MAX_PX = 1600

    /** Plus grand côté des vignettes affichées, en pixels. */
    private const val THUMBNAIL_MAX_PX = 640

    private val thumbnails = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    /** Copie une image choisie dans le stockage interne et renvoie son URI `file://`. */
    suspend fun importImage(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        var target: File? = null
        try {
            val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
            target = File(dir, "${UUID.randomUUID()}.jpg")
            val stream = context.contentResolver.openInputStream(source)
            if (stream == null) {
                return@withContext null
            }
            stream.use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
            downscale(target, IMPORT_MAX_PX)
            Uri.fromFile(target).toString()
        } catch (_: Exception) {
            target?.delete()
            null
        }
    }

    /** Fichier prêt à recevoir une photo prise par l'appareil photo. */
    fun newCaptureFile(context: Context): File {
        val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    /** URI partagée à l'appareil photo via [FileProvider] (obligatoire depuis l'API 24). */
    fun captureUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Décodage hors du thread principal, mémorisé par URI. `null` si l'image est illisible. */
    suspend fun loadThumbnail(context: Context, uriString: String): Bitmap? {
        thumbnails.get(uriString)?.let { return it }
        return withContext(Dispatchers.IO) {
            val bytes = readBytes(context, uriString) ?: return@withContext null
            val bitmap = decodeSampled(bytes, THUMBNAIL_MAX_PX) ?: return@withContext null
            thumbnails.put(uriString, bitmap)
            bitmap
        }
    }

    private fun readBytes(context: Context, uriString: String): ByteArray? {
        return try {
            val uri = Uri.parse(uriString)
            when (uri.scheme) {
                "file" -> uri.path?.let { File(it).takeIf(File::canRead)?.readBytes() }
                else -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Réduit le plus grand côté à [maxPx]. Écriture atomique : le résultat part dans un
     * fichier temporaire qui remplace l'original seulement si l'encodage a réussi, sinon
     * une image corrompue resterait à la place de l'original.
     */
    private fun downscale(file: File, maxPx: Int) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= maxPx || longest <= 0) return

        val options = BitmapFactory.Options().apply {
            // Puissance de deux la plus proche par valeur inférieure : 3200 -> 800 (maxPx 1600).
            var sample = 1
            while (longest / (sample * 2) >= maxPx) sample *= 2
            inSampleSize = sample
        }
        val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return

        val temp = File(file.parentFile, "${file.name}.tmp")
        try {
            val compressed = FileOutputStream(temp).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            if (compressed && file.delete() && temp.renameTo(file)) {
                return
            }
            temp.delete()
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeSampled(bytes: ByteArray, maxPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        val options = BitmapFactory.Options().apply {
            var sample = 1
            while (longest / (sample * 2) >= maxPx) sample *= 2
            inSampleSize = sample
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}

/**
 * Section images d'une ligne, enchaînement Notion : zone d'ajout discrète puis images
 * empilées verticalement, chacune en pleine largeur avec sa légende et sa croix.
 * Les nouvelles images s'ajoutent en dessous des précédentes.
 */
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

    // Sélecteur système de photos : aucune permission de stockage n'est demandée.
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { picked ->
        if (picked.isNotEmpty()) {
            scope.launch {
                val imported = picked.mapNotNull { uri -> RowImageStore.importImage(context, uri) }
                if (imported.isNotEmpty()) onImagesChanged(images + imported.map { RowImage(uri = it) })
            }
        }
    }

    // Appareil photo : la photo est écrite directement dans le stockage interne.
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

            // Choix de la source, façon menu de commande Notion.
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
                        val file = RowImageStore.newCaptureFile(context)
                        pendingCaptureFile = file
                        cameraLauncher.launch(RowImageStore.captureUri(context, file))
                    }
                )
            }
        }

        // Pile verticale pleine largeur. Pas de LazyColumn : la page est déjà scrollable,
        // et deux scrolls verticaux imbriqués sont interdits.
        images.forEachIndexed { index, image ->
            key(image.uri ?: image.remote ?: index) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Pas de cadre imposé : le conteneur épouse la hauteur réelle
                    // de l'image, affichée en entier (Fit), sans rognage.
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when {
                            image.uri != null -> ImageThumbnail(
                                uri = image.uri,
                                modifier = Modifier.fillMaxWidth()
                            )
                            image.remote != null -> CloudPlaceholder(modifier = Modifier.fillMaxWidth())
                            else -> UnavailablePlaceholder(modifier = Modifier.fillMaxWidth())
                        }

                        // Croix de suppression, dans un badge sombre pour rester lisible
                        // quelle que soit l'image derrière.
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(24.dp)
                                .background(AppColors.Background.copy(alpha = 0.8f), CircleShape)
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

                    Spacer(modifier = Modifier.height(4.dp))
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

/** Zone d'ajout : survol et clic donnent le même retour visuel, bouton discret. */
@Composable
private fun AddImageZone(onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background = if (pressed) AppColors.SelectedBg.copy(alpha = 0.45f) else AppColors.Panel

    Surface(
        color = background,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, AppColors.Border),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
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
                tint = AppColors.AccentLight,
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

/**
 * Champ de légende sous une image. Buffer local comme le titre : la légende est réécrite
 * en base à chaque frappe, donc la value reviendrait à la fin et le curseur sauterait
 * sans ce buffer. Resynchronisé depuis le store quand le champ n'a pas le focus
 * (suppression, réouverture, sync).
 */
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

/** Image locale affichée en entier, sans cadre ni rognage. Placeholder si illisible. */
@Composable
private fun ImageThumbnail(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = RowImageStore.loadThumbnail(context, uri)
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

/** Image téléversée dont le fichier local est absent : le téléchargement viendra d'un autre chantier. */
@Composable
private fun CloudPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(AppColors.Background, RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
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

/** Image locale devenue inaccessible : pas de plantage, un simple emplacement discret. */
@Composable
private fun UnavailablePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(AppColors.Background, RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
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
