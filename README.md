# ChandanGem App

> **Engineered & Owned by Chandan Kumar**  
> An autonomous multi-agent AI assistant for Android with Jetpack Compose, native hands-free voice loop, Termux terminal management, and Model Context Protocol (MCP) tool integration.

---

## Highlights

* 🎙️ **Full-Duplex Voice Assistant**: Hands-free conversational loop integrating Speech-to-Text (STT), Text-to-Speech (TTS), barge-in interruption, and real-time audio waveform visualizers.
* 💻 **Terminal & Shell Execution Manager**: Native execution of bash/sh scripts, interactive terminal sessions with streaming standard I/O, scrollback buffers, and Termux environment bridging.
* 🔌 **Model Context Protocol (MCP) Client**: Full JSON-RPC 2.0 implementation supporting both local STDIO process transports and remote/local Server-Sent Events (SSE) web endpoints.
* 🤖 **Multi-Agent Collaborative System**: Autonomous routing and delegation between specialized agents:
  * **Jarvis**: Lead conversational coordinator (Neon Cyan)
  * **Shell Specialist**: Linux & Termux commands execution (Emerald Green)
  * **MCP Dispatcher**: Protocol queries and tool invocation (Cyber Purple)
  * **Code Architect**: Android & Kotlin system design (Amber)
* 🎨 **Jetpack Compose & Material 3 UI**: Futuristic dark cyber-theme, glassmorphism input dock, collapsible tool invocation cards, and interactive modal bottom sheets for terminal and MCP inspection.

---

## Project Structure

```
chandangem-app/
├── app/
│   ├── build.gradle.kts               # Module build configuration
│   ├── src/main/AndroidManifest.xml   # Permissions & application declaration
│   ├── src/main/java/com/chandankumar/chandangem/
│   │   ├── ChandanGemApplication.kt   # Singleton infrastructure bootstrap
│   │   ├── MainActivity.kt            # Compose host & runtime permission handling
│   │   ├── core/
│   │   │   ├── voice/                 # STT, TTS, hands-free loop, waveform DSP
│   │   │   ├── terminal/              # Shell execution, sessions, Termux integration
│   │   │   ├── mcp/                   # MCP Client, STDIO/SSE transports, registry
│   │   │   └── agent/                 # Agent definitions, router, coordinator
│   │   └── ui/
│   │       ├── theme/                 # Cyber dark palette, Typography, M3 Theme
│   │       ├── chat/                  # ChatScreen, ViewModel, MessageBubble, InputBar
│   │       ├── terminal/              # TerminalSheet & ViewModel
│   │       └── mcp/                   # McpServerSheet & ViewModel
│   └── src/test/java/                 # Comprehensive Unit Test Suite
├── gradle/
│   ├── libs.versions.toml             # Gradle Version Catalog
│   └── wrapper/                       # Gradle 8.10.2 wrapper
├── build.gradle.kts                   # Top-level Gradle build
├── settings.gradle.kts                # Project settings
├── ARCHITECTURE.md                    # In-depth architectural specification
└── README.md                          # Project documentation
```

---

## Building and Running

### Prerequisites
* **Android Studio** Ladybug (2024.2.1+) or newer
* **JDK 17+**
* **Android SDK** API 35 (minSdk 26)

### Build with Gradle CLI
```bash
# Clean and assemble debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Documentation

For full architecture diagrams, sequence flows, and design rationale, see:
* [ARCHITECTURE.md](file:///data/user/0/com.termux/files/home/projects/chandangem-app/ARCHITECTURE.md)

---

## Author & License

* **Owner**: Chandan Kumar
* **Project**: ChandanGem Mobile Assistant
* **License**: Proprietary / AntiGem Open Standard
