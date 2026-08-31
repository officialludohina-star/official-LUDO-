package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

// Asal HTML (#yallaHome) ka poora lobby screen — topbar (avatar/coins/gems/shop/settings),
// league-rank locked cards, mode grid (2&4 Players / Team), bottom nav (Events/Battle/Chat/Social).
// Sab image assets wahi postimg.cc links hain jo index.html mein the, taake look bilkul match kare.
private const val HOME_BG_IMG = "file:///android_asset/img/file-0000000097f0820b81bc2995a995177d.png"
private const val NAV_GLOW_IMG = "file:///android_asset/img/selected-light-green-glow-transparent.png"
private const val PILL_BG_IMG = "file:///android_asset/img/bg-add-coin.png"

// ---- Home-page icon layout: har icon ki final position (Left/Right, Up/Down)
// aur Size — tuning panel se confirm ki gayi values yahan permanently bake kar
// di gayi hain (panel ab hata diya gaya hai, koi runtime editing nahi hoti). ----
private data class IconAdjust(
    val x: Int = 0,      // Left/Right, px
    val y: Int = 0,      // Up/Down, px
    val size: Int = 100  // %, 100 = default
)

private val HOME_LAYOUT: Map<String, IconAdjust> = mapOf(
    "avatar" to IconAdjust(x = -8, y = 5, size = 100),
    "coinPill" to IconAdjust(x = -2, y = 7, size = 100),
    "gemPill" to IconAdjust(x = -1, y = 7, size = 100),
    "shop" to IconAdjust(x = 3, y = 8, size = 159),
    "settings" to IconAdjust(x = 2, y = 5, size = 108),
    "modeCard1" to IconAdjust(x = -9, y = 196, size = 104),
    "modeCard2" to IconAdjust(x = 7, y = 195, size = 104),
    "navEvents" to IconAdjust(x = -23, y = 6, size = 93),
    "navBattle" to IconAdjust(x = -32, y = 13, size = 104),
    "navChat" to IconAdjust(x = -19, y = 8, size = 112),
    "navSocial" to IconAdjust(x = -1, y = 11, size = 106)
)

// Modifier extension — kisi bhi icon/card par lagao, HOME_LAYOUT ke mutabiq
// fixed offset + size apply ho jata hai.
private fun Modifier.homeLayout(key: String): Modifier {
    val a = HOME_LAYOUT[key] ?: IconAdjust()
    return this
        .offset(x = a.x.dp, y = a.y.dp)
        .graphicsLayer(scaleX = a.size / 100f, scaleY = a.size / 100f)
}


@Composable
fun YallaHomeScreen(navController: NavController) {
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
                TopBar(onAvatarClick = { navController.navigate("vp_profile_edit") })
                Spacer(Modifier.height(6.dp))
                StatRow()
                Spacer(Modifier.height(28.dp))
                ModeGrid(navController)
            }
            BottomNav()
        }
    }
}

@Composable
private fun TopBar(onAvatarClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Home ka avatar hamesha profile screen mein save ki hui photo dikhata hai
    // (HTML ke #homeAvatarImg jaisa) — agar kuch save nahi hui to default icon.
    val savedAvatar = ProfileStore.get(context).avatarUri
    val avatarModel: Any = when {
        savedAvatar.isEmpty() -> "file:///android_asset/img/user-icon.png"
        savedAvatar.startsWith("http") -> savedAvatar
        else -> java.io.File(savedAvatar)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(46.dp).homeLayout("avatar").clickable(onClick = onAvatarClick)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF22c55e), Color(0xFFa349ff)))),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = avatarModel,
                    contentDescription = "avatar",
                    modifier = Modifier.size(46.dp).clip(CircleShape),
                    contentScale = if (savedAvatar.isEmpty()) ContentScale.Fit else ContentScale.Crop
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
            // TODO: settings screen abhi implement nahi hui — tap yahan add karna
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
