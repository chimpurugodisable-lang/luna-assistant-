LUNA On-Device Keyword Spotting (KWS) Assets
---------------------------------------------
Target wake phrases:
- "Luna"
- "Hey Luna"

This folder contains the configuration and token files for the offline wake-word detector.
LunaWakeWordEngine dynamically checks for ONNX binaries and falls back gracefully to
the high-precision on-device acoustic keyword recognizer if ONNX runtime models are missing,
preventing any crashes and ensuring seamless hands-free wake word detection in background and foreground.
