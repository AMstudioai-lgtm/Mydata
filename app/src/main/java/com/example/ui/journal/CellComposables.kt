package com.example.ui.journal

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FORMAT_DOLLAR
import com.example.data.FORMAT_PERCENT
import com.example.data.parseNumberInput
import com.example.ui.theme.AppColors
import org.json.JSONArray
import java.util.Calendar
import java.util.Locale

@Composable
fun TextCell(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textState by remember(value) { mutableStateOf(value) }

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = textState,
            onValueChange = {
                textState = it
            },
            singleLine = true,
            textStyle = TextStyle(
                color = AppColors.TextPrimary,
                fontSize = 13.sp
            ),
            cursorBrush = SolidColor(AppColors.Accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onValueChange(textState)
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && textState != value) {
                        onValueChange(textState)
                    }
                }
                .testTag("text_cell_input"),
            decorationBox = { innerTextField ->
                if (textState.isEmpty()) {
                    Text(
                        text = "—",
                        color = AppColors.TextMuted,
                        fontSize = 13.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun NumberCell(
    value: String,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    // null vaut "plain" : les colonnes sans sous-format gardent l'affichage historique.
    format: String? = null
) {
    var textState by remember(value) { mutableStateOf(value) }

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Le symbole est purement décoratif : la valeur stockée reste un nombre brut.
            if (format == FORMAT_DOLLAR) {
                Text(
                    text = "$",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            BasicTextField(
                value = textState,
                onValueChange = {
                    textState = it
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = AppColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(AppColors.Accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onValueChange(parseNumberInput(textState))
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && textState != value) {
                            onValueChange(parseNumberInput(textState))
                        }
                    }
                    .testTag("number_cell_input"),
                decorationBox = { innerTextField ->
                    if (textState.isEmpty()) {
                        Text(
                            text = "0.0",
                            color = AppColors.TextMuted,
                            fontSize = 13.sp
                        )
                    }
                    innerTextField()
                }
            )

            if (format == FORMAT_PERCENT) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "%",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun DateCell(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    val showDatePicker = {
        val dateParts = value.split("-")
        var year = calendar.get(Calendar.YEAR)
        var month = calendar.get(Calendar.MONTH)
        var day = calendar.get(Calendar.DAY_OF_MONTH)

        if (dateParts.size == 3) {
            dateParts[0].toIntOrNull()?.let { year = it }
            dateParts[1].toIntOrNull()?.let { month = it - 1 }
            dateParts[2].toIntOrNull()?.let { day = it }
        }

        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formatted = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )
                onValueChange(formatted)
            },
            year,
            month,
            day
        ).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { showDatePicker() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("date_cell_picker"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Choisir date",
                tint = if (value.isNotEmpty()) AppColors.Accent else AppColors.TextMuted,
                modifier = Modifier.height(13.dp)
            )
            Text(
                text = value.ifEmpty { "Choisir" },
                color = if (value.isNotEmpty()) AppColors.TextPrimary else AppColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SelectCell(
    value: String,
    optionsJson: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val options = remember(optionsJson) {
        try {
            val array = JSONArray(optionsJson)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { expanded = true }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("select_cell_dropdown"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional colored badge for Long / Short / Gain / Perte style options
            val isPositive = value.equals("Gain", ignoreCase = true) || value.equals("Long", ignoreCase = true)
            val isNegative = value.equals("Perte", ignoreCase = true) || value.equals("Short", ignoreCase = true)

            val badgeBg = when {
                isPositive -> AppColors.SuccessGreen.copy(alpha = 0.2f)
                isNegative -> AppColors.ErrorRed.copy(alpha = 0.2f)
                value.isNotEmpty() -> AppColors.SelectedBg
                else -> androidx.compose.ui.graphics.Color.Transparent
            }
            val badgeText = when {
                isPositive -> AppColors.SuccessGreen
                isNegative -> AppColors.ErrorRed
                value.isNotEmpty() -> AppColors.TextPrimary
                else -> AppColors.TextMuted
            }

            Box(
                modifier = Modifier
                    .background(badgeBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = if (value.isNotEmpty()) 6.dp else 0.dp, vertical = 2.dp)
            ) {
                Text(
                    text = value.ifEmpty { "Choisir…" },
                    color = badgeText,
                    fontSize = 12.sp,
                    fontWeight = if (value.isNotEmpty()) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Options",
                tint = AppColors.TextMuted,
                modifier = Modifier.height(14.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColors.Panel)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = AppColors.TextPrimary,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
