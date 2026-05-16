package br.com.agendaai.capacitor.appfunctions.dto

import androidx.appfunctions.AppFunctionSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs do dominio agendaAI.
 *
 * Cada DTO tem duas anotacoes:
 *  - @AppFunctionSerializable: permite ser parametro/retorno de @AppFunction (lido pelo Gemini).
 *  - @kotlinx.serialization.Serializable: permite serializar pra/de JSON do backend REST.
 *
 * Os campos seguem o contrato da API REST do agendaAI (Render).
 */

@AppFunctionSerializable
@Serializable
data class PacienteDto(
    val id: String,
    @SerialName("responsavelId") val responsavelId: String,
    val nome: String,
    @SerialName("dataNascimento") val dataNascimento: String,
    val sexo: String = "O",
    val observacoes: String? = null,
    val ativo: Boolean = true,
)

@AppFunctionSerializable
@Serializable
data class ProfissionalDto(
    val id: String,
    val nome: String,
    val crm: String,
    val especialidade: String,
    val telefone: String? = null,
    val ativo: Boolean = true,
)

@AppFunctionSerializable
@Serializable
data class ServicoDto(
    val id: String,
    val nome: String,
    @SerialName("duracaoMinutos") val duracaoMinutos: Int,
    @SerialName("precoCentavos") val precoCentavos: Int = 0,
)

@AppFunctionSerializable
@Serializable
data class SlotLivreDto(
    val inicio: String,
    val fim: String,
)

@AppFunctionSerializable
@Serializable
data class AgendamentoDto(
    val id: String,
    @SerialName("pacienteId") val pacienteId: String,
    @SerialName("profissionalId") val profissionalId: String,
    @SerialName("servicoId") val servicoId: String,
    @SerialName("dataHoraInicio") val dataHoraInicio: String,
    @SerialName("dataHoraFim") val dataHoraFim: String,
    val status: String,
    val observacoes: String? = null,
    @SerialName("criadoEm") val criadoEm: String,
    @SerialName("canceladoEm") val canceladoEm: String? = null,
    val paciente: PacienteDto? = null,
    val profissional: ProfissionalDto? = null,
    val servico: ServicoDto? = null,
)

/** Resposta do auth/login que armazenamos no TokenStore. */
@Serializable
data class LoginResponseDto(
    val accessToken: String,
    val responsavel: ResponsavelMini,
)

@Serializable
data class ResponsavelMini(
    val id: String,
    val nome: String,
    val email: String,
)

// ---- Bodies para POST ----

@Serializable
data class CriarPacienteBody(
    val nome: String,
    val dataNascimento: String,
    val sexo: String? = "O",
    val observacoes: String? = null,
)

@Serializable
data class CriarAgendamentoBody(
    val pacienteId: String,
    val profissionalId: String,
    val servicoId: String,
    val dataHoraInicio: String,
    val observacoes: String? = null,
)

@Serializable
data class OkResponse(val ok: Boolean)

@Serializable
data class ApiErrorBody(
    val statusCode: Int? = null,
    val error: String? = null,
    val message: String? = null,
)
