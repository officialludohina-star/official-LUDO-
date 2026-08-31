package com.voiceludo.app.ui.voiceparty

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import java.io.File

// ============================================================================
// Asal HTML ke #profileEdit screen ka hoobahoo Compose version — Name, Flag,
// Gender, Bio (sab dashed rows, tap karne par ek modal khulta hai) aur upar
// Avatar + "Gallery" button (preset avatars ka grid, wahi yalla.games default
// pictures jo HTML mein thay) + device se apni photo choose karne ka option.
// Data ProfileStore (SharedPreferences) mein save hota hai, HTML ke
// window.currentUserData + Firestore setDoc jaisa hi, bas on-device.
// ============================================================================

private const val DEFAULT_AVATAR = "file:///android_asset/img/user-icon.png"

// HTML gallery grid ke wahi preset "Default Head Picture" avatars
private val PRESET_AVATARS = listOf(
    "https://file.yalla.games/DefaultHeadPicture/54b9edc6-f2fa-473b-bbdf-7cac5bdbcad1/49b69044-c8e8-4579-9a68-bb88b8c036d8.png",
    "https://file.yalla.games/DefaultHeadPicture/9d01bbe4-7fcf-4e6a-97b7-060d99a880e6/c819f6e8-27a3-4396-b0f8-7721fbcdeeeb.png",
    "https://file.yalla.games/DefaultHeadPicture/99b51e8e-ee24-4a1b-8e39-a5e8091b8891/c32ba635-d3e1-45e8-b1d0-26b1258e7e5f.png",
    "https://file.yalla.games/DefaultHeadPicture/83da3bf1-bd83-46c6-97b1-bb55f6d78295/e6f9e613-358b-4423-b5ab-86091f518ee4.png",
    "https://file.yalla.games/DefaultHeadPicture/b2f235f2-56fb-4ce6-b512-132fc73df510/01368b10-dbac-480c-aa4b-87443155f857.png",
    "https://file.yalla.games/DefaultHeadPicture/51d07a07-649a-4166-9d1b-ef9716d22037/fc3f4a15-a9df-4455-994e-958fb1b2aac2.png",
    "https://file.yalla.games/DefaultHeadPicture/f55e13e0-8628-4386-bf51-15f6b6826898/28c737cd-0060-40a1-8014-08f33824e15d.png",
    "https://file.yalla.games/DefaultHeadPicture/3b8a7503-af7b-47cc-9922-1d9e0cab1209/eae4363d-2f23-445a-9051-a898d275c073.png",
    "https://file.yalla.games/DefaultHeadPicture/681d0154-7e0c-4455-a5d3-d29591aeb1b1/26640f5a-282f-4726-9a7b-907569406824.png",
    "https://file.yalla.games/DefaultHeadPicture/effa19d8-307e-47ca-8c4c-c611a80a0485/ca627e98-daa4-4db0-8bc5-e48da87baa52.png",
    "https://file.yalla.games/DefaultHeadPicture/8ad4a909-12bd-4bd8-bb2e-ccd41e5a3192/9a2dd4a4-dcdd-403e-a576-1041c5976cf9.png",
    "https://file.yalla.games/DefaultHeadPicture/4fc36131-a52b-4d1d-8a56-7568f07ed5bc/0201bb59-6e01-4a35-94c9-b2233e233720.png"
)

// COUNTRY_LIST (CountryList.kt) already "🇵🇰 +92 Pakistan" jaisi 195 entries rakhti
// hai (phone signup dropdown ke liye) — flag modal ke liye usi se icon+name nikal
// kar A-Z sort kar dete hain, taake dobara wahi list ko hardcode na karna pare.
private val FLAG_LIST: List<Pair<String, String>> by lazy {
    val regex = Regex("^(\\S+)\\s+\\+\\d+\\s+(.+)$")
    COUNTRY_LIST.mapNotNull { entry ->
        regex.find(entry)?.let { m -> m.groupValues[1] to m.groupValues[2] }
    }.distinctBy { it.second }.sortedBy { it.second }
}

private val ProfileGreenBg = Color(0xFFf5fff7)
private val ProfileGreenBorder = Color(0xFF0a7a42)
private val ProfileGreenDark = Color(0xFF2a5a3a)
private val ProfileGreenAccent = Color(0xFF22c55e)

@Composable
fun ProfileEditScreen(navController: NavController) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(ProfileStore.get(context)) }

    var showNameDialog by remember { mutableStateOf(false) }
    var showBioDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showFlagDialog by remember { mutableStateOf(false) }
    var showGalleryDialog by remember { mutableStateOf(false) }

    // Device ki photo picker (Android system picker — koi runtime permission nahi
    // chahiye). Photo internal storage mein copy kar dete hain taake app restart
    // ke baad bhi dikhti rahe (HTML wale base64-persist wale idea jaisa hi).
    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val outFile = File(context.filesDir, "profile_avatar.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                profile = profile.copy(avatarUri = outFile.absolutePath)
                ProfileStore.saveAvatar(context, outFile.absolutePath)
                showGalleryDialog = false
            } catch (_: Exception) {
                // Photo copy fail hui to chup chaap ignore — avatar purana hi rahega
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF051a0f))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ProfileGreenBorder)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) { Text("\u2039", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black) }

                Text(
                    "Edit Profile",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )

                Spacer(Modifier.size(32.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ProfileGreenBg)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // ---- Avatar + Gallery button ----
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val avatarModel = when {
                        profile.avatarUri.isEmpty() -> DEFAULT_AVATAR
                        profile.avatarUri.startsWith("http") -> profile.avatarUri
                        else -> File(profile.avatarUri)
                    }
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF22c55e), Color(0xFFa349ff))))
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = avatarModel,
                            contentDescription = "avatar",
                            modifier = Modifier
                                .size(if (profile.avatarUri.isEmpty()) 60.dp else 96.dp)
                                .clip(CircleShape),
                            contentScale = if (profile.avatarUri.isEmpty()) ContentScale.Fit else ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ProfileGreenAccent)
                            .clickable { showGalleryDialog = true }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text("\uD83D\uDDBC\uFE0F Gallery", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ---- Name ----
                ProfileRow(
                    label = null,
                    value = profile.name,
                    valueColor = ProfileGreenDark,
                    onClick = { showNameDialog = true }
                )
                // ---- Flag ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFlagDialog = true }
                        .padding(vertical = 14.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.flagIcon, fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(profile.flagName, color = ProfileGreenDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text("\u25B6", color = ProfileGreenAccent)
                }
                RowDivider()

                // ---- Gender ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGenderDialog = true }
                        .padding(vertical = 14.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (profile.gender == "female") Color(0xFFec4899) else Color(0xFF3b82f6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (profile.gender == "female") "\u2640" else "\u2642", color = Color.White, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            profile.gender.replaceFirstChar { it.uppercase() },
                            color = ProfileGreenDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text("\u25B6", color = ProfileGreenAccent)
                }
                RowDivider()

                // ---- Bio ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBioDialog = true }
                        .padding(vertical = 14.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        profile.bio.ifEmpty { "Bio is left empty" },
                        color = if (profile.bio.isEmpty()) Color(0xFFa0c0a0) else ProfileGreenDark,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        fontStyle = if (profile.bio.isEmpty()) FontStyle.Italic else FontStyle.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    Text("\u25B6", color = ProfileGreenAccent)
                }
            }
        }
    }

    // ---- Name modal ----
    if (showNameDialog) {
        TextInputDialog(
            title = "Enter name (max 16 chars)",
            initial = profile.name,
            maxLen = 16,
            onDismiss = { showNameDialog = false },
            onConfirm = { t ->
                ProfileStore.saveName(context, t)
                profile = ProfileStore.get(context)
                showNameDialog = false
            }
        )
    }

    // ---- Bio modal ----
    if (showBioDialog) {
        TextInputDialog(
            title = "Bio (max 70 chars)",
            initial = profile.bio,
            maxLen = 70,
            singleLine = false,
            onDismiss = { showBioDialog = false },
            onConfirm = { t ->
                ProfileStore.saveBio(context, t)
                profile = ProfileStore.get(context)
                showBioDialog = false
            }
        )
    }

    // ---- Gender modal ----
    if (showGenderDialog) {
        AlertDialog(
            onDismissRequest = { showGenderDialog = false },
            title = { Text("Select gender", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    listOf("male" to "Male", "female" to "Female").forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ProfileStore.saveGender(context, value)
                                    profile = ProfileStore.get(context)
                                    showGenderDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = profile.gender == value, onClick = {
                                ProfileStore.saveGender(context, value)
                                profile = ProfileStore.get(context)
                                showGenderDialog = false
                            })
                            Spacer(Modifier.width(6.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGenderDialog = false }) { Text("Close") }
            }
        )
    }

    // ---- Flag modal (A-Z searchable country list) ----
    if (showFlagDialog) {
        FlagPickerDialog(
            onDismiss = { showFlagDialog = false },
            onSelect = { icon, name ->
                ProfileStore.saveFlag(context, name, icon)
                profile = ProfileStore.get(context)
                showFlagDialog = false
            }
        )
    }

    // ---- Gallery modal (preset avatars + pick from device) ----
    if (showGalleryDialog) {
        AlertDialog(
            onDismissRequest = { showGalleryDialog = false },
            title = { Text("Choose avatar", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ProfileGreenAccent)
                            .clickable {
                                pickPhoto.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\uD83D\uDCF7 Apni gallery se photo chunein", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Ya default avatar chunein:", fontSize = 12.sp, color = ProfileGreenDark, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.heightIn(max = 260.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PRESET_AVATARS) { url ->
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFFb5dfb8), CircleShape)
                                    .clickable {
                                        profile = profile.copy(avatarUri = url)
                                        ProfileStore.saveAvatar(context, url)
                                        showGalleryDialog = false
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGalleryDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ProfileRow(label: String?, value: String, valueColor: Color, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("\u25B6", color = ProfileGreenAccent)
        }
        RowDivider()
    }
}

@Composable
private fun RowDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color(0xFFd0e8d0))
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    initial: String,
    maxLen: Int,
    singleLine: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= maxLen) text = it },
                    singleLine = singleLine,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text("${text.length}/$maxLen", fontSize = 11.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FlagPickerDialog(onDismiss: () -> Unit, onSelect: (icon: String, name: String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) FLAG_LIST
        else FLAG_LIST.filter { it.second.contains(query, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Country A-Z", fontWeight = FontWeight.Black, fontSize = 14.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search country...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(filtered) { (icon, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(icon, name) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(icon, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(name, fontSize = 13.sp, color = ProfileGreenDark)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
