# B.A.C.H. Android MIDI

Port inicial do sequenciador generativo B.A.C.H. para Android, com interface inspirada no painel físico de referência.

## O que inclui
- Painel escuro com knobs para Material, Memory, Growth, Morph, Density, Tension, Interaction e Humanize.
- LEDs Play/Stop, Break, Reset e quatro indicadores coloridos.
- Display circular azul com BPM e quatro vozes.
- Seleção de saída MIDI Android, Play/Stop, controle de BPM e envio de notas.
- Dois geradores de exemplo: PROBA (Markov simplificado) e CELLULAR (autômato celular Regra 30).
- GitHub Actions para compilar o APK debug e disponibilizá-lo como artefato.

## Compilar
1. Crie um repositório vazio no GitHub e envie estes arquivos.
2. Abra a aba **Actions** e aguarde o workflow **Build BACH Android APK**.
3. Baixe o artefato `BACH-MIDI-debug-apk`; dentro estará `app-debug.apk`.

Também é possível abrir o projeto no Android Studio e compilar com Gradle 8.2 + JDK 17.

## MIDI
O aplicativo usa `android.media.midi`. A disponibilidade de portas virtuais depende da versão do Android e dos apps instalados. O app lista dispositivos MIDI com porta de saída disponível; selecione um dispositivo para conectar e toque em PLAY.

## Estado do projeto
Esta é uma base funcional inicial, não uma reprodução certificada do firmware original. Os motores são protótipos simplificados e ainda não representam os 12 algoritmos completos do módulo original.
