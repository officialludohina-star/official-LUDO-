package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// index.html ke .card-real / .input-real / header-bar patterns — Facebook/Mobile/Gmail
// teeno login screens isi styling ko share karti hain.
val CardRealBg = Color(0xFFf5fff7)
val CardRealBorder = Color(0xFF0a7a42)

@Composable
fun LoginHeaderBar(
    title: String,
    headerColor: Color = Color.Transparent,
    padding: Int = 12,
    titleSize: Int = 22,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerColor)
            .padding(horizontal = 14.dp, vertical = padding.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(40.dp))
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = titleSize.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C8F72).copy(alpha = 0.82f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) { Text("\u2715", color = Color.White, fontSize = 25.sp) }
    }
}

// Yalla Ludo jaisa daba — gradient border (halka teal upar, gehra green nichay)
// mint/white card ke charo taraf, jo kisi bhi content height par sahi rehta hai.
val CardBorderGradient = Brush.verticalGradient(
    listOf(Color(0xFF6EE7B7), Color(0xFF17A863), Color(0xFF0a7a42))
)

// User ki di hui green card/background image — Gmail Login, Sign Up, Set/Reset
// Password, Mobile aur Facebook Login sab isi LoginCard ko share karte hain, is
// liye yahan ek jagah badalne se sab jagah apply ho jata hai.
private const val CARD_BG_IMG = "file:///android_asset/img/bghome-1295ddb.png"

@Composable
fun LoginCard(showTopBorder: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE6FBF5).copy(alpha = 0.94f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
fun RemoteImageButton(
    imageUrl: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.clickable(onClick = onClick)
    )
}

@Composable
fun RealInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFF9aa89e)) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color(0xFFc8e6c9),
            unfocusedBorderColor = Color(0xFFc8e6c9)
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun LinkRow(leftText: String, rightText: String, onLeft: () -> Unit, onRight: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            leftText, color = CardRealBorder, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onLeft)
        )
        Text(
            rightText, color = CardRealBorder, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onRight)
        )
    }
}
