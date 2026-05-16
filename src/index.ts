import { registerPlugin } from '@capacitor/core';

import type { AgendaAIAppFunctionsPlugin } from './definitions';

const AgendaAIAppFunctions = registerPlugin<AgendaAIAppFunctionsPlugin>(
  'AgendaAIAppFunctions',
  {
    web: () => import('./web').then((m) => new m.AgendaAIAppFunctionsWeb()),
  },
);

export * from './definitions';
export { AgendaAIAppFunctions };
