package com.example.data.images

import org.json.JSONArray
import org.json.JSONObject

/**
 * Clé réservée de `valuesJson` pour les images d'une ligne.
 *
 * Aucun risque de collision : les valeurs de propriétés sont stockées sous l'UUID de la
 * propriété, jamais sous son nom. Le préfixe `__` est donc réservé à l'application.
 *
 * Format canonique :
 * ```
 * "__images__": [
 *   {"uri": "content://...", "remote": "USERID/ROWID/UUID.jpg", "caption": "légende"},
 *   ...
 * ]
 * ```
 * - `uri` : URI locale affichable immédiatement (peut être absente après réinstallation).
 * - `remote` : chemin dans le bucket Supabase `row-images`, présent une fois téléversé.
 * - `caption` : légende saisie sous l'image.
 *
 * Fichier de contrat : aucune des 5 tâches v1.3.0 ne doit modifier ce fichier,
 * toutes doivent l'utiliser tel quel.
 */
const val IMAGES_KEY = "__images__"

/** Bucket Supabase Storage recevant les images des lignes. */
const val IMAGES_BUCKET = "row-images"

data class RowImage(
    val uri: String? = null,
    val remote: String? = null,
    val caption: String = ""
)

/**
 * Lecture tolérante, ne lève jamais :
 * - objet {uri, remote, caption} (format canonique),
 * - ancienne chaîne simple (versions précédentes : vaut `uri`, le reste est vide),
 * - clé absente ou JSON invalide → liste vide.
 */
fun rowImagesFromJson(valuesJson: String): List<RowImage> {
    return try {
        val obj = JSONObject(valuesJson)
        val raw = obj.opt(IMAGES_KEY) ?: return emptyList()
        when (raw) {
            is JSONArray -> (0 until raw.length()).mapNotNull { index ->
                when (val item = raw.opt(index)) {
                    is JSONObject -> RowImage(
                        uri = item.optString("uri", "").ifEmpty { null },
                        remote = item.optString("remote", "").ifEmpty { null },
                        caption = item.optString("caption", "")
                    )
                    is String -> item.ifEmpty { null }?.let { RowImage(uri = it) }
                    else -> null
                }
            }
            // Double encodage défensif : la valeur est une chaîne contenant un tableau JSON.
            is String -> try {
                val array = JSONArray(raw)
                (0 until array.length()).mapNotNull { index ->
                    when (val item = array.opt(index)) {
                        is JSONObject -> RowImage(
                            uri = item.optString("uri", "").ifEmpty { null },
                            remote = item.optString("remote", "").ifEmpty { null },
                            caption = item.optString("caption", "")
                        )
                        is String -> item.ifEmpty { null }?.let { RowImage(uri = it) }
                        else -> null
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
            else -> emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Sérialisation canonique : toujours un tableau d'objets, même pour une image locale seule. */
fun rowImagesToJson(images: List<RowImage>): JSONArray = JSONArray().apply {
    images.forEach { image ->
        put(JSONObject().apply {
            put("uri", image.uri ?: JSONObject.NULL)
            put("remote", image.remote ?: JSONObject.NULL)
            put("caption", image.caption)
        })
    }
}

/**
 * Réécrit le tableau d'images dans le JSON de la ligne en conservant toutes les autres clés.
 * Ne lève jamais : en cas de JSON d'entrée invalide, repart d'un objet vide.
 */
fun writeImagesToValuesJson(valuesJson: String, images: List<RowImage>): String {
    val obj = try {
        JSONObject(valuesJson)
    } catch (_: Exception) {
        JSONObject()
    }
    obj.put(IMAGES_KEY, rowImagesToJson(images))
    return obj.toString()
}
