package com.example.data.images

import com.example.domain.model.IMAGES_KEY
import com.example.domain.model.RowImage
import org.json.JSONArray
import org.json.JSONObject

fun rowImagesFromJson(valuesJson: String): List<RowImage> {
    return try {
        val obj = JSONObject(valuesJson)
        val raw = obj.opt(IMAGES_KEY) ?: return emptyList()
        when (raw) {
            is JSONArray -> parseImageArray(raw)
            is String -> try {
                parseImageArray(JSONArray(raw))
            } catch (_: Exception) {
                emptyList()
            }
            else -> emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseImageArray(array: JSONArray): List<RowImage> =
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

fun rowImagesToJson(images: List<RowImage>): JSONArray = JSONArray().apply {
    images.forEach { image ->
        put(JSONObject().apply {
            put("uri", image.uri ?: JSONObject.NULL)
            put("remote", image.remote ?: JSONObject.NULL)
            put("caption", image.caption)
        })
    }
}

fun writeImagesToValuesJson(valuesJson: String, images: List<RowImage>): String {
    val obj = try {
        JSONObject(valuesJson)
    } catch (_: Exception) {
        JSONObject()
    }
    obj.put(IMAGES_KEY, rowImagesToJson(images))
    return obj.toString()
}
