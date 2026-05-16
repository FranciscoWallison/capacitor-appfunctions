# Como reativar AppFunctions no agendaAI

> **Estado atual:** AppFunctions desativado no app `frontend-capacitor` por incompatibilidade entre `androidx.appfunctions:1.0.0-alpha08` e o stack atual (Kotlin 2.1.21 + KSP 2.1.21-2.0.1 + AGP 8.13). O plugin Capacitor permanece **funcional** — `AgendaAIClient`, `TokenStore`, DTOs e bridge JS continuam ativos.

## Por que foi desativado

| Erro encontrado | Causa |
|---|---|
| `alpha09` exige AGP 9.1 + compileSdk 37 | Capacitor 8 ainda nao migrou pra AGP 9. |
| `alpha07` Unresolved reference `'AppFunction'` | A anotacao so apareceu como classe runtime em alpha08. |
| `alpha08` Empty collection can't be reduced (KSP) | O `appfunctions-compiler` alpha08 tem bug em single-module ou exige KSP2 (Kotlin 2.3.20). |

## Caminho A: esperar alpha10+ ou stable

Quando `androidx.appfunctions` chegar em **beta** ou stable, voltar aqui:

1. Reativar deps em `frontend-capacitor/android/app/build.gradle`:
   ```gradle
   apply plugin: 'com.google.devtools.ksp'

   implementation "androidx.appfunctions:appfunctions:<nova-versao>"
   implementation "androidx.appfunctions:appfunctions-service:<nova-versao>"
   ksp "androidx.appfunctions:appfunctions-compiler:<nova-versao>"
   ```
2. Renomear `AgendaAIFunctions.kt.disabled` → `AgendaAIFunctions.kt`.
3. Reativar `<service>` + `<meta-data>` em `frontend-capacitor/android/app/src/main/AndroidManifest.xml` (esta comentado).
4. Subir `minSdkVersion = 35` em `variables.gradle` (AppFunctions exige Android U+).
5. Build: `.\gradlew :app:assembleDebug --no-daemon`.

## Caminho B: subir Kotlin/KSP pra versao do sample NotyKT (alpha08 com KSP2)

Se quiser usar alpha08 ja:

1. Em `frontend-capacitor/android/build.gradle` (raiz), subir:
   ```gradle
   classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20"
   classpath "org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:2.3.20"
   classpath "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6"
   ```
2. Idem em `plugins/capacitor-appfunctions/android/build.gradle`.
3. Em `variables.gradle`, atualizar `kotlinVersion` e `kspVersion`.
4. Risco: Capacitor 8 talvez nao seja compativel com Kotlin 2.3.20 — testar.

## Como testar quando reativar

```powershell
# Build APK
cd F:\projetos\app\agendaAI\frontend-capacitor\android
.\gradlew :app:assembleDebug --no-daemon

# Install
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Inspecionar catalogo registrado no sistema
adb shell dumpsys appfunctions | findstr br.com.agendaai.app

# Testar via Gemini (device com Gemini + Android 15+):
# "lista meus pacientes no agendaAI"
# "marca uma consulta para amanha 10h pra Joao com a Dra. Marina"
```

## O que NAO foi perdido

- Plugin Capacitor `@agendaai/capacitor-appfunctions` continua funcional via TS:
  ```ts
  import { AgendaAIAppFunctions } from '@agendaai/capacitor-appfunctions';
  await AgendaAIAppFunctions.setAuthToken({ token, apiBaseUrl });
  const { pacientes } = await AgendaAIAppFunctions.listarPacientes();
  ```
- `AgendaAIClient.kt` (HTTP OkHttp), `TokenStore.kt`, DTOs `@AppFunctionSerializable` continuam intocados no plugin.
- AuthService TS continua empurrando token via `setAuthToken` no login (no-op se nao Android).

Quando AppFunctions voltar, o trabalho restante e so anotar metodos em `AgendaAIFunctions.kt` que delegam pro `AgendaAIClient` (ja escrito).
