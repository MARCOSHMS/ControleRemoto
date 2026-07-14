# Como gerar o .apk (usando GitHub Actions)

Esse caminho **não precisa de Android Studio instalado** — só precisa
rodar 3 comandos localmente (pra gerar a pasta `android/` uma vez) e
depois o GitHub compila o `.apk` pra você na nuvem, de graça.

## 1) Preparar localmente (só uma vez)

Precisa ter o **Node.js** instalado (https://nodejs.org/, versão LTS).

Abra o Prompt de Comando dentro desta pasta (`android-app`) e rode, um
comando de cada vez:

```
npm install
npx cap add android
npx cap sync
```

Isso cria a pasta `android/` com o projeto nativo completo.

## 2) Colocar o plugin de descoberta automática no lugar

O app "acha" o PC sozinho na rede usando um pequeno plugin nativo
(arquivos `.java`). Copie esses dois arquivos da pasta `native-plugin/`
pra dentro do projeto gerado:

**De:**
```
native-plugin/PcDiscoveryPlugin.java
native-plugin/MainActivity.java
```

**Para:**
```
android/app/src/main/java/com/wonit/controleremotopc/
```

(Essa pasta de destino só existe depois do passo 1. O `MainActivity.java`
que já existir lá deve ser **substituído** pelo novo.)

## 3) Subir pro GitHub

1. Crie um repositório novo no GitHub (pode ser privado)
2. Suba todo o conteúdo desta pasta `android-app` pra ele, **incluindo a
   pasta `android/` gerada e já com os arquivos do plugin no lugar**
   (é importante commitar a pasta `android/` — ela não pode ficar de
   fora, diferente do padrão usual de projetos Capacitor)
3. O arquivo `.github/workflows/build-apk.yml` já está pronto — assim
   que você fizer o primeiro `push` pra branch `main`, o GitHub já
   começa a gerar o `.apk` sozinho

## 4) Pegar o APK gerado

1. No GitHub, vá na aba **Actions** do repositório
2. Clique na execução mais recente (ícone verde ✓ quando terminar)
3. Lá embaixo, em **Artifacts**, baixe **ControleRemotoPC-apk** (vem como
   um `.zip`, dentro tem o `app-debug.apk`)

## 5) Instalar no celular

1. Copie o `.apk` pro celular
2. Toque nele pra instalar (o Android vai pedir permissão de "fontes
   desconhecidas" — normal, é assim pra qualquer apk fora da Play Store)
3. Abra o app — ele já começa a **procurar o PC sozinho na rede** e
   mostra numa lista pra você tocar. Só falta digitar o PIN.

Se não achar nada em alguns segundos, aparece a opção "Digitar endereço
manualmente" como plano B.

---

## Como funciona a busca automática

- O programa no PC manda um "aviso" pela rede Wi-Fi a cada poucos
  segundos, dizendo seu nome e endereço.
- O app no celular escuta esses avisos e monta a lista.
- **Isso só funciona dentro do app instalado** (não funciona abrindo
  pelo navegador comum) — é um recurso exclusivo do `.apk`.
- Em algumas redes (Wi-Fi de empresa, "isoladas", ou economia de bateria
  muito agressiva no celular) esse aviso pode não chegar — por isso
  sempre deixamos a opção manual como reserva.

---

## Se der erro no build do GitHub Actions

Como não tenho como compilar esse código Kotlin/Java aqui pra testar
antes, é possível que o primeiro build falhe por algum detalhe do
projeto Android gerado. Se acontecer:

1. Vá em **Actions** → clique na execução que falhou (❌)
2. Abra o log da etapa que falhou (geralmente "Gerar APK (debug)")
3. Me manda o print do erro que eu ajusto o código

---

## Personalização (opcional)

- **Ícone do app:** por enquanto usa o ícone padrão do Capacitor. Dá pra
  trocar em `android/app/src/main/res/` depois.
- **Nome do app / pacote:** já configurado como "Controle Remoto PC" /
  `com.wonit.controleremotopc` no `capacitor.config.json`.
