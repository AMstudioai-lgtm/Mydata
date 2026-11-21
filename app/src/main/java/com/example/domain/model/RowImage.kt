package com.example.domain.model

const val IMAGES_KEY = "__images__"
const val IMAGES_BUCKET = "row-images"
const val NOTES_KEY = "__notes__"
const val LEGACY_NOTES_KEY = "_page_notes"

data class RowImage(
    val uri: String? = null,
    val remote: String? = null,
    val caption: String = ""
)
