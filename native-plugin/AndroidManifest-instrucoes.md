# Ajustes no AndroidManifest.xml

Esse arquivo é gerado automaticamente pelo `npx cap add android` em:
```
android/app/src/main/AndroidManifest.xml
```

Ele já vem com uma estrutura padrão do Capacitor. Você precisa fazer
**dois ajustes** nele (não precisa reescrever o arquivo inteiro, só
adicionar/editar as partes abaixo).

## 1) Adicionar as permissões de rede/Wi-Fi

Logo abaixo da linha `<uses-permission android:name="android.permission.INTERNET" />`
(que já deve existir), adicione estas quatro:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
```

Essas linhas ficam **fora** da tag `<application>`, junto com as outras
`<uses-permission>` que já existirem, geralmente logo abaixo da tag
`<manifest ...>`.

## 2) Habilitar cleartext e vincular o network_security_config

Na tag `<application ...>` (a mesma que já tem `android:allowBackup`,
`android:icon`, `android:label`, etc.), adicione estes dois atributos:

```xml
<application
    android:usesCleartextTraffic="true"
    android:networkSecurityConfig="@xml/network_security_config"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    ...>
```

(mantenha os atributos que já existem, só acrescente os dois novos)

## 3) Colocar o arquivo network_security_config.xml no lugar certo

Copie o arquivo `native-plugin/network_security_config.xml` (dessa
pasta) para:

```
android/app/src/main/res/xml/network_security_config.xml
```

A pasta `res/xml/` normalmente não existe ainda — pode criar ela.

---

Depois desses 3 passos, o manifest + a config de rede já permitem tanto
o Socket.IO (HTTP comum) quanto abrem caminho pro UDP funcionar (a parte
de UDP também depende do código Java, que já foi ajustado — veja
`PcDiscoveryPlugin.java` atualizado).
