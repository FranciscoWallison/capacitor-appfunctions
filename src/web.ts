import { WebPlugin } from '@capacitor/core';

import type {
  AgendaAIAppFunctionsPlugin,
  Agendamento,
  CriarAgendamentoInput,
  CriarPacienteInput,
  Paciente,
  Profissional,
  Servico,
  SlotLivre,
  VerHorariosInput,
} from './definitions';

/**
 * Fallback web: nao expoe AppFunctions (so existe em Android).
 * Mantemos o set/get/clear de token funcional via localStorage para facilitar
 * testes em browser, mas as operacoes de dominio retornam erro.
 */
export class AgendaAIAppFunctionsWeb
  extends WebPlugin
  implements AgendaAIAppFunctionsPlugin
{
  private static TOKEN_KEY = 'agendaai.appfunctions.token';
  private static BASE_KEY = 'agendaai.appfunctions.base';

  async setAuthToken(options: {
    token: string;
    apiBaseUrl?: string;
  }): Promise<void> {
    localStorage.setItem(AgendaAIAppFunctionsWeb.TOKEN_KEY, options.token);
    if (options.apiBaseUrl) {
      localStorage.setItem(AgendaAIAppFunctionsWeb.BASE_KEY, options.apiBaseUrl);
    }
  }

  async getAuthToken(): Promise<{ token: string | null }> {
    return {
      token: localStorage.getItem(AgendaAIAppFunctionsWeb.TOKEN_KEY),
    };
  }

  async clearAuthToken(): Promise<void> {
    localStorage.removeItem(AgendaAIAppFunctionsWeb.TOKEN_KEY);
  }

  async getApiBaseUrl(): Promise<{ apiBaseUrl: string }> {
    return {
      apiBaseUrl:
        localStorage.getItem(AgendaAIAppFunctionsWeb.BASE_KEY) ??
        'https://agendaai-backend-qw6w.onrender.com',
    };
  }

  async listarPacientes(): Promise<{ pacientes: Paciente[] }> {
    throw this.unimplemented('listarPacientes() so no Android.');
  }
  async criarPaciente(
    _options: CriarPacienteInput,
  ): Promise<{ paciente: Paciente }> {
    throw this.unimplemented('criarPaciente() so no Android.');
  }
  async removerPaciente(_options: {
    pacienteId: string;
  }): Promise<{ ok: boolean }> {
    throw this.unimplemented('removerPaciente() so no Android.');
  }
  async listarProfissionais(): Promise<{ profissionais: Profissional[] }> {
    throw this.unimplemented('listarProfissionais() so no Android.');
  }
  async listarServicos(): Promise<{ servicos: Servico[] }> {
    throw this.unimplemented('listarServicos() so no Android.');
  }
  async verHorariosDisponiveis(
    _options: VerHorariosInput,
  ): Promise<{ slots: SlotLivre[] }> {
    throw this.unimplemented('verHorariosDisponiveis() so no Android.');
  }
  async criarAgendamento(
    _options: CriarAgendamentoInput,
  ): Promise<{ agendamento: Agendamento }> {
    throw this.unimplemented('criarAgendamento() so no Android.');
  }
  async listarProximosAgendamentos(): Promise<{ agendamentos: Agendamento[] }> {
    throw this.unimplemented('listarProximosAgendamentos() so no Android.');
  }
  async cancelarAgendamento(_options: {
    agendamentoId: string;
  }): Promise<{ agendamento: Agendamento }> {
    throw this.unimplemented('cancelarAgendamento() so no Android.');
  }
  async agendaDoDia(_options: {
    data: string;
  }): Promise<{ agendamentos: Agendamento[] }> {
    throw this.unimplemented('agendaDoDia() so no Android.');
  }
}
