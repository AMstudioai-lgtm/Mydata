package com.example.domain.model

data class TableSort(
    val propertyId: String? = null,
    val ascending: Boolean? = null
)

enum class CalculationMode {
    SUM,
    AVG;

    fun toggle(): CalculationMode = if (this == SUM) AVG else SUM
}

data class ColumnNumberStats(
    val sum: Double = 0.0,
    val avg: Double = 0.0
)

data class TablePropertyData(
    val id: String,
    val name: String,
    val kind: String,
    val format: String? = null,
    val optionsJson: String = "[]"
)

data class TableRowData(
    val id: String,
    val valuesJson: String,
    val createdAt: Long
)
