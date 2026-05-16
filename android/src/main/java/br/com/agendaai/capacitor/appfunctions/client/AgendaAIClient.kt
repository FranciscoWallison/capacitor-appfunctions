package br.com.agendaai.capacitor.appfunctions.client

import android.content.Context
import br.com.agendaai.capacitor.appfunctions.dto.AgendamentoDto
import br.com.agendaai.capacitor.appfunctions.dto.ApiErrorBody
import br.com.agendaai.capacitor.appfunctions.dto.CriarAgendamentoBody
import br.com.agendaai.capacitor.appfunctions.dto.CriarPacienteBody
import br.com.agendaai.capacitor.appfunctions.dto.OkResponse
import br.com.agendaai.capacitor.appfunctions.dto.PacienteDto
import br.com.agendaai.capacitor.appfunctions.dto.ProfissionalDto
import br.com.agendaai.capacitor.appfunctions.dto.ServicoDto
import br.com.agendaai.capacitor.appfunctions.dto.SlotLivreDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP do agendaAI usado tanto pelo bridge Capacitor (foreground)
 * quanto pelas @AppFunctions (background invocado pelo Gemini).
 *
 * Toda chamada le o token mais recente do [TokenStore] e injeta como
 * `Authorization: Bearer ...`. Erros HTTP nao-2xx viram [AgendaAIApiException].
 *
 * Uso:
 * ```
 * val client = AgendaAIClient(context)
 * val pacientes = client.listarPacientes()
 * ```
 */
class AgendaAIClient(context: Context) {

    private val tokenStore = TokenStore(context)
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ---------- Pacientes ----------

    suspend fun listarPacientes(): List<PacienteDto> =
        get("/pacientes")

    suspend fun criarPaciente(body: CriarPacienteBody): PacienteDto =
        post("/pacientes", body)

    suspend fun removerPaciente(pacienteId: String): OkResponse =
        delete("/pacientes/$pacienteId")

    // ---------- Profissionais / servicos / slots ----------

    suspend fun listarProfissionais(): List<ProfissionalDto> =
        get("/profissionais", auth = false)

    suspend fun listarServicos(): List<ServicoDto> =
        get("/servicos", auth = false)

    suspend fun verHorariosDisponiveis(
        profissionalId: String,
        data: String,
        servicoId: String,
    ): List<SlotLivreDto> =
        get("/profissionais/$profissionalId/slots?data=$data&servicoId=$servicoId", auth = false)

    // ---------- Agendamentos ----------

    suspend fun criarAgendamento(body: CriarAgendamentoBody): AgendamentoDto =
        post("/agendamentos", body)

    suspend fun listarAgendamentos(): List<AgendamentoDto> =
        get("/agendamentos")

    suspend fun cancelarAgendamento(agendamentoId: String): AgendamentoDto =
        delete("/agendamentos/$agendamentoId")

    // ---------- Auth utilitario ----------

    /** Define token + (opcional) base URL em memoria persistente. */
    fun setAuthToken(token: String, apiBaseUrl: String? = null) {
        tokenStore.token = token
        if (apiBaseUrl != null) tokenStore.apiBaseUrl = apiBaseUrl
    }

    fun getAuthToken(): String? = tokenStore.token
    fun getApiBaseUrl(): String = tokenStore.apiBaseUrl
    fun clearAuthToken() = tokenStore.clear()

    // ---------- HTTP plumbing ----------

    private suspend inline fun <reified T> get(
        path: String,
        auth: Boolean = true,
    ): T = withContext(Dispatchers.IO) {
        executeAndParse(buildRequest(path, auth).get().build())
    }

    private suspend inline fun <reified TBody, reified TResp> post(
        path: String,
        body: TBody,
    ): TResp = withContext(Dispatchers.IO) {
        val bodyJson = json.encodeToString(body)
        val req = buildRequest(path, auth = true)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()
        executeAndParse(req)
    }

    private suspend inline fun <reified T> delete(path: String): T =
        withContext(Dispatchers.IO) {
            executeAndParse(buildRequest(path, auth = true).delete().build())
        }

    private fun buildRequest(path: String, auth: Boolean): Request.Builder {
        val builder = Request.Builder()
            .url(tokenStore.apiBaseUrl.trimEnd('/') + path)
            .header("Accept", "application/json")
        if (auth) {
            val token = tokenStore.token
                ?: throw AgendaAIApiException(
                    401,
                    null,
                    "Sem token de auth - peca o usuario abrir o app e fazer login.",
                )
            builder.header("Authorization", "Bearer $token")
        }
        return builder
    }

    private inline fun <reified T> executeAndParse(request: Request): T {
        http.newCall(request).execute().use { resp ->
            ensureSuccess(resp)
            val raw = resp.body?.string().orEmpty()
            return json.decodeFromString(raw)
        }
    }

    private fun ensureSuccess(resp: Response) {
        if (resp.isSuccessful) return
        val raw = resp.body?.string()
        val parsed = runCatching { json.decodeFromString<ApiErrorBody>(raw.orEmpty()) }
            .getOrNull()
        val message = parsed?.message ?: parsed?.error ?: "HTTP ${resp.code}"
        throw AgendaAIApiException(resp.code, raw, message)
    }
}
