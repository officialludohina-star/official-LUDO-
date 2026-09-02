package com.voiceludo.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Login/Signup bhejte hi jab tak server ka jawab (auth ya error) na aa jaye,
// yeh poori screen ke upar dikhta hai — is dauran neeche wali screen ke taps
// bhi consume ho jate hain (pointerInput block), taake user loading khatam
// hone se pehle dobara button na daba sake ya galti se agli screen par na
// pohanch jaye. Server response (success ya fail) aate hi yeh khud hat jata hai.
// ============================================================================
@Composable
fun LoadingOverlay(message: String = "Loading...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color(0xFF1c1c1c), RoundedCornerShape(14.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFF0a7a42))
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color.White, fontSize = 13.sp)
        }
    }
}
