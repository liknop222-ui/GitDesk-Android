package com.gitdesk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CardShape = RoundedCornerShape(18.dp)
val FieldShape = RoundedCornerShape(12.dp)
val PillShape = RoundedCornerShape(999.dp)

@Composable
fun GdCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Gd.c
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.card)
            .border(1.dp, c.hair, CardShape)
            .padding(16.dp)
    ) {
        if (title != null) {
            Text(title, color = c.text1, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        if (subtitle != null) {
            if (title != null) Spacer(Modifier.height(4.dp))
            Text(subtitle, color = c.text2, fontSize = 13.sp, lineHeight = 19.sp)
        }
        if (title != null || subtitle != null) Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun GdSectionTitle(text: String) {
    val c = Gd.c
    Text(
        text,
        color = c.text3,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun GdField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    secret: Boolean = false,
    singleLine: Boolean = true,
    lines: Int = 1
) {
    val c = Gd.c
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, fontSize = 13.sp) },
            placeholder = { Text(hint, fontSize = 14.sp, color = c.text3) },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else lines,
            maxLines = if (singleLine) 1 else lines * 3,
            shape = FieldShape,
            visualTransformation = if (secret) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = c.text1,
                unfocusedTextColor = c.text1,
                focusedBorderColor = c.ink,
                unfocusedBorderColor = c.hair,
                focusedLabelColor = c.text2,
                unfocusedLabelColor = c.text3,
                cursorColor = c.ink
            )
        )
    }
}

@Composable
fun GdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    filled: Boolean = true,
    danger: Boolean = false
) {
    val c = Gd.c
    val active = enabled && !loading
    val bg = when {
        danger -> Color(0xFFD70015)
        filled -> c.ink
        else -> c.card
    }
    val fg = when {
        danger -> Color.White
        filled -> c.paper
        else -> c.text1
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(FieldShape)
            .background(if (active) bg else c.card2)
            .border(1.dp, if (filled || danger) Color.Transparent else c.hair, FieldShape)
            .clickable(enabled = active) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = if (active) fg else c.text3,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text,
                color = if (active) fg else c.text3,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun GdChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Gd.c
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) c.ink else c.card)
            .border(1.dp, if (selected) Color.Transparent else c.hair, PillShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text,
            color = if (selected) c.paper else c.text2,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun GdSelect(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Gd.c
    var open by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, color = c.text3, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(FieldShape)
                    .background(c.paper)
                    .border(1.dp, c.hair, FieldShape)
                    .clickable { open = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(value, color = c.text1, fontSize = 15.sp)
                Text("▾", color = c.text3, fontSize = 14.sp)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, fontSize = 15.sp) },
                        onClick = {
                            open = false
                            onSelect(opt)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GdRow(label: String, value: String, valueColor: Color? = null) {
    val c = Gd.c
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = c.text2, fontSize = 14.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            color = valueColor ?: c.text1,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GdNote(text: String, warn: Boolean = false) {
    val c = Gd.c
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(if (warn) Color(0x14D70015) else c.card2)
            .padding(12.dp)
    ) {
        Text(
            text,
            color = if (warn) Color(0xFFD70015) else c.text2,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun GdStep(index: Int, title: String, body: String) {
    val c = Gd.c
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(PillShape)
                .background(c.ink),
            contentAlignment = Alignment.Center
        ) {
            Text("$index", color = c.paper, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = c.text1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(body, color = c.text2, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
fun GdStat(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Gd.c
    Column(
        modifier = modifier
            .clip(FieldShape)
            .background(c.card2)
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Text(value, color = c.text1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = c.text3, fontSize = 11.sp)
    }
}
