package com.irsyadlabs.espbridge.data.xiaozhi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class XiaozhiApiClient(
    private val baseUrl: String = "https://xiaozhiscig.biz.id",
    private val fallbackUrl: String = "http://163.61.58.235:8080"
) {

    suspend fun login(username: String, password: String): XiaozhiAuthResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        val (code, response) = executeRequest("/api/v1/auth/login", "POST", payload.toString())
        parseAuthResponse(code, response)
    }

    suspend fun register(username: String, password: String): XiaozhiAuthResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        val (code, response) = executeRequest("/api/v1/auth/register", "POST", payload.toString())
        parseAuthResponse(code, response)
    }

    suspend fun googleAuth(idToken: String, action: String = "login", accessToken: String? = null): XiaozhiAuthResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("id_token", idToken.trim())
            put("action", action)
        }
        val (code, response) = executeRequest("/api/v1/auth/google", "POST", payload.toString(), token = accessToken)
        parseAuthResponse(code, response)
    }

    suspend fun unlinkGoogle(accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/auth/google/unlink", "POST", "{}", token = accessToken)
        if (code in 200..299) {
            Result.success(true)
        } else {
            val errorMsg = parseErrorMessage(response) ?: "Gagal melepas tautan Google ($code)"
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun getMcpStatus(accessToken: String): XiaozhiMcpStatus = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/mcp/status", "GET", token = accessToken)
        if (code in 200..299 && response != null) {
            try {
                val json = JSONObject(response)
                XiaozhiMcpStatus(
                    success = json.optBoolean("success", true),
                    connected = json.optBoolean("connected", false),
                    statusText = json.optString("status_text", if (json.optBoolean("connected", false)) "Terhubung" else "Belum aktif"),
                    tokenPreview = json.optString("token_preview", ""),
                    tokenSaved = json.optBoolean("token_saved", false),
                    message = json.optString("message", "")
                )
            } catch (e: Exception) {
                XiaozhiMcpStatus(false, false, "Gagal memproses data MCP")
            }
        } else {
            XiaozhiMcpStatus(false, false, "Tidak dapat terhubung ke server ($code)")
        }
    }

    suspend fun saveMcpToken(accessToken: String, mcpToken: String): Result<String> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("mcp_token", mcpToken.trim())
        }
        val (code, response) = executeRequest("/api/v1/mcp/save", "POST", payload.toString(), token = accessToken)
        if (code in 200..299) {
            val msg = try { JSONObject(response ?: "{}").optString("message", "Berhasil disimpan.") } catch (_: Exception) { "Berhasil disimpan." }
            Result.success(msg)
        } else {
            val errorMsg = parseErrorMessage(response) ?: "Gagal menyimpan token MCP (HTTP $code)"
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun reconnectMcp(accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/mcp/reconnect", "POST", "{}", token = accessToken)
        if (code in 200..299) {
            Result.success(true)
        } else {
            val errorMsg = parseErrorMessage(response) ?: "Gagal menghubungkan ulang ($code)"
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun deleteMcpToken(accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/mcp/delete", "POST", "{}", token = accessToken)
        if (code in 200..299) {
            Result.success(true)
        } else {
            val errorMsg = parseErrorMessage(response) ?: "Gagal menghapus koneksi ($code)"
            Result.failure(Exception(errorMsg))
        }
    }

    // ── Dashboard Endpoint ──
    suspend fun getDashboard(accessToken: String): Result<XiaozhiDashboardData> = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/dashboard", "GET", token = accessToken)
        if (code in 200..299 && response != null) {
            try {
                val json = JSONObject(response)
                val data = json.optJSONObject("data") ?: JSONObject()

                // User
                val uObj = data.optJSONObject("user") ?: JSONObject()
                val user = XiaozhiUser(
                    id = uObj.optInt("id", 1),
                    username = uObj.optString("username", ""),
                    role = uObj.optString("role", "user"),
                    uiTheme = uObj.optString("ui_theme", "neo"),
                    createdAt = uObj.optString("created_at", ""),
                    googleId = uObj.optString("google_id").takeIf { it.isNotBlank() },
                    googleEmail = uObj.optString("google_email").takeIf { it.isNotBlank() },
                    registeredWithGoogle = uObj.optBoolean("registered_with_google", false),
                    deviceMac = uObj.optString("device_mac").takeIf { it.isNotBlank() }
                )

                // Stats
                val sObj = data.optJSONObject("stats") ?: JSONObject()
                val stats = XiaozhiDashboardStats(
                    total = sObj.optInt("total", 0),
                    tugas = sObj.optInt("tugas", 0),
                    pengumuman = sObj.optInt("pengumuman", 0),
                    materi = sObj.optInt("materi", 0)
                )

                // Quota
                val qObj = data.optJSONObject("quota") ?: JSONObject()
                val alertsArr = qObj.optJSONArray("alerts") ?: JSONArray()
                val alertList = mutableListOf<String>()
                for (i in 0 until alertsArr.length()) {
                    alertList.add(alertsArr.optString(i))
                }

                fun parseLimit(key: String): QuotaLimit {
                    val o = qObj.optJSONObject(key) ?: JSONObject()
                    return QuotaLimit(
                        reached = o.optBoolean("reached", false),
                        label = o.optString("label", "0 / 0"),
                        limit = o.optInt("limit", 0),
                        count = o.optInt("count", 0),
                        unlimited = o.optBoolean("unlimited", false)
                    )
                }

                val quota = XiaozhiQuota(
                    materials = parseLimit("materials"),
                    wordsPerMaterial = parseLimit("words_per_material"),
                    liveApis = parseLimit("live_apis"),
                    relayRooms = parseLimit("relay_rooms"),
                    alerts = alertList
                )

                // MCP Status
                val mcpObj = data.optJSONObject("mcp_status") ?: JSONObject()
                val mcpStatus = XiaozhiMcpStatus(
                    success = mcpObj.optBoolean("success", true),
                    connected = mcpObj.optBoolean("connected", false),
                    statusText = mcpObj.optString("status_text", ""),
                    tokenPreview = mcpObj.optString("token_preview", ""),
                    tokenSaved = mcpObj.optBoolean("token_saved", false)
                )

                // Categories
                val catsArr = data.optJSONArray("categories") ?: JSONArray()
                val categories = mutableListOf<XiaozhiCategory>()
                for (i in 0 until catsArr.length()) {
                    val c = catsArr.getJSONObject(i)
                    categories.add(XiaozhiCategory(c.optInt("id"), c.optString("name")))
                }

                // Materials
                val matArr = data.optJSONArray("materials") ?: JSONArray()
                val materials = mutableListOf<XiaozhiMaterial>()
                for (i in 0 until matArr.length()) {
                    val m = matArr.getJSONObject(i)
                    materials.add(
                        XiaozhiMaterial(
                            id = m.optInt("id"),
                            title = m.optString("title", ""),
                            category = m.optString("category", ""),
                            content = m.optString("content", ""),
                            keywords = m.optString("keywords", ""),
                            sourceType = m.optString("source_type", "manual"),
                            apiUrl = m.optString("api_url", ""),
                            apiLabel = m.optString("api_label", ""),
                            contentSizeLabel = m.optString("content_size_label", ""),
                            createdAt = m.optString("created_at", "")
                        )
                    )
                }

                Result.success(
                    XiaozhiDashboardData(
                        user = user,
                        stats = stats,
                        quota = quota,
                        mcpStatus = mcpStatus,
                        categories = categories,
                        materials = materials
                    )
                )
            } catch (e: Exception) {
                Result.failure(Exception("Gagal menguraikan data dashboard: ${e.localizedMessage}"))
            }
        } else {
            val errorMsg = parseErrorMessage(response) ?: "Gagal memuat dashboard ($code)"
            Result.failure(Exception(errorMsg))
        }
    }

    // ── Material CRUD ──
    suspend fun createMaterial(
        accessToken: String,
        title: String,
        category: String,
        content: String,
        keywords: String = "",
        apiUrl: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("title", title.trim())
            put("category", category.trim())
            put("content", content.trim())
            put("keywords", keywords.trim())
            put("api_url", apiUrl.trim())
        }
        val (code, response) = executeRequest("/api/v1/materials", "POST", payload.toString(), token = accessToken)
        if (code in 200..299) Result.success(true) else Result.failure(Exception(parseErrorMessage(response) ?: "Gagal ($code)"))
    }

    suspend fun updateMaterial(
        accessToken: String,
        materialId: Int,
        title: String,
        category: String,
        content: String,
        keywords: String = "",
        apiUrl: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("title", title.trim())
            put("category", category.trim())
            put("content", content.trim())
            put("keywords", keywords.trim())
            put("api_url", apiUrl.trim())
        }
        val (code, response) = executeRequest("/api/v1/materials/$materialId", "PUT", payload.toString(), token = accessToken)
        if (code in 200..299) Result.success(true) else Result.failure(Exception(parseErrorMessage(response) ?: "Gagal ($code)"))
    }

    suspend fun deleteMaterial(accessToken: String, materialId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/materials/$materialId", "DELETE", token = accessToken)
        if (code in 200..299) Result.success(true) else Result.failure(Exception(parseErrorMessage(response) ?: "Gagal ($code)"))
    }

    // ── Category CRUD ──
    suspend fun createCategory(accessToken: String, name: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("name", name.trim())
        }
        val (code, response) = executeRequest("/api/v1/categories", "POST", payload.toString(), token = accessToken)
        if (code in 200..299) Result.success(true) else Result.failure(Exception(parseErrorMessage(response) ?: "Gagal ($code)"))
    }

    suspend fun deleteCategory(accessToken: String, catId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/categories/$catId", "DELETE", token = accessToken)
        if (code in 200..299) Result.success(true) else Result.failure(Exception(parseErrorMessage(response) ?: "Gagal ($code)"))
    }

    // ── Chat History Endpoints ──
    suspend fun getChatHistory(
        accessToken: String,
        query: String = "",
        limit: Int = 100,
        date: String = ""
    ): Result<XiaozhiChatHistoryData> = withContext(Dispatchers.IO) {
        val qParam = if (query.isNotBlank()) "&q=${URLEncoder.encode(query, "UTF-8")}" else ""
        val dParam = if (date.isNotBlank()) "&date=${URLEncoder.encode(date, "UTF-8")}" else ""
        val path = "/api/v1/chat/history?limit=$limit$qParam$dParam"

        val (code, response) = executeRequest(path, "GET", token = accessToken)
        if (code in 200..299 && response != null) {
            try {
                val json = JSONObject(response)
                val total = json.optInt("total", 0)

                val itemsArr = json.optJSONArray("items") ?: JSONArray()
                val list = mutableListOf<XiaozhiChatMessage>()
                for (i in 0 until itemsArr.length()) {
                    val item = itemsArr.getJSONObject(i)
                    list.add(
                        XiaozhiChatMessage(
                            id = if (item.has("id")) item.optInt("id") else null,
                            role = item.optString("role", ""),
                            userMessage = item.optString("user_message").takeIf { it.isNotBlank() },
                            xiaozhiAnswer = item.optString("xiaozhi_answer").takeIf { it.isNotBlank() },
                            toolName = item.optString("tool_name").takeIf { it.isNotBlank() },
                            responsePayload = item.optString("response_payload").takeIf { it.isNotBlank() },
                            createdAt = item.optString("created_at", ""),
                            source = item.optString("source").takeIf { it.isNotBlank() }
                        )
                    )
                }

                val dateArr = json.optJSONArray("date_list") ?: JSONArray()
                val dateList = mutableListOf<ChatDateGroup>()
                for (i in 0 until dateArr.length()) {
                    val d = dateArr.getJSONObject(i)
                    dateList.add(ChatDateGroup(d.optString("date", ""), d.optInt("count", 0)))
                }

                val mcpObj = json.optJSONObject("mcp_status") ?: JSONObject()
                val mcpStatus = XiaozhiMcpStatus(
                    success = mcpObj.optBoolean("success", true),
                    connected = mcpObj.optBoolean("connected", false),
                    statusText = mcpObj.optString("status_text", ""),
                    tokenPreview = mcpObj.optString("token_preview", ""),
                    tokenSaved = mcpObj.optBoolean("token_saved", false)
                )

                Result.success(
                    XiaozhiChatHistoryData(
                        total = total,
                        items = list,
                        dateList = dateList,
                        activeDate = date,
                        query = query,
                        mcpStatus = mcpStatus
                    )
                )
            } catch (e: Exception) {
                Result.failure(Exception("Gagal menguraikan riwayat chat: ${e.localizedMessage}"))
            }
        } else {
            val errorMsg = parseErrorMessage(response) ?: "Gagal memuat riwayat ($code)"
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun clearChatHistory(accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/chat/history/clear", "POST", "{}", token = accessToken)
        if (code in 200..299) Result.success(true) else Result.failure(Exception(parseErrorMessage(response) ?: "Gagal ($code)"))
    }

    // ── Profile & AI Persona ──
    suspend fun getProfileData(accessToken: String): Result<XiaozhiProfileData> = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/profile/data", "GET", token = accessToken)
        if (code in 200..299 && response != null) {
            try {
                val json = JSONObject(response)
                val userObj = json.optJSONObject("user") ?: JSONObject()
                val user = XiaozhiUser(
                    id = userObj.optInt("id", 1),
                    username = userObj.optString("username", ""),
                    role = userObj.optString("role", "user"),
                    uiTheme = userObj.optString("ui_theme", "neo"),
                    createdAt = userObj.optString("created_at", ""),
                    googleId = userObj.optString("google_id").takeIf { it.isNotBlank() },
                    googleEmail = userObj.optString("google_email").takeIf { it.isNotBlank() },
                    registeredWithGoogle = userObj.optBoolean("registered_with_google", false),
                    deviceMac = userObj.optString("device_mac").takeIf { it.isNotBlank() }
                )
                val personaObj = json.optJSONObject("persona_analysis")
                val persona = parsePersona(personaObj)

                val toolsArr = json.optJSONArray("tools_catalog") ?: JSONArray()
                val tools = mutableListOf<XiaozhiToolItem>()
                for (i in 0 until toolsArr.length()) {
                    val t = toolsArr.getJSONObject(i)
                    tools.add(
                        XiaozhiToolItem(
                            name = t.optString("name", ""),
                            title = t.optString("title", ""),
                            category = t.optString("category", ""),
                            categoryLabel = t.optString("category_label", ""),
                            icon = t.optString("icon", "🔧"),
                            description = t.optString("description", ""),
                            enabled = t.optBoolean("enabled", true)
                        )
                    )
                }

                val mcpObj = json.optJSONObject("mcp_status") ?: JSONObject()
                val mcpStatus = XiaozhiMcpStatus(
                    success = mcpObj.optBoolean("success", true),
                    connected = mcpObj.optBoolean("connected", false),
                    statusText = mcpObj.optString("status_text", ""),
                    tokenPreview = mcpObj.optString("token_preview", ""),
                    tokenSaved = mcpObj.optBoolean("token_saved", false)
                )

                val totalTools = json.optInt("tools_count", tools.size.coerceAtLeast(39))
                Result.success(XiaozhiProfileData(user, persona, tools, totalTools, mcpStatus))
            } catch (e: Exception) {
                Result.failure(Exception("Gagal menguraikan profil: ${e.localizedMessage}"))
            }
        } else {
            val errorMsg = parseErrorMessage(response) ?: "Gagal memuat profil ($code)"
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun scanPersona(accessToken: String): Result<PersonaAnalysis> = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/profile/scan-persona", "POST", "{}", token = accessToken)
        if (code in 200..299 && response != null) {
            try {
                val json = JSONObject(response)
                val analysisObj = json.optJSONObject("analysis")
                val persona = parsePersona(analysisObj)
                Result.success(persona)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal menguraikan hasil scan: ${e.localizedMessage}"))
            }
        } else {
            Result.failure(Exception(parseErrorMessage(response) ?: "Gagal scan persona ($code)"))
        }
    }

    suspend fun getSmartHomeState(accessToken: String): Result<List<SmartHomeRelay>> = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/smarthome/state", "GET", token = accessToken)
        if (code in 200..299 && response != null) {
            val json = JSONObject(response)
            val data = json.optJSONObject("data") ?: JSONObject()
            val relaysArray = data.optJSONArray("relays") ?: JSONArray()
            val list = mutableListOf<SmartHomeRelay>()
            for (i in 0 until relaysArray.length()) {
                val r = relaysArray.getJSONObject(i)
                list.add(
                    SmartHomeRelay(
                        channel = r.optInt("channel", i + 1),
                        name = r.optString("name", "Relay ${i + 1}"),
                        state = r.optBoolean("state", false),
                        isVirtual = r.optBoolean("is_virtual", false)
                    )
                )
            }
            if (list.isEmpty()) {
                list.addAll(
                    listOf(
                        SmartHomeRelay(1, "Lampu Utama", false),
                        SmartHomeRelay(2, "Kipas Angin", false),
                        SmartHomeRelay(3, "Lampu Teras", false),
                        SmartHomeRelay(4, "Pompa Air", false)
                    )
                )
            }
            Result.success(list)
        } else {
            Result.failure(Exception("Gagal mengambil relay ($code)"))
        }
    }

    suspend fun setRelay(accessToken: String, channel: Int, state: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("channel", channel)
            put("state", state)
        }
        val (code, _) = executeRequest("/api/v1/smarthome/relays/channel", "POST", payload.toString(), token = accessToken)
        if (code in 200..299) Result.success(true) else Result.failure(Exception("Gagal mengubah relay ($code)"))
    }

    
    // ── Admin Endpoints ──
    suspend fun getAdminUsers(accessToken: String): Result<List<XiaozhiAdminUserItem>> = withContext(Dispatchers.IO) {
        val (code, response) = executeRequest("/api/v1/admin/users", "GET", token = accessToken)
        if (code in 200..299 && response != null) {
            try {
                val json = JSONObject(response)
                val usersArr = json.optJSONArray("users") ?: JSONArray()
                val list = mutableListOf<XiaozhiAdminUserItem>()
                for (i in 0 until usersArr.length()) {
                    val u = usersArr.getJSONObject(i)
                    val mcpObj = u.optJSONObject("mcp_status") ?: JSONObject()
                    val mcp = XiaozhiAdminMcpStatus(
                        hasToken = mcpObj.optBoolean("has_token", false),
                        connected = mcpObj.optBoolean("connected", false),
                        message = mcpObj.optString("message", "")
                    )
                    list.add(
                        XiaozhiAdminUserItem(
                            id = u.optInt("id", 0),
                            username = u.optString("username", ""),
                            role = u.optString("role", "user"),
                            createdAt = u.optString("created_at", ""),
                            deviceMac = u.optString("device_mac", ""),
                            deviceName = u.optString("device_name", ""),
                            isPlaying = u.optBoolean("is_playing", false),
                            currentTrack = u.optString("current_track", ""),
                            mcpStatus = mcp
                        )
                    )
                }
                Result.success(list)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal menguraikan daftar pengguna: ${e.localizedMessage}"))
            }
        } else {
            val errorMsg = parseErrorMessage(response) ?: "Gagal memuat daftar pengguna ($code)"
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun claimPresetCode(token: String?, code: String): Result<String> = withContext(Dispatchers.IO) {
        val body = JSONObject().put("code", code).toString()
        val (httpCode, response) = executeRequest("/api/v1/flasher/preset/claim", "POST", body, token)
        if (httpCode in 200..299 && response != null) {
            val json = JSONObject(response)
            val msg = json.optString("message", "Kode lisensi berhasil diklaim.")
            Result.success(msg)
        } else {
            val err = parseErrorMessage(response) ?: "Gagal mengklaim kode lisensi ($httpCode)"
            Result.failure(Exception(err))
        }
    }

    private fun parseAuthResponse(code: Int, response: String?): XiaozhiAuthResult {
        if (code in 200..299 && response != null) {
            return try {
                val json = JSONObject(response)
                val data = json.optJSONObject("data") ?: JSONObject()
                val userObj = data.optJSONObject("user")
                val user = if (userObj != null) {
                    XiaozhiUser(
                        id = userObj.optInt("id", 1),
                        username = userObj.optString("username", ""),
                        role = userObj.optString("role", "user"),
                        googleId = userObj.optString("google_id").takeIf { it.isNotBlank() },
                        googleEmail = userObj.optString("google_email").takeIf { it.isNotBlank() },
                        registeredWithGoogle = userObj.optBoolean("registered_with_google", false),
                        deviceMac = userObj.optString("device_mac").takeIf { it.isNotBlank() }
                    )
                } else null
                XiaozhiAuthResult(
                    success = true,
                    user = user,
                    accessToken = data.optString("access_token").takeIf { it.isNotBlank() },
                    refreshToken = data.optString("refresh_token").takeIf { it.isNotBlank() },
                    message = json.optString("message", "Berhasil.")
                )
            } catch (e: Exception) {
                XiaozhiAuthResult(success = false, message = "Format respons server tidak valid.")
            }
        } else {
            val errorMsg = parseErrorMessage(response) ?: "Autentikasi gagal (Kode HTTP $code)"
            return XiaozhiAuthResult(success = false, message = errorMsg)
        }
    }

    private fun parsePersona(obj: JSONObject?): PersonaAnalysis {
        if (obj == null) {
            return PersonaAnalysis(
                totalChatsAnalyzed = 1,
                confidenceLevel = "Data Awal",
                personality = PersonalityTrait("Ambivert Seimbang", "Xiaozhi sedang mempelajari pola komunikasi Anda.", 50, 50),
                hobbies = listOf(
                    PersonaMetricItem("💻", "Coding & IT", 85),
                    PersonaMetricItem("🎮", "Gaming", 75)
                ),
                challenges = listOf(
                    PersonaMetricItem("⏳", "Manajemen Waktu", 70, "Tinggi", "danger")
                ),
                activities = listOf(
                    PersonaMetricItem("🌙", "Begadang", 80)
                ),
                preferences = listOf(
                    PersonaMetricItem("💡", "Diskusi Santai", 90)
                )
            )
        }

        val totalChats = obj.optInt("total_chats_analyzed", 0)
        val conf = obj.optString("confidence_level", "Data Awal")

        // Personality
        val pObj = obj.optJSONObject("personality") ?: JSONObject()
        val personality = PersonalityTrait(
            primaryTrait = pObj.optString("primary_trait", "Ambivert Seimbang"),
            description = pObj.optString("description", "Pola interaksi seimbang."),
            introvertPercent = pObj.optInt("introvert_percent", 50),
            extrovertPercent = pObj.optInt("extrovert_percent", 50)
        )

        fun parseMetricList(key: String): List<PersonaMetricItem> {
            val arr = obj.optJSONArray(key) ?: JSONArray()
            val list = mutableListOf<PersonaMetricItem>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                list.add(
                    PersonaMetricItem(
                        icon = item.optString("icon", "🎯"),
                        name = item.optString("name", ""),
                        percent = item.optInt("percent", 50),
                        level = item.optString("level", "Sedang"),
                        badge = item.optString("badge", "neutral")
                    )
                )
            }
            return list
        }

        return PersonaAnalysis(
            totalChatsAnalyzed = totalChats,
            confidenceLevel = conf,
            personality = personality,
            hobbies = parseMetricList("hobbies"),
            challenges = parseMetricList("challenges"),
            activities = parseMetricList("activities"),
            preferences = parseMetricList("preferences")
        )
    }

    private fun parseErrorMessage(response: String?): String? {
        if (response.isNullOrBlank()) return null
        return try {
            val json = JSONObject(response)
            if (json.has("detail")) {
                val detail = json.get("detail")
                if (detail is JSONObject) {
                    detail.optString("message", detail.toString())
                } else {
                    detail.toString()
                }
            } else {
                json.optString("message", null)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun executeRequest(
        path: String,
        method: String,
        body: String? = null,
        token: String? = null
    ): Pair<Int, String?> {
        return try {
            doHttp(baseUrl + path, method, body, token)
        } catch (e: Exception) {
            try {
                doHttp(fallbackUrl + path, method, body, token)
            } catch (e2: Exception) {
                -1 to e2.localizedMessage
            }
        }
    }

    private fun doHttp(
        urlString: String,
        method: String,
        body: String?,
        token: String?
    ): Pair<Int, String?> {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 12_000
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            if (token != null) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
            }
        }

        if (body != null) {
            conn.outputStream.use { os ->
                OutputStreamWriter(os, "UTF-8").use { it.write(body) }
            }
        }

        val code = conn.responseCode
        val responseText = try {
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }

        return code to responseText
    }
}
