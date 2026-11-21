package com.example.data.db

import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.PropertyEntity
import com.example.domain.model.TablePropertyData
import com.example.domain.model.TableRowData

fun PropertyEntity.toDomain(): TablePropertyData = TablePropertyData(
    id = id,
    name = name,
    kind = kind,
    format = format,
    optionsJson = optionsJson
)

fun EntryRowEntity.toDomain(): TableRowData = TableRowData(
    id = id,
    valuesJson = valuesJson,
    createdAt = createdAt
)
