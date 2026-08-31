package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
    useBackArrow: Boolean = false,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerColor)
            .padding(horizontal = 14.dp, vertical = padding.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (useBackArrow) {
            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Text("\u2190", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        } else {
            Spacer(Modifier.width(40.dp))
        }
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = titleSize.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        if (useBackArrow) {
            Spacer(Modifier.width(40.dp))
        } else {
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

// ================= LIVE SIZE/POSITION DEBUG PANEL =================
// Chota gear (⚙) button screen par tap karo to yeh panel khul jata hai.
// Har row ke [-] [+] dabao to size/position turant phone par change hoti
// dikhegi — jo number sahi lage wo mujhe bata dena, code mein baak diya
// jayega. Panel sirf testing ke liye hai, isay kabhi bhi off kiya ja sakta hai.

@Composable
fun DebugToggleButton(visible: MutableState<Boolean>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { visible.value = !visible.value },
        contentAlignment = Alignment.Center
    ) { Text("\u2699", color = Color.White, fontSize = 18.sp) }
}

@Composable
fun DebugStepperRow(
    label: String,
    value: MutableState<Int>,
    step: Int = 4,
    range: IntRange = -150..400
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$label: ${value.value}",
            color = Color.White, fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF444444))
                .clickable { value.value = (value.value - step).coerceIn(range) },
            contentAlignment = Alignment.Center
        ) { Text("\u2212", color = Color.White, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0a7a42))
                .clickable { value.value = (value.value + step).coerceIn(range) },
            contentAlignment = Alignment.Center
        ) { Text("+", color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun DebugPanel(visible: MutableState<Boolean>, content: @Composable ColumnScope.() -> Unit) {
    if (!visible.value) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
            .heightIn(max = 260.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Size Panel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                "close", color = Color(0xFF9ad6b8), fontSize = 12.sp,
                modifier = Modifier.clickable { visible.value = false }
            )
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}
// =====================================================================

@Composable
fun EyeToggleIcon(visible: Boolean, modifier: Modifier = Modifier) {
    // Ankh ka icon — password chupa ho (visible = false) to ankh par ek "/"
    // line aa jati hai, dekhna ho (visible = true) to seedhi khuli ankh.
    Box(modifier = modifier.size(22.dp), contentAlignment = Alignment.Center) {
        Text("\uD83D\uDC41\uFE0F", fontSize = 18.sp)
        if (!visible) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .rotate(-45f)
                    .background(Color(0xFF555555))
            )
        }
    }
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
