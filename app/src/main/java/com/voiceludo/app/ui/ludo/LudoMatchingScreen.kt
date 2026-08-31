package com.voiceludo.app.ui.ludo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// Asal HTML ke #ludoMatchingScreen jaisa hi — bet confirm hone ke baad "opponents
// dhoonda ja raha hai" wala screen. match.gif ab upar center mein nahi, balke seedha
// har player slot (Aap + opponents) ke andar chalti hai. Har slot ka size aur
// position (x/y) neeche wale calibration panel se alag alag set ho sakta hai —
// values note karke baad mein defaults mein hardcode kar sakte ho.
private const val MATCH_GIF = "https://i.postimg.cc/wvc7cYNC/match.gif"

// Har player slot ke liye calibration: size (dp), offsetX (dp), offsetY (dp)
private data class SlotCalib(val size: Float, val offsetX: Float, val offsetY: Float)

private val DEFAULT_CALIB = SlotCalib(size = 84f, offsetX = 0f, offsetY = 0f)

@Composable
fun LudoMatchingScreen(
    navController: NavController,
    mode: String,
    players: Int,
    magic: Boolean,
    betIndex: Int
) {
    val bet = BET_OPTIONS.getOrElse(betIndex) { BET_OPTIONS.first() }
    val totalPool = bet * players

    LaunchedEffect(Unit) {
        delay(2500)
        navController.navigate("ludo_game/$mode/$players/$magic/$betIndex") {
            popUpTo("ludo_matching/$mode/$players/$magic/$betIndex") { inclusive = true }
        }
    }

    // 4 slots ke calib values — sirf pehle `players` hi use honge
    val calibs = remember { mutableStateListOf(DEFAULT_CALIB, DEFAULT_CALIB, DEFAULT_CALIB, DEFAULT_CALIB) }
    var panelOpen by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0a1a2a))) {
        AsyncImage(
            model = MODE_SCREEN_BG_IMG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Opponents dhoonde ja rahe hain...",
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
            )
            Spacer(Modifier.height(40.dp))

            // Entry coins + total pool
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .padding(top = 0.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                AsyncImage(model = COIN_ICON, contentDescription = null, modifier = Modifier.size(40.dp))
                Column {
                    Text("Entry Coins $bet", color = Color(0xFFffd93b), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("Total Pool $totalPool", color = Color(0xFFffd93b), fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(30.dp))

            // Players — gif ab yahan har slot ke andar chalti hai
            // 4 players ho to 2x2 grid (2 rows) taake koi icon screen se bahar/cut na ho,
            // warna ek hi row mein sab dikhte hain
            if (players == 4) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (i in 0 until 2) {
                            MatchPlayerSlot(
                                label = if (i == 0) "Aap" else "Player",
                                calib = calibs[i],
                                highlighted = panelOpen && selectedSlot == i,
                                onTap = { selectedSlot = i; panelOpen = true }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (i in 2 until 4) {
                            MatchPlayerSlot(
                                label = "Player",
                                calib = calibs[i],
                                highlighted = panelOpen && selectedSlot == i,
                                onTap = { selectedSlot = i; panelOpen = true }
                            )
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    for (i in 0 until players) {
                        MatchPlayerSlot(
                            label = if (i == 0) "Aap" else "Player",
                            calib = calibs[i],
                            highlighted = panelOpen && selectedSlot == i,
                            onTap = { selectedSlot = i; panelOpen = true }
                        )
                    }
                }
            }
        }

        // Close button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 12.dp)
                .size(width = 44.dp, height = 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFff7a2a), Color(0xFFc93a0a))))
                .clickable { navController.popBackStack() },
            contentAlignment = Alignment.Center
        ) { Text("\u2715", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }

        // Calibration panel toggle button — top-left "gear"
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 14.dp, start = 12.dp)
                .size(width = 44.dp, height = 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF143c64).copy(alpha = 0.85f))
                .clickable { panelOpen = !panelOpen },
            contentAlignment = Alignment.Center
        ) { Text("\u2699", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }

        // Calibration panel — neeche se
        if (panelOpen) {
            CalibrationPanel(
                players = players,
                selectedSlot = selectedSlot,
                onSelectSlot = { selectedSlot = it },
                calib = calibs[selectedSlot],
                onCalibChange = { calibs[selectedSlot] = it },
                onClose = { panelOpen = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun MatchPlayerSlot(
    label: String,
    calib: SlotCalib,
    highlighted: Boolean,
    onTap: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(110.dp), // fixed slot footprint taake offset se layout na hile
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset(x = calib.offsetX.dp, y = calib.offsetY.dp)
                    .size(calib.size.dp)
                    .clip(CircleShape)
                    .then(
                        if (highlighted)
                            Modifier.background(Color(0xFFffd93b).copy(alpha = 0.25f))
                        else Modifier
                    )
                    .clickable { onTap() },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = MATCH_GIF,
                    contentDescription = "matching",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF143c64).copy(alpha = 0.75f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CalibrationPanel(
    players: Int,
    selectedSlot: Int,
    onSelectSlot: (Int) -> Unit,
    calib: SlotCalib,
    onCalibChange: (SlotCalib) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF0d1f33).copy(alpha = 0.96f))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Gif Calibration", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2a3a52))
                    .clickable { onClose() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text("Band karo", color = Color.White, fontSize = 11.sp) }
        }

        Spacer(Modifier.height(10.dp))

        // Slot selector — P1..P4
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until players) {
                val isSel = i == selectedSlot
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0xFFffd93b) else Color(0xFF2a3a52))
                        .clickable { onSelectSlot(i) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "P${i + 1}",
                        color = if (isSel) Color(0xFF0d1f33) else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        CalibSlider(
            label = "Size",
            value = calib.size,
            range = 1f..200f,
            onChange = { onCalibChange(calib.copy(size = it)) }
        )
        CalibSlider(
            label = "Left / Right (X)",
            value = calib.offsetX,
            range = -50f..50f,
            onChange = { onCalibChange(calib.copy(offsetX = it)) }
        )
        CalibSlider(
            label = "Upar / Neeche (Y)",
            value = calib.offsetY,
            range = -50f..50f,
            onChange = { onCalibChange(calib.copy(offsetY = it)) }
        )

        Spacer(Modifier.height(6.dp))
        Text(
            "size=${calib.size.toInt()}  x=${calib.offsetX.toInt()}  y=${calib.offsetY.toInt()}",
            color = Color(0xFF9fb4cc), fontSize = 11.sp
        )
    }
}

@Composable
private fun CalibSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range
        )
    }
}
