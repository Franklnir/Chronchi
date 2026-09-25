package com.irsyadlabs.espbridge.data.xiaozhi

data class XiaozhiUser(
    val id: Int = 0,
    val username: String = "",
    val role: String = "user",
    val uiTheme: String = "neo",
    val createdAt: String = "",
    val googleId: String? = null,
    val googleEmail: String? = null,
    val registeredWithGoogle: Boolean = false,
    val deviceMac: String? = null
)

data class XiaozhiAuthResult(
    val success: Boolean,
    val user: XiaozhiUser? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val message: String = ""
)

data class XiaozhiMcpStatus(
    val success: Boolean = false,
    val connected: Boolean = false,
    val statusText: String = "",
    val tokenPreview: String = "",
    val tokenSaved: Boolean = false,
    val message: String = ""
)

data class XiaozhiDashboardStats(
    val total: Int = 0,
    val tugas: Int = 0,
    val pengumuman: Int = 0,
    val materi: Int = 0
)

data class QuotaLimit(
    val reached: Boolean = false,
    val label: String = "0 / 0",
    val limit: Int = 0,
    val count: Int = 0,
    val unlimited: Boolean = false
)

data class XiaozhiQuota(
    val materials: QuotaLimit = QuotaLimit(),
    val wordsPerMaterial: QuotaLimit = QuotaLimit(unlimited = true),
    val liveApis: QuotaLimit = QuotaLimit(),
    val relayRooms: QuotaLimit = QuotaLimit(),
    val alerts: List<String> = emptyList()
)

data class XiaozhiCategory(
    val id: Int,
    val name: String
)

data class XiaozhiMaterial(
    val id: Int,
    val title: String,
    val category: String,
    val content: String,
    val keywords: String = "",
    val sourceType: String = "manual",
    val apiUrl: String = "",
    val apiLabel: String = "",
    val contentSizeLabel: String = "",
    val createdAt: String = ""
)

data class XiaozhiDashboardData(
    val user: XiaozhiUser = XiaozhiUser(),
    val stats: XiaozhiDashboardStats = XiaozhiDashboardStats(),
    val quota: XiaozhiQuota = XiaozhiQuota(),
    val mcpStatus: XiaozhiMcpStatus = XiaozhiMcpStatus(),
    val categories: List<XiaozhiCategory> = emptyList(),
    val materials: List<XiaozhiMaterial> = emptyList()
)

// Chat History Models
data class XiaozhiChatMessage(
    val id: Int? = null,
    val role: String = "",
    val userMessage: String? = null,
    val xiaozhiAnswer: String? = null,
    val toolName: String? = null,
    val responsePayload: String? = null,
    val createdAt: String = "",
    val source: String? = null
)

data class ChatDateGroup(
    val date: String,
    val count: Int
)

data class XiaozhiChatHistoryData(
    val total: Int = 0,
    val items: List<XiaozhiChatMessage> = emptyList(),
    val dateList: List<ChatDateGroup> = emptyList(),
    val activeDate: String = "",
    val query: String = "",
    val mcpStatus: XiaozhiMcpStatus = XiaozhiMcpStatus()
)

// AI Persona Models
data class PersonalityTrait(
    val primaryTrait: String = "Ambivert Seimbang",
    val description: String = "",
    val introvertPercent: Int = 50,
    val extrovertPercent: Int = 50
)

data class PersonaMetricItem(
    val icon: String = "🎯",
    val name: String = "",
    val percent: Int = 50,
    val level: String = "Sedang",
    val badge: String = "neutral"
)

data class PersonaAnalysis(
    val totalChatsAnalyzed: Int = 0,
    val confidenceLevel: String = "Data Awal",
    val personality: PersonalityTrait = PersonalityTrait(),
    val hobbies: List<PersonaMetricItem> = emptyList(),
    val challenges: List<PersonaMetricItem> = emptyList(),
    val activities: List<PersonaMetricItem> = emptyList(),
    val preferences: List<PersonaMetricItem> = emptyList()
)

data class XiaozhiToolItem(
    val name: String,
    val title: String,
    val category: String,
    val categoryLabel: String,
    val icon: String,
    val description: String,
    val enabled: Boolean = true
)

data class XiaozhiProfileData(
    val user: XiaozhiUser = XiaozhiUser(),
    val personaAnalysis: PersonaAnalysis = PersonaAnalysis(),
    val toolsCatalog: List<XiaozhiToolItem> = emptyList(),
    val totalTools: Int = 39,
    val mcpStatus: XiaozhiMcpStatus = XiaozhiMcpStatus()
)

data class SmartHomeRelay(
    val channel: Int,
    val name: String,
    val state: Boolean,
    val isVirtual: Boolean = false
)


// Admin Models
data class XiaozhiAdminMcpStatus(
    val hasToken: Boolean = false,
    val connected: Boolean = false,
    val message: String = ""
)

data class XiaozhiAdminUserItem(
    val id: Int = 0,
    val username: String = "",
    val role: String = "user",
    val createdAt: String = "",
    val deviceMac: String = "",
    val deviceName: String = "",
    val isPlaying: Boolean = false,
    val currentTrack: String = "",
    val mcpStatus: XiaozhiAdminMcpStatus = XiaozhiAdminMcpStatus()
)
