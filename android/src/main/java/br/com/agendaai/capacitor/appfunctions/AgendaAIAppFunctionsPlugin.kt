package br.com.agendaai.capacitor.appfunctions

import br.com.agendaai.capacitor.appfunctions.client.AgendaAIApiException
import br.com.agendaai.capacitor.appfunctions.client.AgendaAIClient
import br.com.agendaai.capacitor.appfunctions.dto.CriarAgendamentoBody
import br.com.agendaai.capacitor.appfunctions.dto.CriarPacienteBody
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

/**
 * Plugin Capacitor que expoe ao JS:
 *
 *  - Set/get/clear de auth token (essencial pro Gemini chamar com app fechado).
 *  - Operacoes de dominio (espelham 1:1 as @AppFunctions registradas no app).
 *
 * As @AppFunctions de fato ficam no app consumidor (KSP build-time), nao aqui.
 */
@CapacitorPlugin(name = "AgendaAIAppFunctions")
class AgendaAIAppFunctionsPlugin : Plugin() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { encodeDefaults = false; ignoreUnknownKeys = true }
    private lateinit var client: AgendaAIClient

    override fun load() {
        super.load()
        client = AgendaAIClient(context)
    }

    // ---------- Auth ----------

    @PluginMethod
    fun setAuthToken(call: PluginCall) {
        val token = call.getString("token")
        if (token.isNullOrEmpty()) {
            call.reject("token obrigatorio")
            return
        }
        client.setAuthToken(token, call.getString("apiBaseUrl"))
        call.resolve()
    }

    @PluginMethod
    fun getAuthToken(call: PluginCall) {
        val result = JSObject()
        result.put("token", client.getAuthToken())
        call.resolve(result)
    }

    @PluginMethod
    fun clearAuthToken(call: PluginCall) {
        client.clearAuthToken()
        call.resolve()
    }

    @PluginMethod
    fun getApiBaseUrl(call: PluginCall) {
        val result = JSObject()
        result.put("apiBaseUrl", client.getApiBaseUrl())
        call.resolve(result)
    }

    // ---------- Pacientes ----------

    @PluginMethod
    fun listarPacientes(call: PluginCall) = runCall(call) {
        val list = client.listarPacientes()
        JSObject().put("pacientes", toJsArray(list))
    }

    @PluginMethod
    fun criarPaciente(call: PluginCall) = runCall(call) {
        val body = CriarPacienteBody(
            nome = call.getString("nome") ?: error("nome obrigatorio"),
            dataNascimento = call.getString("dataNascimento")
                ?: error("dataNascimento obrigatoria"),
            sexo = call.getString("sexo") ?: "O",
            observacoes = call.getString("observacoes"),
        )
        val paciente = client.criarPaciente(body)
        JSObject().put("paciente", toJsObject(paciente))
    }

    @PluginMethod
    fun removerPaciente(call: PluginCall) = runCall(call) {
        val id = call.getString("pacienteId") ?: error("pacienteId obrigatorio")
        val r = client.removerPaciente(id)
        JSObject().put("ok", r.ok)
    }

    // ---------- Profissionais / servicos / slots ----------

    @PluginMethod
    fun listarProfissionais(call: PluginCall) = runCall(call) {
        JSObject().put("profissionais", toJsArray(client.listarProfissionais()))
    }

    @PluginMethod
    fun listarServicos(call: PluginCall) = runCall(call) {
        JSObject().put("servicos", toJsArray(client.listarServicos()))
    }

    @PluginMethod
    fun verHorariosDisponiveis(call: PluginCall) = runCall(call) {
        val slots = client.verHorariosDisponiveis(
            profissionalId = call.getString("profissionalId") ?: error("profissionalId obrigatorio"),
            data = call.getString("data") ?: error("data obrigatoria"),
            servicoId = call.getString("servicoId") ?: error("servicoId obrigatorio"),
        )
        JSObject().put("slots", toJsArray(slots))
    }

    // ---------- Agendamentos ----------

    @PluginMethod
    fun criarAgendamento(call: PluginCall) = runCall(call) {
        val body = CriarAgendamentoBody(
            pacienteId = call.getString("pacienteId") ?: error("pacienteId obrigatorio"),
            profissionalId = call.getString("profissionalId") ?: error("profissionalId obrigatorio"),
            servicoId = call.getString("servicoId") ?: error("servicoId obrigatorio"),
            dataHoraInicio = call.getString("dataHoraInicio") ?: error("dataHoraInicio obrigatorio"),
            observacoes = call.getString("observacoes"),
        )
        val agendamento = client.criarAgendamento(body)
        JSObject().put("agendamento", toJsObject(agendamento))
    }

    @PluginMethod
    fun listarProximosAgendamentos(call: PluginCall) = runCall(call) {
        val agora = System.currentTimeMillis()
        val futuros = client.listarAgendamentos()
            .filter { it.status == "AGENDADO" }
            .filter { runCatching { java.time.Instant.parse(it.dataHoraInicio).toEpochMilli() }.getOrDefault(0L) >= agora }
            .sortedBy { it.dataHoraInicio }
        JSObject().put("agendamentos", toJsArray(futuros))
    }

    @PluginMethod
    fun cancelarAgendamento(call: PluginCall) = runCall(call) {
        val id = call.getString("agendamentoId") ?: error("agendamentoId obrigatorio")
        JSObject().put("agendamento", toJsObject(client.cancelarAgendamento(id)))
    }

    @PluginMethod
    fun agendaDoDia(call: PluginCall) = runCall(call) {
        val data = call.getString("data") ?: error("data obrigatoria")
        val dia = client.listarAgendamentos().filter { it.dataHoraInicio.startsWith(data) }
        JSObject().put("agendamentos", toJsArray(dia))
    }

    // ---------- Helpers ----------

    private fun runCall(call: PluginCall, block: suspend () -> JSObject) {
        scope.launch {
            try {
                val result = block()
                call.resolve(result)
            } catch (e: AgendaAIApiException) {
                call.reject(e.message, e.statusCode.toString(), e)
            } catch (e: Exception) {
                call.reject(e.message ?: "erro desconhecido", e)
            }
        }
    }

    private inline fun <reified T> toJsArray(list: List<T>): JSArray {
        val raw = json.encodeToString(list)
        return JSArray(JSONArray(raw))
    }

    private inline fun <reified T> toJsObject(obj: T): JSObject {
        val raw = json.encodeToString(obj)
        return JSObject(JSONObject(raw).toString())
    }
}
