package com.irsyadlabs.espbridge.data.xiaozhi

import com.irsyadlabs.espbridge.data.local.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class XiaozhiRepository(
    private val apiClient: XiaozhiApiClient,
    private val settingsRepository: SettingsRepository
) {
    suspend fun login(username: String, password: String): XiaozhiAuthResult {
        val result = apiClient.login(username, password)
        if (result.success && result.accessToken != null && result.user != null) {
            val mcp = apiClient.getMcpStatus(result.accessToken)
            settingsRepository.saveXiaozhiSession(
                token = result.accessToken,
                refreshToken = result.refreshToken,
                username = result.user.username,
                userId = result.user.id,
                role = result.user.role,
                mcpConnected = mcp.connected,
                preview = mcp.tokenPreview
            )
        }
        return result
    }

    suspend fun register(username: String, password: String): XiaozhiAuthResult {
        val result = apiClient.register(username, password)
        if (result.success && result.accessToken != null && result.user != null) {
            settingsRepository.saveXiaozhiSession(
                token = result.accessToken,
                refreshToken = result.refreshToken,
                username = result.user.username,
                userId = result.user.id,
                role = result.user.role,
                mcpConnected = false
            )
        }
        return result
    }

    suspend fun getMcpStatus(): XiaozhiMcpStatus {
        val token = currentToken() ?: return XiaozhiMcpStatus(false, false, "Tidak terotentikasi")
        val status = apiClient.getMcpStatus(token)
        settingsRepository.setXiaozhiMcpConnected(status.connected, status.tokenPreview)
        return status
    }

    suspend fun saveMcpToken(mcpToken: String): Result<String> {
        val token = currentToken() ?: return Result.failure(Exception("Sesi login berakhir."))
        return apiClient.saveMcpToken(token, mcpToken)
    }

    suspend fun reconnectMcp(): Result<Boolean> {
        val token = currentToken() ?: return Result.failure(Exception("Sesi login berakhir."))
        return apiClient.reconnectMcp(token)
    }

    suspend fun deleteMcpToken(): Result<Boolean> {
        val token = currentToken() ?: return Result.failure(Exception("Sesi login berakhir."))
        val res = apiClient.deleteMcpToken(token)
        if (res.isSuccess) {
            settingsRepository.setXiaozhiMcpConnected(false, "")
        }
        return res
    }

    suspend fun pollMcpConnection(
        maxAttempts: Int = 15,
        delayMs: Long = 2000L,
        onUpdate: (XiaozhiMcpStatus) -> Unit
    ): Boolean {
        val token = currentToken() ?: return false
        for (i in 1..maxAttempts) {
            val status = apiClient.getMcpStatus(token)
            onUpdate(status)
            if (status.connected) {
                settingsRepository.setXiaozhiMcpConnected(true, status.tokenPreview)
                return true
            }
            if (i < maxAttempts) {
                delay(delayMs)
            }
        }
        return false
    }

    // ── Dashboard Data ──
    suspend fun getDashboard(): Result<XiaozhiDashboardData> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        val result = apiClient.getDashboard(token)
        result.onSuccess { data ->
            settingsRepository.setXiaozhiMcpConnected(data.mcpStatus.connected, data.mcpStatus.tokenPreview)
        }
        return result
    }

    // ── Material Operations ──
    suspend fun createMaterial(
        title: String,
        category: String,
        content: String,
        keywords: String = "",
        apiUrl: String = ""
    ): Result<Boolean> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        return apiClient.createMaterial(token, title, category, content, keywords, apiUrl)
    }

    suspend fun updateMaterial(
        id: Int,
        title: String,
        category: String,
        content: String,
        keywords: String = "",
        apiUrl: String = ""
    ): Result<Boolean> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        return apiClient.updateMaterial(token, id, title, category, content, keywords, apiUrl)
    }

    suspend fun deleteMaterial(id: Int): Result<Boolean> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        return apiClient.deleteMaterial(token, id)
    }

    // ── Category Operations ──
    suspend fun createCategory(name: String): Result<Boolean> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        return apiClient.createCategory(token, name)
    }

    suspend fun deleteCategory(catId: Int): Result<Boolean> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        return apiClient.deleteCategory(token, catId)
    }

    // ── Chat History Operations ──
    suspend fun getChatHistory(query: String = "", limit: Int = 100, date: String = ""): Result<XiaozhiChatHistoryData> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        return apiClient.getChatHistory(token, query, limit, date)
    }

    suspend fun clearChatHistory(): Result<Boolean> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        return apiClient.clearChatHistory(token)
    }

    // ── Profile & AI Persona Operations ──
    suspend fun getProfileData(): Result<XiaozhiProfileData> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        return apiClient.getProfileData(token)
    }

    suspend fun scanPersona(): Result<PersonaAnalysis> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        return apiClient.scanPersona(token)
    }

    // ── Smart Home Relays ──
    suspend fun getSmartHomeRelays(): Result<List<SmartHomeRelay>> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        return apiClient.getSmartHomeState(token)
    }

    suspend fun setRelay(channel: Int, state: Boolean): Result<Boolean> {
        val token = currentToken() ?: return Result.failure(Exception("Tidak terotentikasi."))
        return apiClient.setRelay(token, channel, state)
    }

    suspend fun logout() {
        settingsRepository.clearXiaozhiSession()
    }

    private suspend fun currentToken(): String? {
        return settingsRepository.settings.first().xiaozhiAccessToken
    }
}
