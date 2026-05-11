package com.example.fruitylicious.ui.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.window.PopupProperties

import androidx.compose.ui.focus.onFocusChanged

@Composable
fun FruityPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text)
        }
    }
}

@Composable
fun FruityOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(text = text)
    }
}

@Composable
fun FruityTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = text)
    }
}

@Composable
fun FruityTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable (() -> Unit))? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(label)
            },
            singleLine = singleLine,
            isError = isError,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color.White
            )
        )

        if (isError && !errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun FruitySectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF1B5E20)
        )

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6D6D6D)
            )
        }
    }
}

@Composable
fun FruityMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF2E7D32)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D6D6D)
                )
            }
        }
    }
}

@Composable
fun FruityInfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6D6D6D)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1B5E20)
            )
        }
    }
}

@Composable
fun FruityEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = title,
            tint = Color(0xFF9E9E9E)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF777777)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FruityDatePicker(
    state: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
    onClear: () -> Unit = {}
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(state.selectedDateMillis)
                    onDismiss()
                }
            ) {
                Text("OK", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onClear()
                    onDismiss()
                }) {
                    Text("Clear", color = Color.Gray)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = Color.White
        )
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                todayContentColor = Color(0xFF2E7D32),
                todayDateBorderColor = Color(0xFF2E7D32),
                selectedDayContainerColor = Color(0xFF2E7D32),
                selectedDayContentColor = Color.White,
                selectedYearContainerColor = Color(0xFF2E7D32),
                selectedYearContentColor = Color.White,
                titleContentColor = Color(0xFF1B5E20),
                headlineContentColor = Color(0xFF1B5E20)
            )
        )
    }
}

@Composable
fun FruityDateFilterField(
    selectedDateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "mm/dd/yyyy",
    onClear: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedDateText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = label,
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color.LightGray
                )
            },
            trailingIcon = {
                if (selectedDateText.isEmpty() || onClear == null) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color.Black
                    )
                } else {
                    // Spacer for the clear button
                    Spacer(modifier = Modifier.size(24.dp))
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFEEEEEE),
                focusedBorderColor = Color(0xFF2E7D32),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = if (selectedDateText.isNotEmpty() && onClear != null) 48.dp else 0.dp)
                .clickable {
                    onClick()
                }
        )

        if (selectedDateText.isNotEmpty() && onClear != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear date",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun FruitySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search..."
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = Color.LightGray,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.LightGray
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = Color.Gray
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFEEEEEE),
            focusedBorderColor = Color(0xFF2E7D32),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FruitySearchableDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<T>,
    onOptionClick: (T) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose an option",
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && !expanded) onExpandedChange(true)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155)
            )

            Spacer(modifier = Modifier.heightIn(8.dp))
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    onValueChange(it)
                    if (!expanded) onExpandedChange(true)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !expanded) {
                            onExpandedChange(true)
                        }
                    },
                placeholder = {
                    Text(
                        text = placeholder,
                        color = Color.LightGray
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (value.isNotEmpty()) {
                                onValueChange("")
                                if (!expanded) onExpandedChange(true)
                            } else {
                                onExpandedChange(!expanded)
                            }
                        }
                    ) {
                        val icon = when {
                            value.isNotEmpty() -> Icons.Default.Close
                            expanded -> Icons.Default.Close
                            else -> Icons.Default.ArrowDropDown
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = if (value.isNotEmpty()) "Clear" else "Toggle",
                            tint = Color.Gray
                        )
                    }
                },
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedBorderColor = Color(0xFF2E7D32),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                singleLine = true
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .exposedDropdownSize()
                    .background(Color.White)
                    .heightIn(max = 280.dp),
                properties = PopupProperties(focusable = false)
            ) {
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No results found", color = Color.Gray) },
                        onClick = { }
                    )
                } else {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { itemContent(option) },
                            onClick = {
                                onOptionClick(option)
                                onExpandedChange(false)
                            }
                        )
                    }
                }
            }
        }
    }
}
