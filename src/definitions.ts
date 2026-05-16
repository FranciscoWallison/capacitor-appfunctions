/**
 * Interface TS exposta ao app Capacitor.
 *
 * Esse plugin tem dois papeis:
 *
 * 1. Permitir que o app (em foreground) chame as MESMAS operacoes que o agent
 *    invoca (compartilha logica HTTP + token).
 *
 * 2. Empurrar o JWT pro lado nativo (TokenStore) pra que as @AppFunctions
 *    do agendaAI possam autenticar quando o Gemini chamar com o app fechado.
 *
 * As anotacoes @AppFunction de fato ficam no app consumidor (KSP exige isso).
 */

export interface AgendaAIAppFunctionsPlugin {
  /** Empurra/atualiza o JWT no TokenStore nativo. */
  setAuthToken(options: { token: string; apiBaseUrl?: string }): Promise<void>;

  /** Le o token atualmente armazenado (null se nao houver). */
  getAuthToken(): Promise<{ token: string | null }>;

  /** Limpa o token nativo (chamado no logout). */
  clearAuthToken(): Promise<void>;

  /** Le a apiBaseUrl atual configurada no plugin. */
  getApiBaseUrl(): Promise<{ apiBaseUrl: string }>;

  // ---- Operacoes (foreground bridge) ----
  // Espelham 1:1 as @AppFunctions registradas no app consumidor.

  listarPacientes(): Promise<{ pacientes: Paciente[] }>;
  criarPaciente(options: CriarPacienteInput): Promise<{ paciente: Paciente }>;
  removerPaciente(options: { pacienteId: string }): Promise<{ ok: boolean }>;

  listarProfissionais(): Promise<{ profissionais: Profissional[] }>;
  listarServicos(): Promise<{ servicos: Servico[] }>;
  verHorariosDisponiveis(
    options: VerHorariosInput,
  ): Promise<{ slots: SlotLivre[] }>;

  criarAgendamento(
    options: CriarAgendamentoInput,
  ): Promise<{ agendamento: Agendamento }>;
  listarProximosAgendamentos(): Promise<{ agendamentos: Agendamento[] }>;
  cancelarAgendamento(options: {
    agendamentoId: string;
  }): Promise<{ agendamento: Agendamento }>;
  agendaDoDia(options: { data: string }): Promise<{ agendamentos: Agendamento[] }>;
}

// ---- Tipos de dominio (devem casar com os DTOs Kotlin do plugin) ----

export interface Paciente {
  id: string;
  responsavelId: string;
  nome: string;
  dataNascimento: string;
  sexo: 'M' | 'F' | 'O';
  observacoes?: string | null;
  ativo: boolean;
}

export interface Profissional {
  id: string;
  nome: string;
  crm: string;
  especialidade: string;
  telefone?: string | null;
  ativo: boolean;
}

export interface Servico {
  id: string;
  nome: string;
  duracaoMinutos: number;
  precoCentavos: number;
}

export interface SlotLivre {
  inicio: string;
  fim: string;
}

export type StatusAgendamento = 'AGENDADO' | 'CANCELADO' | 'CONCLUIDO';

export interface Agendamento {
  id: string;
  pacienteId: string;
  profissionalId: string;
  servicoId: string;
  dataHoraInicio: string;
  dataHoraFim: string;
  status: StatusAgendamento;
  observacoes?: string | null;
  criadoEm: string;
  canceladoEm: string | null;
  paciente?: Paciente;
  profissional?: Profissional;
  servico?: Servico;
}

// ---- Inputs ----

export interface CriarPacienteInput {
  nome: string;
  dataNascimento: string;
  sexo?: 'M' | 'F' | 'O';
  observacoes?: string;
}

export interface VerHorariosInput {
  profissionalId: string;
  servicoId: string;
  data: string; // YYYY-MM-DD
}

export interface CriarAgendamentoInput {
  pacienteId: string;
  profissionalId: string;
  servicoId: string;
  dataHoraInicio: string; // ISO 8601
  observacoes?: string;
}
