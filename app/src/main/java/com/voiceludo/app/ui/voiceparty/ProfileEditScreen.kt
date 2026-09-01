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
import com.voiceludo.app.net.BackendClient
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
private const val UPLOAD_PHOTO_ICON = "https://i.postimg.cc/PrbZKVL7/IMG-20260831-WA0011.jpg"
private const val FEMALE_ICON = "https://i.postimg.cc/SKgfdTvw/female.webp"
private const val MALE_ICON = "https://i.postimg.cc/65CVg5SN/male.webp"

// Gender icon ab image se load hoti hai (female.webp / male.webp) — pehle
// Unicode ♀/♂ symbol draw hota tha, ab asal icon dikhta hai.
@Composable
private fun GenderIcon(gender: String, size: androidx.compose.ui.unit.Dp) {
    val isFemale = gender == "female"
    AsyncImage(
        model = if (isFemale) FEMALE_ICON else MALE_ICON,
        contentDescription = gender,
        modifier = Modifier.size(size).clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}

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
    var avatarUploading by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }

    // Naam/avatar badalte hi bekend (real account, game-partners ko dikhne wala)
    // ko bhi sync kar dete hain — kabhi bhi local file path avatar ke taur par
    // nahi bhejte, sirf pehle se hosted http(s) URL (upload hone ke baad).
    fun pushProfileToBackend(currentProfile: UserProfile) {
        val avatarForBackend = currentProfile.avatarUri.takeIf { it.startsWith("http") } ?: ""
        BackendClient.updateProfile(currentProfile.name, avatarForBackend)
    }

    // Device ki photo picker (Android system picker — koi runtime permission nahi
    // chahiye). Photo pehle internal storage mein copy karte hain (turant preview
    // ke liye), phir usay bekend ke POST /avatar par upload karte hain — response
    // mein mila hosted URL hi asal avatarUri ban jata hai (README ke mutabiq
    // base64/local path kabhi bhi bekend/doosre players ko nahi bheja jata).
    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    avatarError = "Photo padhi nahi ja saki"
                    return@rememberLauncherForActivityResult
                }
                // Turant local preview (upload complete hone tak)
                val outFile = File(context.filesDir, "profile_avatar.jpg")
                outFile.writeBytes(bytes)
                profile = profile.copy(avatarUri = outFile.absolutePath)
                avatarError = null
                avatarUploading = true
                BackendClient.uploadAvatar(bytes, mime) { url, err ->
                    avatarUploading = false
                    if (url != null) {
                        ProfileStore.saveAvatar(context, url)
                        profile = profile.copy(avatarUri = url)
                        pushProfileToBackend(profile)
                    } else {
                        // Upload fail — local preview hi reh jati hai is device par,
                        // lekin doosre players ko nahi dikhegi jab tak dobara try na ho.
                        avatarError = err ?: "Photo upload nahi ho saki, dobara koshish karein"
                    }
                }
            } catch (_: Exception) {
                avatarError = "Photo save nahi ho saki"
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
                // ---- Avatar + alag se upload button (avatar ke right side wali
                // jagah, red-box wali position) — tap karte hi gallery khul jati hai. ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
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

                    Spacer(Modifier.width(16.dp))

                    // Upload button — avatar ke bagal mein, tap karte hi gallery/photo-picker khul jati hai.
                    Box(
                        modifier = Modifier
                            .size(width = 84.dp, height = 60.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(2.dp, ProfileGreenAccent, RoundedCornerShape(10.dp))
                            .clickable {
                                pickPhoto.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = UPLOAD_PHOTO_ICON,
                            contentDescription = "upload photo",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                if (avatarUploading) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Photo upload ho rahi hai...",
                        color = ProfileGreenDark,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                avatarError?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        err,
                        color = Color(0xFFB33123),
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
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
                        GenderIcon(profile.gender, 24.dp)
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
                pushProfileToBackend(profile)
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
                            GenderIcon(value, 20.dp)
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
