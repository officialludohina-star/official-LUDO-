package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlin.math.roundToInt

// Asal HTML (#yallaHome) ka poora lobby screen — topbar (avatar/coins/gems/shop/settings),
// league-rank locked cards, mode grid (2&4 Players / Team), bottom nav (Events/Battle/Chat/Social).
// Sab image assets wahi postimg.cc links hain jo index.html mein the, taake look bilkul match kare.
private const val HOME_BG_IMG = "file:///android_asset/img/file-0000000097f0820b81bc2995a995177d.png"
private const val NAV_GLOW_IMG = "file:///android_asset/img/selected-light-green-glow-transparent.png"
private const val PILL_BG_IMG = "file:///android_asset/img/bg-add-coin.png"

// ---- Home-page icon layout panel: har icon ki position (Left/Right, Up/Down)
// aur Size — bilkul ludo token panel jaisa (dropdown se icon chuno, phir slider
// + -/+ buttons + number box). Session ke doraan yaad rehta hai; app restart
// hone par default par wapis aa jata hai. ----
private data class IconAdjust(
    val x: Int = 0,      // Left/Right, px
    val y: Int = 0,      // Up/Down, px
    val size: Int = 100  // %, 100 = default
)

private object HomeLayoutStore {
    // key -> current adjustment. Compose state map hai isliye jahan bhi read hoga
    // wahan value change hote hi khud recompose ho jayega.
    val adjust = mutableStateMapOf<String, IconAdjust>()

    val keys = listOf(
        "avatar" to "Avatar", "coinPill" to "Coins", "gemPill" to "Gems",
        "shop" to "Shop icon", "settings" to "Settings icon",
        "modeCard1" to "2&4 Players card", "modeCard2" to "Team card",
        "navEvents" to "Events (nav)", "navBattle" to "Battle (nav)",
        "navChat" to "Chat (nav)", "navSocial" to "Social (nav)"
    )

    fun get(key: String): IconAdjust = adjust[key] ?: IconAdjust()

    fun setX(key: String, px: Int) {
        val a = get(key)
        adjust[key] = a.copy(x = px.coerceIn(-200, 200))
    }

    fun setY(key: String, px: Int) {
        val a = get(key)
        adjust[key] = a.copy(y = px.coerceIn(-200, 200))
    }

    fun setSize(key: String, percent: Int) {
        val a = get(key)
        adjust[key] = a.copy(size = percent.coerceIn(10, 200))
    }

    fun reset(key: String) { adjust.remove(key) }
    fun resetAll() { adjust.clear() }
}

// Modifier extension — kisi bhi icon/card par lagao, HomeLayoutStore ke mutabiq
// offset + size apply ho jata hai (edar-udar move + chota-bara).
@Composable
private fun Modifier.homeLayout(key: String): Modifier {
    val a = HomeLayoutStore.get(key)
    return this
        .offset(x = a.x.dp, y = a.y.dp)
        .graphicsLayer(scaleX = a.size / 100f, scaleY = a.size / 100f)
}


@Composable
fun YallaHomeScreen(navController: NavController) {
    var showLayoutPanel by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF051a0f))) {
        AsyncImage(
            model = HOME_BG_IMG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                TopBar(onOpenLayoutPanel = { showLayoutPanel = true })
                Spacer(Modifier.height(6.dp))
                StatRow()
                Spacer(Modifier.height(28.dp))
                ModeGrid(navController)
            }
            BottomNav()
        }

        if (showLayoutPanel) {
            IconLayoutPanel(onClose = { showLayoutPanel = false })
        } else {
            // Alag, saaf-saaf dikhne wala button — settings gear ke ilawa, taake
            // panel dhoondna mushkil na ho. Hamesha top-right corner mein rehta hai.
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 4.dp, end = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFCC33))
                    .clickable { showLayoutPanel = true }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\u270F\uFE0F", fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Text("Icons edit karein", color = Color(0xFF3a2500), fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    }
}

// Icons ko edar-udar (position) aur chota-bara (size +/-) karne ka panel. Har row
// ek icon ke liye 4-directional move buttons + size − / + buttons + reset dikhata hai.
// Panel ka card khud bhi upar-neechay drag ho sakta hai (panelDragY yaad rakhta hai)
// — taake yeh screen ki puri jagah na ghere aur user isay jahan chahe wahan sarka sake.
@Composable
private fun IconLayoutPanel(onClose: () -> Unit) {
    var panelDragY by remember { mutableStateOf(0f) }
    var selectedKey by remember { mutableStateOf(HomeLayoutStore.keys.first().first) }
    var dropdownOpen by remember { mutableStateOf(false) }
    val selectedLabel = HomeLayoutStore.keys.first { it.first == selectedKey }.second
    val current = HomeLayoutStore.get(selectedKey)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClose)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 30.dp)
                .offset { IntOffset(0, panelDragY.roundToInt()) }
                .fillMaxWidth(0.92f)
                .background(Color(0xFF12122a), RoundedCornerShape(14.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {} // andar tap se panel band na ho
        ) {
            // Purple header — bilkul Ludo token-panel jaisa "pakar kar kahin bhi le jao" bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF7c3aed), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panelDragY += dragAmount.y
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "\uD83D\uDC49 ICON PANEL \u2014 pakar kar kahin bhi le jao",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFff5a5a))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u2715", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text("Icon Position / Size", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))

                // Dropdown — kaunsa icon adjust karna hai
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .clickable { dropdownOpen = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedLabel, color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("\u25BC", color = Color.Black, fontSize = 11.sp)
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = dropdownOpen,
                        onDismissRequest = { dropdownOpen = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        HomeLayoutStore.keys.forEach { (key, label) ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(label, fontSize = 13.sp) },
                                onClick = { selectedKey = key; dropdownOpen = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                SliderRow(
                    label = "Left / Right",
                    value = current.x,
                    range = -200..200,
                    onValue = { HomeLayoutStore.setX(selectedKey, it) }
                )
                Spacer(Modifier.height(10.dp))
                SliderRow(
                    label = "Up / Down",
                    value = current.y,
                    range = -200..200,
                    onValue = { HomeLayoutStore.setY(selectedKey, it) }
                )
                Spacer(Modifier.height(10.dp))
                SliderRow(
                    label = "Size",
                    value = current.size,
                    range = 10..200,
                    onValue = { HomeLayoutStore.setSize(selectedKey, it) }
                )

                Spacer(Modifier.height(14.dp))

                YellowButton("Reset selected icon") { HomeLayoutStore.reset(selectedKey) }
                Spacer(Modifier.height(8.dp))
                YellowButton("Reset all icons") { HomeLayoutStore.resetAll() }

                Spacer(Modifier.height(8.dp))
                Text(
                    "+ / \u2212 se 1-1 step adjust karo. Slider bhi available hai. X/Y \u00B1200px, Size 10\u2013200%.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

// Ek row: label, [-] button, slider, [+] button, number box — bilkul ludo token
// panel ("Left/Right", "Up/Down", "Size") jaisa.
@Composable
private fun SliderRow(label: String, value: Int, range: IntRange, onValue: (Int) -> Unit) {
    Column {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            YellowSquareBtn("\u2212") { onValue((value - 1).coerceIn(range)) }
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.Slider(
                value = value.toFloat(),
                onValueChange = { onValue(it.roundToInt().coerceIn(range)) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                modifier = Modifier.weight(1f).height(24.dp),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Color(0xFF3a8bff),
                    activeTrackColor = Color(0xFF3a8bff),
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                )
            )
            Spacer(Modifier.width(8.dp))
            YellowSquareBtn("+") { onValue((value + 1).coerceIn(range)) }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(value.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun YellowSquareBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFFCC33))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFF3a2500), fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun YellowButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFCC33))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFF3a2500), fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun TopBar(onOpenLayoutPanel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(46.dp).homeLayout("avatar")) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF22c55e), Color(0xFFa349ff)))),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "file:///android_asset/img/user-icon.png",
                    contentDescription = "avatar",
                    modifier = Modifier.size(46.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFff5a1a))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text("42", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.width(6.dp))

        Box(modifier = Modifier.homeLayout("coinPill")) {
            Pill(
                iconUrl = "file:///android_asset/img/coin-icon.webp",
                value = "10K",
                valueColor = Color(0xFFffd700),
                addBg = Color(0xFF22c55e),
                addContent = { Text("+", color = Color(0xFF3a2500), fontWeight = FontWeight.Black, fontSize = 13.sp) }
            )
        }

        Spacer(Modifier.width(6.dp))

        Box(modifier = Modifier.homeLayout("gemPill")) {
            Pill(
                iconUrl = "file:///android_asset/img/diamond.png",
                value = "10K",
                valueColor = Color(0xFF00e5ff),
                addBg = Color.Transparent,
                addContent = {
                    AsyncImage(
                        model = "file:///android_asset/img/plus.png",
                        contentDescription = "add",
                        modifier = Modifier.size(22.dp)
                    )
                }
            )
        }

        Spacer(Modifier.weight(1f))

        AsyncImage(
            model = "file:///android_asset/img/shop-cart.png",
            contentDescription = "shop",
            modifier = Modifier.size(26.dp).homeLayout("shop")
        )
        Spacer(Modifier.width(14.dp))
        AsyncImage(
            model = "file:///android_asset/img/setting.png",
            contentDescription = "settings",
            modifier = Modifier
                .size(26.dp)
                .homeLayout("settings")
                .clickable(onClick = onOpenLayoutPanel)
        )
    }
}

@Composable
private fun Pill(
    iconUrl: String,
    value: String,
    valueColor: Color,
    addBg: Color,
    addContent: @Composable () -> Unit
) {
    Box {
        AsyncImage(
            model = PILL_BG_IMG,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(20.dp))
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        AsyncImage(model = iconUrl, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(6.dp))
        Text(value, color = valueColor, fontWeight = FontWeight.Black, fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(addBg),
            contentAlignment = Alignment.Center
        ) { addContent() }
        }
    }
}

@Composable
private fun StatRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3a2a0a), Color(0xFF8a5a10))))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("\uD83D\uDD12", fontSize = 24.sp)
                Text("Unlock at\nLevel 4", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0a2a3a), Color(0xFF106a8a))))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Rank\nUnlock at Level 4", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                Text("\uD83C\uDF0E", fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun ModeGrid(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModeCard(
            imageUrl = "file:///android_asset/img/2-4-players.png",
            label = "2&4 Players",
            gradient = Brush.linearGradient(listOf(Color(0xFFf7b733), Color(0xFFc46b1a))),
            modifier = Modifier.weight(1f).homeLayout("modeCard1")
        ) { navController.navigate("ludo_mode_select") }

        ModeCard(
            imageUrl = "file:///android_asset/img/team.png",
            label = "Team",
            gradient = Brush.linearGradient(listOf(Color(0xFF8a7bd8), Color(0xFF5a4bc4))),
            modifier = Modifier.weight(1f).homeLayout("modeCard2")
        ) { /* TODO: Team mode abhi implement nahi hui */ }
    }
}

@Composable
private fun ModeCard(
    imageUrl: String,
    label: String,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = label,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(344f / 220f),
            contentScale = ContentScale.Crop
        )
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun BottomNav() {
    var active by remember { mutableStateOf("Events") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavItem("file:///android_asset/img/Events.png", "Events", active == "Events", Modifier.homeLayout("navEvents")) { active = "Events" }
        NavItem("file:///android_asset/img/Battle.png", "Battle", active == "Battle", Modifier.homeLayout("navBattle")) { active = "Battle" }
        NavItem("file:///android_asset/img/Chat.png", "Chat", active == "Chat", Modifier.homeLayout("navChat")) { active = "Chat" }
        NavItem("file:///android_asset/img/Social.png", "Social", active == "Social", Modifier.homeLayout("navSocial")) { active = "Social" }
    }
}

@Composable
private fun NavItem(iconUrl: String, label: String, active: Boolean = false, layoutModifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = layoutModifier.clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) {
            if (active) {
                AsyncImage(
                    model = NAV_GLOW_IMG,
                    contentDescription = null,
                    modifier = Modifier.size(width = 78.dp, height = 57.dp)
                )
            }
            AsyncImage(model = iconUrl, contentDescription = label, modifier = Modifier.size(if (active) 56.dp else 46.dp))
        }
        Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}
