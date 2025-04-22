# FIAP Hackathon App
App em Kotlin para identificação de objetos cortantes através do upload de imagens,  vídeos ou ou câmera utilizando o modelo TFLite gerado pelo processo de anotação e treinamento do dataset.

## Android SDK

compileSdk = 34

minSdk = 24 (Android 7.0+)

targetSdk = 33

### Gradle & Plugins

Android Gradle Plugin: 8.3.1

Kotlin plugin: 1.9.0

Java/Kotlin compatibilidade: Java 17 / JVM target 

## Plugins principais no build.gradle do módulo:

plugins {
id "com.android.application"
id "org.jetbrains.kotlin.android"
id "kotlin-kapt"
id "kotlin-parcelize"
id "androidx.navigation.safeargs.kotlin"
// se usarem Firebase:
id "com.google.gms.google-services"
id "com.google.firebase.crashlytics"
}

## Bibliotecas TFLite (versão 2.12.0):

implementation "org.tensorflow:tensorflow-lite:2.12.0"
implementation "org.tensorflow:tensorflow-lite-gpu:2.12.0"
implementation "org.tensorflow:tensorflow-lite-support:0.4.4"
implementation "org.tensorflow:tensorflow-lite-task-vision:0.4.4"

#### GPU Delegate (opcional) para acelerar no dispositivo.

## Formato do modelo: best_float32.tflite

Tipo: float32

Input esperado: [320×320] RGB

### Parâmetros de Detecção do Object Detector

## inputSize

    Valor: 640
    Descricao: Tamanho de entrada do modelo (width × height).

## output shape

    Valor:[1, 5, 8400]
    Descricao:Interpretação: 5 arrays de 8400 detections (x, y, w, h, confidence).

## setNumThreads

    Valor:4
    Descricao:Número de threads para quantização e inferência; equilibra velocidade vs. uso de CPU.

## setUseXNNPACK

    Valor:true
    Descricao:	Habilita otimizações XNNPACK para acelerar operações tensoriais.

## detectionThreshold

    Valor:0.0005f
    Descricao:Confiança mínima para considerar uma predição relevante. Ajustar conforme logs TopConfs.

## minAreaThreshold

    Valor:0.01f
    Descricao:Área mínima normalizada (w × h relativa ao input) para filtrar caixas pequenas/ruído.

## normalização RGB

    Valor:[-1, +1]
    Descricao:Cada canal: (pixel/255 - 0.5)/0.5; padroniza valores de 0–255 para -1 a +1.

### Processo de inferência:

## Pré‑processamento:

Redimensiona bitmap para 640×640 + normaliza.

## Inferência:

interpreter.run(inputBuffer, outputArray).

## Pós‑processamento:

Percorre 8400 boxes, aplica thresholds de confiança e área.

## Integração com Telegram

Para notificar em tempo real quando um objeto cortante for detectado:

* Crie um bot no Telegram via BotFather, defina o nome cutwatch e o username Hackathon2IADT_bot (link: t.me/Hackathon2IADT_bot).

*  No código Kotlin (sendTelegramMessage), configure:

        private val TELEGRAM_BOT_TOKEN = "<seu_bot_token>"
        private val TELEGRAM_CHAT_ID = "<seu_chat_id>"

 * Envie uma POST para https://api.telegram.org/bot<token>/sendMessage com chat_id e text.

## Exemplo de mensagem enviada no alarme:

⚠️ Objeto cortante detectado! ⚠️
Hora: HH:mm:ss