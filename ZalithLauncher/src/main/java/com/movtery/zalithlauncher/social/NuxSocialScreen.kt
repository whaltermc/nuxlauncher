package com.movtery.zalithlauncher.social

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.auth.NuxAuthManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import com.movtery.zalithlauncher.setting.enums.isLauncherInDarkTheme
import com.movtery.zalithlauncher.ui.theme.backgroundColor
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class NuxSocialTheme(
    val isDark: Boolean,
    val background: Color,
    val cardSurface: Color,
    val cardHover: Color,
    val border: Color,
    val primary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val bubbleMineBg: Color = if (isDark) Color(0xFF1E3F2E) else Color(0xFFD6F0E0),
    val bubbleMineText: Color = if (isDark) Color(0xFFE8F5E9) else Color(0xFF0F3822),
    val bubbleMineBorder: Color = if (isDark) Color(0xFF2E6349) else Color(0xFFB5DEC5),
    val bubbleOtherBg: Color = if (isDark) Color(0xFF242C27) else Color(0xFFFFFFFF),
    val bubbleOtherText: Color = if (isDark) Color(0xFFF1F5F3) else Color(0xFF1E2922),
    val bubbleOtherBorder: Color = if (isDark) Color(0xFF334037) else Color(0xFFE2E8E4)
)

val LocalSocialTheme = compositionLocalOf {
    NuxSocialTheme(
        isDark = true,
        background = Color(0xFF141916),
        cardSurface = Color(0xFF1D2621),
        cardHover = Color(0xFF25322B),
        border = Color(0xFF2E3D34),
        primary = Color(0xFF2E7D5B),
        textPrimary = Color.White,
        textSecondary = Color.LightGray,
        textMuted = Color.Gray,
        bubbleMineBg = Color(0xFF1E3F2E),
        bubbleMineText = Color(0xFFE8F5E9),
        bubbleMineBorder = Color(0xFF2E6349),
        bubbleOtherBg = Color(0xFF242C27),
        bubbleOtherText = Color(0xFFF1F5F3),
        bubbleOtherBorder = Color(0xFF334037)
    )
}

@Composable
fun rememberSocialTheme(): NuxSocialTheme {
    val isDark = isLauncherInDarkTheme()
    val bg = backgroundColor()
    return if (isDark) {
        NuxSocialTheme(
            isDark = true,
            background = bg,
            cardSurface = MaterialTheme.colorScheme.surfaceContainerHigh,
            cardHover = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            primary = Color(0xFF2E7D5B),
            textPrimary = MaterialTheme.colorScheme.onSurface,
            textSecondary = MaterialTheme.colorScheme.onSurfaceVariant,
            textMuted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            bubbleMineBg = Color(0xFF1E3F2E),
            bubbleMineText = Color(0xFFE8F5E9),
            bubbleMineBorder = Color(0xFF2E6349),
            bubbleOtherBg = MaterialTheme.colorScheme.surfaceContainerHigh,
            bubbleOtherText = MaterialTheme.colorScheme.onSurface,
            bubbleOtherBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    } else {
        NuxSocialTheme(
            isDark = false,
            background = bg,
            cardSurface = MaterialTheme.colorScheme.surface,
            cardHover = MaterialTheme.colorScheme.surfaceVariant,
            border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            primary = Color(0xFF1E6E4B),
            textPrimary = MaterialTheme.colorScheme.onSurface,
            textSecondary = MaterialTheme.colorScheme.onSurfaceVariant,
            textMuted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            bubbleMineBg = Color(0xFFD6F0E0),
            bubbleMineText = Color(0xFF0F3822),
            bubbleMineBorder = Color(0xFFB5DEC5),
            bubbleOtherBg = Color(0xFFFFFFFF),
            bubbleOtherText = MaterialTheme.colorScheme.onSurface,
            bubbleOtherBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    }
}

private val DarkBackground: Color
    @Composable get() = LocalSocialTheme.current.background

private val DarkCardSurface: Color
    @Composable get() = LocalSocialTheme.current.cardSurface

private val DarkCardHover: Color
    @Composable get() = LocalSocialTheme.current.cardHover

private val BorderColor: Color
    @Composable get() = LocalSocialTheme.current.border

private val EmeraldPrimary: Color
    @Composable get() = LocalSocialTheme.current.primary

private val TextPrimaryColor: Color
    @Composable get() = LocalSocialTheme.current.textPrimary

private val TextSecondaryColor: Color
    @Composable get() = LocalSocialTheme.current.textSecondary

private val TextMutedColor: Color
    @Composable get() = LocalSocialTheme.current.textMuted

private val BubbleMineBg: Color
    @Composable get() = LocalSocialTheme.current.bubbleMineBg

private val BubbleMineText: Color
    @Composable get() = LocalSocialTheme.current.bubbleMineText

private val BubbleMineBorder: Color
    @Composable get() = LocalSocialTheme.current.bubbleMineBorder

private val BubbleOtherBg: Color
    @Composable get() = LocalSocialTheme.current.bubbleOtherBg

private val BubbleOtherText: Color
    @Composable get() = LocalSocialTheme.current.bubbleOtherText

private val BubbleOtherBorder: Color
    @Composable get() = LocalSocialTheme.current.bubbleOtherBorder

@Composable
fun NuxVerifiedBadge(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 14.dp
) {
    Image(
        painter = painterResource(R.drawable.ic_verified),
        contentDescription = "Verified",
        modifier = modifier.size(size)
    )
}

enum class SocialTab {
    FRIENDS,
    VOICE_ROOMS,
    REQUESTS
}

@Composable
fun NuxSocialScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val socialTheme = rememberSocialTheme()

    LaunchedEffect(Unit) {
        NuxVoiceManager.init(context)
        NuxSocialManager.startSync()
    }

    var currentTab by remember { mutableStateOf(SocialTab.FRIENDS) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    val friends by NuxSocialManager.friends.collectAsState()
    val activeVoiceRoom by NuxSocialManager.activeVoiceRoom.collectAsState()

    val pendingRequestsCount = friends.count { it.isPendingReceived }
    val totalUnreadCount = friends.sumOf { it.unreadCount }

    CompositionLocalProvider(LocalSocialTheme provides socialTheme) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TOP HEADER BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(DarkCardSurface)
                        .border(width = 1.dp, color = BorderColor)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TAB SWITCHER
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabButton(
                            text = "Teman & Chat",
                            selected = currentTab == SocialTab.FRIENDS,
                            badgeCount = totalUnreadCount,
                            iconRes = R.drawable.ic_chat_info,
                            onClick = { currentTab = SocialTab.FRIENDS }
                        )

                        TabButton(
                            text = if (activeVoiceRoom != null) "Voice (Aktif)" else "Voice Rooms",
                            selected = currentTab == SocialTab.VOICE_ROOMS,
                            isLive = activeVoiceRoom != null,
                            iconRes = R.drawable.ic_mic,
                            onClick = { currentTab = SocialTab.VOICE_ROOMS }
                        )

                        TabButton(
                            text = "Permintaan",
                            selected = currentTab == SocialTab.REQUESTS,
                            badgeCount = pendingRequestsCount,
                            iconRes = R.drawable.ic_person_outlined,
                            onClick = { currentTab = SocialTab.REQUESTS }
                        )
                    }

                    // TOMBOL CARI & TAMBAH TEMAN
                    Button(
                        onClick = { showAddFriendDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Cari Teman", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // CONTENT BODY
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (currentTab) {
                        SocialTab.FRIENDS -> FriendsAndChatView()
                        SocialTab.VOICE_ROOMS -> VoiceRoomsView()
                        SocialTab.REQUESTS -> FriendRequestsView()
                    }
                }
            }

            // DIALOG CARI TEMAN
            if (showAddFriendDialog) {
                AddFriendDialog(onDismiss = { showAddFriendDialog = false })
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    iconRes: Int,
    badgeCount: Int = 0,
    isLive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (selected) EmeraldPrimary.copy(alpha = 0.25f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (selected) EmeraldPrimary else TextSecondaryColor,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = text,
                color = if (selected) (if (LocalSocialTheme.current.isDark) Color.White else EmeraldPrimary) else TextSecondaryColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE53935), shape = CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isLive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF00E676), shape = CircleShape)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. TEMAN & DIRECT CHAT VIEW
// -------------------------------------------------------------
@Composable
private fun FriendsAndChatView() {
    val friends by NuxSocialManager.friends.collectAsState()
    val activeChatFriend by NuxSocialManager.activeChatFriend.collectAsState()

    val acceptedFriends = remember(friends) {
        friends.filter { it.isAccepted }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // SIDEBAR DAFTAR TEMAN
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(DarkCardSurface)
                .border(width = 1.dp, color = BorderColor)
        ) {
            Text(
                text = "Teman (${acceptedFriends.size})",
                color = TextSecondaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            if (acceptedFriends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada teman terhubung.\nTekan 'Cari Teman' untuk menambah teman!",
                        color = TextMutedColor,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(acceptedFriends, key = { it.uid }) { friend ->
                        FriendItemRow(
                            friend = friend,
                            isSelected = activeChatFriend?.uid == friend.uid,
                            onClick = { NuxSocialManager.openChat(friend) }
                        )
                    }
                }
            }
        }

        // PANEL RUANG OBROLAN (CHAT)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(DarkBackground)
        ) {
            if (activeChatFriend == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chat_info),
                        contentDescription = null,
                        tint = TextMutedColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Pilih teman di sebelah kiri untuk mulai mengobrol",
                        color = TextMutedColor,
                        fontSize = 13.sp
                    )
                }
            } else {
                ChatConversationPanel(friend = activeChatFriend!!)
            }
        }
    }
}

private fun formatLastOnline(ts: Long?): String {
    if (ts == null || ts <= 0L) return "Offline"
    val now = System.currentTimeMillis()
    val diff = now - ts
    val h24 = 24 * 60 * 60 * 1000L
    return try {
        if (diff < h24) {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            "Terakhir dilihat ${sdf.format(java.util.Date(ts))}"
        } else if (diff < h24 * 2) {
            "Terakhir dilihat kemarin"
        } else {
            val sdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
            "Terakhir dilihat ${sdf.format(java.util.Date(ts))}"
        }
    } catch (_: Exception) {
        "Offline"
    }
}

@Composable
private fun FriendItemRow(
    friend: NuxFriend,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) EmeraldPrimary.copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(1.dp, EmeraldPrimary) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Online / In-Game Badge
            Box(modifier = Modifier.size(38.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.15f))
                        .border(1.dp, if (isSelected) EmeraldPrimary else BorderColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (friend.photoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = friend.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_person_outlined),
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (friend.isOnline || friend.isInGame) {
                    val dotColor = if (friend.isInGame) Color(0xFFFFB300) else Color(0xFF00E676)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .background(dotColor, CircleShape)
                            .border(1.5.dp, DarkCardSurface, CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = friend.username,
                        color = TextPrimaryColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (friend.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        NuxVerifiedBadge(size = 13.dp)
                    }
                }

                Spacer(Modifier.height(1.dp))

                Text(
                    text = when {
                        friend.isTyping -> "sedang mengetik..."
                        friend.isInGame -> "🎮 In-Game"
                        friend.isOnline -> "🟢 Online"
                        else -> formatLastOnline(friend.lastOnline)
                    },
                    color = when {
                        friend.isTyping -> EmeraldPrimary
                        friend.isInGame -> Color(0xFFFFB300)
                        friend.isOnline -> Color(0xFF00E676)
                        else -> TextMutedColor
                    },
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (friend.isTyping || friend.isOnline || friend.isInGame) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            if (friend.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE53935), shape = CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (friend.unreadCount > 99) "99+" else friend.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatConversationPanel(friend: NuxFriend) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val messages by NuxSocialManager.chatMessages.collectAsState()
    val isFriendTyping by NuxSocialManager.isFriendTyping.collectAsState()

    val validMessages = remember(messages) {
        messages.filter { it.text.isNotBlank() || !it.imageUrl.isNullOrEmpty() || it.replyTo != null }
    }

    var inputText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<NuxChatReply?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                selectedImageBytes = bytes
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(validMessages.size, isFriendTyping) {
        if (validMessages.isNotEmpty()) {
            listState.animateScrollToItem(validMessages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // CHAT HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(DarkCardSurface)
                .border(1.dp, BorderColor)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (friend.photoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = friend.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_person_outlined),
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (friend.isOnline || friend.isInGame) {
                    val dotColor = if (friend.isInGame) Color(0xFFFFB300) else Color(0xFF00E676)
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .align(Alignment.BottomEnd)
                            .background(dotColor, CircleShape)
                            .border(1.5.dp, DarkCardSurface, CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = friend.username,
                        color = TextPrimaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (friend.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        NuxVerifiedBadge(size = 14.dp)
                    }
                }
                Text(
                    text = when {
                        isFriendTyping -> "sedang mengetik..."
                        friend.isInGame -> "🎮 IN-GAME"
                        friend.isOnline -> "🟢 ONLINE"
                        else -> "⚪ ${formatLastOnline(friend.lastOnline).uppercase()}"
                    },
                    color = when {
                        isFriendTyping -> EmeraldPrimary
                        friend.isInGame -> Color(0xFFFFB300)
                        friend.isOnline -> Color(0xFF00E676)
                        else -> TextMutedColor
                    },
                    fontSize = 10.sp,
                    fontWeight = if (isFriendTyping || friend.isOnline || friend.isInGame) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // Hapus Pertemanan
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        NuxSocialManager.removeFriend(friend.uid)
                        NuxSocialManager.closeChat()
                        Toast.makeText(context, "Pertemanan dihapus", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_outlined),
                    contentDescription = "Hapus Teman",
                    tint = TextSecondaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(4.dp))

            // Tutup Chat
            IconButton(
                onClick = { NuxSocialManager.closeChat() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Tutup Chat",
                    tint = TextSecondaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // MESSAGES STREAM
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(validMessages, key = { it.id }) { msg ->
                val isMine = msg.senderId == NuxAuthManager.currentUser?.uid
                ChatMessageBubble(
                    message = msg,
                    isMine = isMine,
                    onImageClick = { url -> previewImageUrl = url },
                    onReplyClick = {
                        replyingTo = NuxChatReply(
                            id = msg.id,
                            senderId = msg.senderId,
                            senderName = if (isMine) "Kamu" else friend.username,
                            text = msg.text.ifEmpty { "[Gambar]" }
                        )
                    }
                )
            }
        }

        // REPLY PREVIEW BANNER
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardSurface)
                    .border(1.dp, BorderColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(30.dp)
                        .background(EmeraldPrimary, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Membalas ${replyingTo!!.senderName}:",
                        color = EmeraldPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = replyingTo!!.text,
                        color = TextSecondaryColor,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Batal",
                        tint = TextSecondaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // SELECTED IMAGE PREVIEW BANNER
        if (selectedImageUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardSurface)
                    .border(1.dp, BorderColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Lampiran gambar siap dikirim",
                    color = TextSecondaryColor,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        selectedImageUri = null
                        selectedImageBytes = null
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Batal Lampiran",
                        tint = TextMutedColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // INPUT BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCardSurface)
                .border(BorderStroke(1.dp, BorderColor))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol Lampirkan Gambar
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isSending) { photoPickerLauncher.launch("image/*") },
                color = if (selectedImageUri != null) EmeraldPrimary.copy(alpha = 0.15f) else DarkCardHover,
                shape = CircleShape,
                border = BorderStroke(1.dp, if (selectedImageUri != null) EmeraldPrimary else BorderColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image_filled),
                        contentDescription = "Kirim Gambar",
                        tint = if (selectedImageUri != null) EmeraldPrimary else TextSecondaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Pill Input Container Bebas Clipping
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkCardHover)
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        NuxSocialManager.setMyTyping(it.isNotBlank())
                    },
                    textStyle = TextStyle(
                        color = TextPrimaryColor,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text("Ketik pesan...", fontSize = 12.5.sp, color = TextMutedColor)
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(Modifier.width(8.dp))

            // Tombol Kirim: Circular Emerald Button
            val canSend = (inputText.isNotBlank() || selectedImageBytes != null) && !isSending
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(enabled = canSend) {
                        val textToSend = inputText
                        val imageToSend = selectedImageBytes
                        val currentReply = replyingTo

                        inputText = ""
                        selectedImageBytes = null
                        selectedImageUri = null
                        replyingTo = null
                        isSending = true

                        coroutineScope.launch {
                            val res = NuxSocialManager.sendChatMessage(
                                text = textToSend,
                                imageBytes = imageToSend,
                                replyTo = currentReply
                            )
                            isSending = false
                            if (res.isFailure) {
                                Toast.makeText(context, "Gagal mengirim: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                color = if (canSend) EmeraldPrimary else DarkCardHover,
                shape = CircleShape,
                border = BorderStroke(1.dp, if (canSend) EmeraldPrimary else BorderColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = if (canSend) Color.White else EmeraldPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_send),
                            contentDescription = "Kirim",
                            tint = if (canSend) Color.White else TextMutedColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }

    // LIGHTBOX IMAGE PREVIEW MODAL
    if (previewImageUrl != null) {
        Dialog(onDismissRequest = { previewImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { previewImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = previewImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: NuxChatMessage,
    isMine: Boolean,
    onImageClick: (String) -> Unit,
    onReplyClick: () -> Unit
) {
    if (message.text.isBlank() && message.imageUrl.isNullOrEmpty() && message.replyTo == null) return

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(message.timestamp) {
        if (message.timestamp > 0) timeFormat.format(Date(message.timestamp)) else ""
    }

    val bubbleBg = if (isMine) BubbleMineBg else BubbleOtherBg
    val bubbleTextColor = if (isMine) BubbleMineText else BubbleOtherText
    val bubbleBorder = if (isMine) BubbleMineBorder else BubbleOtherBorder

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleBg,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isMine) 12.dp else 3.dp,
                bottomEnd = if (isMine) 3.dp else 12.dp
            ),
            border = BorderStroke(1.dp, bubbleBorder),
            modifier = Modifier
                .widthIn(min = 36.dp, max = 260.dp)
                .clickable(onClick = onReplyClick)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                // KUTIPAN REPLY JIKA ADA
                if (message.replyTo != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isMine) Color.Black.copy(alpha = 0.08f)
                                else EmeraldPrimary.copy(alpha = 0.08f)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(EmeraldPrimary)
                        )
                        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)) {
                            Text(
                                text = message.replyTo.senderName,
                                color = EmeraldPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = message.replyTo.text,
                                color = bubbleTextColor.copy(alpha = 0.75f),
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // GAMBAR LAMPIRAN JIKA ADA
                if (!message.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(message.imageUrl) },
                        contentScale = ContentScale.Crop
                    )
                    if (message.text.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                    }
                }

                // TEKS PESAN & WAKTU INLINE
                if (message.text.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = message.text,
                            color = bubbleTextColor,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (timeStr.isNotEmpty()) {
                            Text(
                                text = timeStr,
                                color = bubbleTextColor.copy(alpha = 0.55f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.padding(bottom = 0.5.dp)
                            )
                        }
                    }
                } else if (timeStr.isNotEmpty()) {
                    Text(
                        text = timeStr,
                        color = bubbleTextColor.copy(alpha = 0.55f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. VOICE ROOMS VIEW
// -------------------------------------------------------------
@Composable
private fun VoiceRoomsView() {
    val activeRoom by NuxSocialManager.activeVoiceRoom.collectAsState()
    val currentRoom = activeRoom
    if (currentRoom != null) {
        ActiveVoiceRoomUI(room = currentRoom)
    } else {
        VoiceRoomsLobbyUI()
    }
}

@Composable
private fun VoiceRoomsLobbyUI() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rooms by NuxSocialManager.voiceRooms.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var passwordPromptRoom by remember { mutableStateOf<NuxVoiceRoom?>(null) }
    var enteredPassword by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Izin mikrofon dibutuhkan agar suara Anda dapat didengar di Voice Room", Toast.LENGTH_SHORT).show()
        }
    }

    val checkAndRequestMicPermission = {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Voice Rooms Lobby",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimaryColor
                )
                Text(
                    text = "Obrolan suara real-time low-latency bersama teman dan tim",
                    fontSize = 11.sp,
                    color = TextSecondaryColor
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Buat Room Baru", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(14.dp))

        if (rooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .background(DarkCardSurface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_home_filled),
                        contentDescription = null,
                        tint = TextSecondaryColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Belum ada Voice Room yang aktif.", color = TextPrimaryColor, fontSize = 12.sp)
                    Text("Jadilah yang pertama membuat room untuk mabar!", color = TextSecondaryColor, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rooms, key = { it.id }) { room ->
                    VoiceRoomCard(
                        room = room,
                        onJoin = {
                            checkAndRequestMicPermission()
                            if (room.isLocked) {
                                passwordPromptRoom = room
                                enteredPassword = ""
                            } else {
                                coroutineScope.launch {
                                    val user = NuxAuthManager.currentUser ?: return@launch
                                    val res = NuxSocialManager.joinVoiceRoom(room)
                                    if (res.isSuccess) {
                                        NuxVoiceManager.joinVoiceRoom(room.id, user)
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal bergabung", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // MODAL BUAT ROOM
    if (showCreateDialog) {
        CreateVoiceRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreated = { name, pass, max ->
                checkAndRequestMicPermission()
                coroutineScope.launch {
                    val user = NuxAuthManager.currentUser ?: return@launch
                    val res = NuxSocialManager.createVoiceRoom(name, pass, max)
                    if (res.isSuccess) {
                        NuxVoiceManager.joinVoiceRoom(res.getOrThrow().id, user)
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Gagal membuat room", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // MODAL PASSWORD PROMPT
    if (passwordPromptRoom != null) {
        val currentPromptRoom = passwordPromptRoom!!
        AlertDialog(
            onDismissRequest = { passwordPromptRoom = null },
            containerColor = DarkCardSurface,
            title = { Text("Masukkan Password Room", color = TextPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = enteredPassword,
                    onValueChange = { enteredPassword = it },
                    placeholder = { Text("Password...", fontSize = 12.sp, color = TextSecondaryColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimaryColor,
                        unfocusedTextColor = TextPrimaryColor
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = enteredPassword
                        passwordPromptRoom = null
                        checkAndRequestMicPermission()
                        coroutineScope.launch {
                            val user = NuxAuthManager.currentUser ?: return@launch
                            val res = NuxSocialManager.joinVoiceRoom(currentPromptRoom, pass)
                            if (res.isSuccess) {
                                NuxVoiceManager.joinVoiceRoom(currentPromptRoom.id, user)
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Password salah!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Masuk", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { passwordPromptRoom = null }) {
                    Text("Batal", color = TextSecondaryColor)
                }
            }
        )
    }
}

@Composable
private fun VoiceRoomCard(
    room: NuxVoiceRoom,
    onJoin: () -> Unit
) {
    Surface(
        color = DarkCardSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = room.name,
                        color = TextPrimaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (room.isLocked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = "Terkunci",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_group_filled),
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${room.participantCount} / ${room.maxUsers} Peserta",
                        color = if (room.isFull) Color(0xFFE53935) else TextSecondaryColor,
                        fontSize = 11.sp,
                        fontWeight = if (room.isFull) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Button(
                onClick = onJoin,
                enabled = !room.isFull,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    disabledContainerColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (room.isFull) "Penuh" else "Gabung Room",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ActiveVoiceRoomUI(room: NuxVoiceRoom) {
    val coroutineScope = rememberCoroutineScope()
    val isMuted by NuxVoiceManager.isMuted.collectAsState()
    val isConnecting by NuxVoiceManager.isConnecting.collectAsState()
    val activeSpeakers by NuxVoiceManager.activeSpeakers.collectAsState()
    val voiceMessages by NuxSocialManager.voiceMessages.collectAsState()

    var chatInputText by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    LaunchedEffect(voiceMessages.size) {
        if (voiceMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(voiceMessages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP CONTROL BAR (KOMPAK 1 BARIS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCardSurface)
                .border(1.dp, BorderColor)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = room.name,
                    color = TextPrimaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF00E676), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .background(DarkCardHover, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "👥 ${room.participantCount} / ${room.maxUsers} Peserta",
                        color = TextSecondaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .background(EmeraldPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE SFU",
                        color = EmeraldPrimary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // MUTE / UNMUTE PILL BUTTON
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { NuxVoiceManager.toggleMute() },
                    color = if (isMuted) Color(0xFFEF4444).copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isMuted) Color(0xFFEF4444) else EmeraldPrimary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isMuted) Color(0xFFEF4444) else EmeraldPrimary
                        )
                        Text(
                            text = if (isMuted) "Muted" else "Mic Aktif",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMuted) Color(0xFFEF4444) else EmeraldPrimary
                        )
                    }
                }

                // KELUAR ROOM PILL BUTTON
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            coroutineScope.launch {
                                NuxVoiceManager.leaveVoiceRoom()
                                NuxSocialManager.leaveVoiceRoom()
                            }
                        },
                    color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFD32F2F))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_call_end),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFD32F2F)
                        )
                        Text(
                            text = "Keluar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }

        // BODY: PANGGUNG PESERTA DI KIRI + CHAT ROOM DI KANAN
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PANGGUNG PESERTA (STAGE GRID)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .background(DarkCardSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Panggung Peserta",
                        color = TextPrimaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .background(DarkCardHover, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${room.participantCount} / ${room.maxUsers} Terisi",
                            color = TextSecondaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (isConnecting) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                } else {
                    val participants = room.participants.values.toList()
                    val totalSlots = room.maxUsers
                    val emptySlotsCount = (totalSlots - participants.size).coerceAtLeast(0)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(participants, key = { it.uid }) { p ->
                            val isSpeaking = activeSpeakers.contains(p.uid)
                            val isLocal = p.uid == NuxAuthManager.currentUser?.uid
                            VoiceParticipantCard(
                                participant = p,
                                isSpeaking = isSpeaking,
                                isLocal = isLocal
                            )
                        }
                        items(emptySlotsCount) {
                            EmptySeatCard()
                        }
                    }
                }
            }

            // CHAT TEKS VOICE ROOM
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .background(DarkCardSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Obrolan Room",
                        color = TextPrimaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(EmeraldPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text("LIVE", color = EmeraldPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (voiceMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada pesan di room ini.\nSapa temanmu!",
                            color = TextMutedColor,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(voiceMessages, key = { it.id }) { msg ->
                            val isMyVoiceMsg = msg.senderId == NuxAuthManager.currentUser?.uid
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMyVoiceMsg) Alignment.End else Alignment.Start
                            ) {
                                Surface(
                                    color = if (isMyVoiceMsg) BubbleMineBg else DarkCardHover,
                                    shape = RoundedCornerShape(
                                        topStart = 8.dp,
                                        topEnd = 8.dp,
                                        bottomStart = if (isMyVoiceMsg) 8.dp else 2.dp,
                                        bottomEnd = if (isMyVoiceMsg) 2.dp else 8.dp
                                    ),
                                    border = BorderStroke(0.5.dp, if (isMyVoiceMsg) BubbleMineBorder else BorderColor.copy(alpha = 0.4f)),
                                    modifier = Modifier.widthIn(min = 32.dp, max = 220.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)) {
                                        if (!isMyVoiceMsg) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = msg.senderName,
                                                    color = EmeraldPrimary,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (msg.isVerified) {
                                                    Spacer(Modifier.width(3.dp))
                                                    NuxVerifiedBadge(size = 10.dp)
                                                }
                                            }
                                            Spacer(Modifier.height(1.dp))
                                        }
                                        Text(
                                            text = msg.text,
                                            color = if (isMyVoiceMsg) BubbleMineText else TextPrimaryColor,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Input Box Bebas Clipping
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(DarkCardHover)
                        .border(1.dp, BorderColor, RoundedCornerShape(22.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = chatInputText,
                        onValueChange = { chatInputText = it },
                        textStyle = TextStyle(color = TextPrimaryColor, fontSize = 11.5.sp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (chatInputText.isEmpty()) {
                                Text("Ketik pesan room...", fontSize = 11.5.sp, color = TextMutedColor)
                            }
                            innerTextField()
                        }
                    )
                    val canSend = chatInputText.isNotBlank()
                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canSend) {
                                val text = chatInputText.trim()
                                chatInputText = ""
                                coroutineScope.launch {
                                    NuxSocialManager.sendVoiceRoomMessage(text)
                                }
                            },
                        color = if (canSend) EmeraldPrimary else Color.Transparent,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_send),
                                contentDescription = "Kirim",
                                tint = if (canSend) Color.White else TextMutedColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceParticipantCard(
    participant: NuxParticipant,
    isSpeaking: Boolean,
    isLocal: Boolean
) {
    val displayName = if (participant.username.length > 5) "${participant.username.substring(0, 5)}..." else participant.username

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = if (isSpeaking) EmeraldPrimary.copy(alpha = 0.12f) else DarkCardHover,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSpeaking) 2.dp else 1.dp,
            color = if (isSpeaking) Color(0xFF00E676) else BorderColor.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar with speaking indicator ring
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.15f))
                    .border(
                        width = if (isSpeaking) 2.5.dp else 1.5.dp,
                        color = if (isSpeaking) Color(0xFF00E676) else BorderColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (participant.photoURL.isNotEmpty()) {
                    AsyncImage(
                        model = participant.photoURL,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_person_outlined),
                        contentDescription = null,
                        tint = if (isSpeaking) Color(0xFF00E676) else EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Name & Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayName,
                    color = TextPrimaryColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (participant.isVerified) {
                    Spacer(Modifier.width(3.dp))
                    NuxVerifiedBadge(size = 12.dp)
                }
            }

            Spacer(Modifier.height(4.dp))

            // Status Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isLocal) {
                    Box(
                        modifier = Modifier
                            .background(EmeraldPrimary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Kamu",
                            color = EmeraldPrimary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = if (isSpeaking) Color(0xFF00E676).copy(alpha = 0.18f) else DarkCardSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, if (isSpeaking) Color(0xFF00E676) else BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (isSpeaking) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                        }
                        Text(
                            text = if (isSpeaking) "Bicara" else "Mendengar",
                            color = if (isSpeaking) Color(0xFF00E676) else TextMutedColor,
                            fontSize = 8.5.sp,
                            fontWeight = if (isSpeaking) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySeatCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DarkCardHover.copy(alpha = 0.4f))
                    .border(1.dp, BorderColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_person_outlined),
                    contentDescription = null,
                    tint = TextMutedColor.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Kursi Kosong",
                color = TextMutedColor.copy(alpha = 0.6f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Tersedia",
                color = TextMutedColor.copy(alpha = 0.4f),
                fontSize = 8.5.sp
            )
        }
    }
}

// -------------------------------------------------------------
// 3. PERMINTAAN PERTEMANAN VIEW
// -------------------------------------------------------------
@Composable
private fun FriendRequestsView() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val friends by NuxSocialManager.friends.collectAsState()

    val receivedRequests = remember(friends) { friends.filter { it.isPendingReceived } }
    val sentRequests = remember(friends) { friends.filter { it.isPendingSent } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Permintaan Masuk (${receivedRequests.size})",
            color = TextPrimaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (receivedRequests.isEmpty()) {
            Text("Tidak ada permintaan pertemanan masuk.", color = TextSecondaryColor, fontSize = 11.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(receivedRequests, key = { it.uid }) { r ->
                    Surface(
                        color = DarkCardSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(r.username, color = TextPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                if (r.isVerified) {
                                    Spacer(Modifier.width(4.dp))
                                    NuxVerifiedBadge(size = 13.dp)
                                }
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        NuxSocialManager.acceptFriendRequest(r.uid)
                                        Toast.makeText(context, "Menerima pertemanan", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Terima", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        NuxSocialManager.removeFriend(r.uid)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Tolak", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Permintaan Terkirim (${sentRequests.size})",
            color = TextPrimaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (sentRequests.isEmpty()) {
            Text("Tidak ada permintaan pertemanan tertunda.", color = TextSecondaryColor, fontSize = 11.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sentRequests, key = { it.uid }) { r ->
                    Surface(
                        color = DarkCardSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(r.username, color = TextPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                if (r.isVerified) {
                                    Spacer(Modifier.width(4.dp))
                                    NuxVerifiedBadge(size = 13.dp)
                                }
                            }
                            Text("Menunggu persetujuan...", color = TextSecondaryColor, fontSize = 10.sp)
                            Spacer(Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        NuxSocialManager.removeFriend(r.uid)
                                    }
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Batal", fontSize = 10.sp, color = TextSecondaryColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. DIALOG CARI & TAMBAH TEMAN
// -------------------------------------------------------------
@Composable
private fun AddFriendDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    val isSearching by NuxSocialManager.isSearching.collectAsState()
    val results by NuxSocialManager.searchResults.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardSurface,
        title = {
            Text("Cari & Tambah Teman", color = TextPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            coroutineScope.launch {
                                NuxSocialManager.searchUser(it)
                            }
                        },
                        placeholder = { Text("Ketik username...", fontSize = 12.sp, color = TextSecondaryColor) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimaryColor,
                            unfocusedTextColor = TextPrimaryColor
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 2.dp)
                    }
                } else if (results.isEmpty() && query.isNotBlank()) {
                    Text("User tidak ditemukan.", color = TextSecondaryColor, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(results, key = { it.uid }) { u ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkCardHover, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (u.photoURL.isNotEmpty()) {
                                        AsyncImage(model = u.photoURL, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(painter = painterResource(R.drawable.ic_person_outlined), contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(u.username, color = TextPrimaryColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    if (u.isVerified) {
                                        Spacer(Modifier.width(4.dp))
                                        NuxVerifiedBadge(size = 13.dp)
                                    }
                                }
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = NuxSocialManager.sendFriendRequest(u.uid)
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Permintaan terkirim ke ${u.username}", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            } else {
                                                Toast.makeText(context, "Gagal mengirim permintaan", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Tambah", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = TextSecondaryColor)
            }
        }
    )
}

// -------------------------------------------------------------
// 5. DIALOG BUAT VOICE ROOM
// -------------------------------------------------------------
@Composable
private fun CreateVoiceRoomDialog(
    onDismiss: () -> Unit,
    onCreated: (name: String, password: String, maxUsers: Int) -> Unit
) {
    var roomName by remember { mutableStateOf("Mabar Bareng") }
    var password by remember { mutableStateOf("") }
    var maxUsers by remember { mutableStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardSurface,
        title = { Text("Buat Voice Room Baru", color = TextPrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Nama Room *", color = TextSecondaryColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimaryColor,
                        unfocusedTextColor = TextPrimaryColor
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (Opsional)", color = TextSecondaryColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimaryColor,
                        unfocusedTextColor = TextPrimaryColor
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kapasitas Peserta:", color = TextSecondaryColor, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (maxUsers > 2) maxUsers-- }) {
                            Text("-", color = EmeraldPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(maxUsers.toString(), color = TextPrimaryColor, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { if (maxUsers < 20) maxUsers++ }) {
                            Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (roomName.isNotBlank()) {
                        onCreated(roomName.trim(), password.trim(), maxUsers)
                        onDismiss()
                    }
                },
                enabled = roomName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Buat & Langsung Masuk", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondaryColor)
            }
        }
    )
}

private var savedVoiceBarOffsetX = 0f
private var savedVoiceBarOffsetY = 0f

/**
 * Floating Voice Bar mini widget saat pengguna sedang terhubung ke voice room
 * namun sedang membuka layar lain (misalnya Minecraft launcher, pengaturan, dll.)
 * Dapat di-drag dan dipindah-pindahkan secara bebas di layar.
 */
@Composable
fun FloatingVoiceBar(
    room: NuxVoiceRoom,
    onMaximize: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMuted by NuxVoiceManager.isMuted.collectAsState()
    val config = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    var offsetX by remember { mutableFloatStateOf(savedVoiceBarOffsetX) }
    var offsetY by remember { mutableFloatStateOf(savedVoiceBarOffsetY) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDragDistance = 0f
                    var isDragging = false

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (!isDragging && totalDragDistance < 10f) {
                                onMaximize()
                            }
                            break
                        }

                        val dragAmount = change.position - change.previousPosition
                        val moveDist = kotlin.math.hypot(dragAmount.x, dragAmount.y)
                        totalDragDistance += moveDist

                        if (!isDragging && totalDragDistance > 8f) {
                            isDragging = true
                            android.util.Log.d("NuxVoiceDrag", "DRAG STARTED! totalDrag=$totalDragDistance")
                        }

                        if (isDragging) {
                            change.consume()

                            val minX = -(screenWidthPx - with(density) { 80.dp.toPx() })
                            val maxX = with(density) { 8.dp.toPx() }
                            val minY = -(screenHeightPx - with(density) { 60.dp.toPx() })
                            val maxY = with(density) { 8.dp.toPx() }

                            offsetX = (offsetX + dragAmount.x).coerceIn(minX, maxX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                            savedVoiceBarOffsetX = offsetX
                            savedVoiceBarOffsetY = offsetY
                        }
                    } while (true)
                }
            }
            .padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            color = DarkCardSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldPrimary),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Drag grip indicator (indikator visual seret)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(14.dp)
                            .background(TextMutedColor.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(14.dp)
                            .background(TextMutedColor.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
                    )
                }

                // Green live dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF00E676), CircleShape)
                )

                // Room details
                Column {
                    Text(
                        text = room.name,
                        color = TextPrimaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 110.dp)
                    )
                    Text(
                        text = "${room.participantCount} 👤 • Voice Aktif",
                        color = EmeraldPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // MUTE TOGGLE
                IconButton(
                    onClick = { NuxVoiceManager.toggleMute() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic),
                        contentDescription = null,
                        tint = if (isMuted) Color(0xFFE53935) else EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // HANG UP
                IconButton(
                    onClick = onLeave,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_call_end),
                        contentDescription = "Keluar Room",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

