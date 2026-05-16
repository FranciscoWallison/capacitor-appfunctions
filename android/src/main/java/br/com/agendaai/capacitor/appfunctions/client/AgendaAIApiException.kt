package br.com.agendaai.capacitor.appfunctions.client

/** Erro de chamada HTTP ao backend agendaAI. */
class AgendaAIApiException(
    val statusCode: Int,
    val errorBody: String?,
    message: String,
) : RuntimeException(message)
