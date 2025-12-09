# 🌌 Android Bedrock Loom

**Version 1.1**

> *Weave realities through probabilistic space, anywhere you go.*

**Android Bedrock Loom** is a mobile implementation of the [Loom interface](https://cyborgism.wiki/hypha/loom) for Android, powered by AWS Bedrock's foundation models. It brings the full multiverse navigation paradigm to your pocket—generate, branch, explore, and curate infinite possibilities from Claude and other Bedrock models.

## ✨ Features

### 💬 Chat Interface
- **Real-time streaming**: Watch Claude's responses appear in real-time
- **Markdown rendering**: Beautiful formatting with horizontal scrolling for code blocks and ASCII art
- **Message editing**: Long-press any message to edit, regenerate, or copy
- **Conversation titles**: Auto-generated titles that reflect your chat content
- **Smart input**: Text input starts with uppercase by default

### 🌳 Tree Navigation
- **Branch anywhere**: Continue conversations from any point in the history
- **Visual tree map**: See your entire conversation tree at a glance
- **Continue from here**: Pick any node and continue from that point
- **Multi-branch support**: Each message can have multiple child responses

### ⚙️ Configuration
- **Model selection**: Choose your preferred Claude model from AWS Bedrock
- **Temperature control**: Adjust creativity from 0.0 (focused) to 1.0 (creative), default 1.0
- **Max tokens**: Configure response length
- **System prompts**: Customize Claude's behavior
- **Region selection**: Use any AWS Bedrock-enabled region

### 💾 Persistence & Export
- **Local-first**: All conversations stored on your device with Room database
- **Import/Export**: Share conversations in JSON format
- **Conversation management**: Rename, reload, and delete conversations
- **Data privacy**: Your credentials and chats stay on your device

### ☁️ AWS Bedrock Integration
- **Streaming responses**: Real-time message generation with ConverseStream API
- **Latest models**: Support for Claude Opus 4.5 and other Bedrock models
- **Secure credentials**: AWS credentials stored locally with DataStore

## 📱 Screenshots

```
┌─────────────────────────────────┐
│  🌌 Bedrock Loom               │
├─────────────────────────────────┤
│                                 │
│  "Once upon a time..."          │
│         │                       │
│    ┌────┴────┐                  │
│    ▼         ▼                  │
│  [A]       [B]                  │
│   │         │                   │
│   ▼         ├──┬──┐             │
│  [A1]      [B1][B2][B3] ←───●  │
│                    │            │
│                   ▼            │
│               [B3.1]           │
│                                 │
├─────────────────────────────────┤
│ [⟳ Branch] [💾 Save] [🔍 Search]│
└─────────────────────────────────┘
```

## 🚀 Getting Started

### Prerequisites
- Android 8.0 (API 26) or higher
- AWS Account with Bedrock access
- Bedrock model access granted (Claude, Titan, etc.)

### Installation

#### From Releases
1. Download the latest APK from [Releases](https://github.com/gabriel-berzescu/android-bedrock-loom/releases)
2. Enable "Install from unknown sources" if needed
3. Install and launch

#### Build from Source
```bash
git clone https://github.com/gabriel-berzescu/android-bedrock-loom.git
cd android-bedrock-loom

# Build only (creates APK in app/build/outputs/apk/debug/)
./gradlew assembleDebug

# Build and install to connected device
./gradlew installDebug
```

### Configuration

1. Launch the app and tap **Settings** ⚙️
2. Enter your AWS credentials in INI format:
   ```
   [default]
   aws_access_key_id=YOUR_ACCESS_KEY
   aws_secret_access_key=YOUR_SECRET_KEY
   aws_session_token=YOUR_SESSION_TOKEN (optional)
   ```
3. Configure your preferences:
   - **Region**: Your Bedrock-enabled region (default: `eu-west-1`)
   - **Model ID**: Your preferred model (default: `global.anthropic.claude-opus-4-5-20251101-v1:0`)
   - **Max Tokens**: Response length (default: `256`)
   - **Temperature**: Creativity level 0.0-1.0 (default: `1.0`)
   - **System Prompt**: Customize Claude's behavior
4. Start chatting! 💬

## 🎮 Usage

### Basic Usage

1. **Start a conversation**: Tap the ➕ button from the conversation list
2. **Send a message**: Type your message and tap the send button
3. **View responses**: Watch Claude's response stream in real-time with markdown formatting
4. **Edit & regenerate**: Long-press any message to:
   - **Edit** your message and resend
   - **Regenerate** Claude's response
   - **Copy** message text to clipboard
5. **View conversation tree**: Tap the tree icon to see your conversation branches
6. **Branch from any point**: In tree view, select a node and tap "Continue from here"

### Advanced Features

#### Conversation Management
- **Rename**: Long-press a conversation to rename it
- **Reload**: Refresh the conversation list to see updates
- **Delete**: Swipe or long-press to delete conversations
- **Import/Export**: Use the import/export buttons to share conversations as JSON files

#### Tree Navigation
- **Visual map**: See the full conversation tree structure
- **Branch selection**: Tap any node to view its content
- **Continue branching**: Select a node and continue the conversation from that point
- **Multiple paths**: Explore different conversation directions from the same starting point

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     Presentation Layer (Jetpack Compose)     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ ChatScreen   │  │ TreeScreen   │  │ SettingsScreen   │  │
│  │              │  │              │  │                  │  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
┌─────────┼──────────────────┼──────────────────┼─────────────┐
│         ▼                  ▼                  ▼             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 ViewModels (Hilt)                    │   │
│  │  ChatViewModel  │  TreeViewModel  │  SettingsVM     │   │
│  └─────────────────────┬───────────────────────────────┘   │
│                        │       Business Logic              │
└────────────────────────┼───────────────────────────────────┘
                         │
┌────────────────────────┼───────────────────────────────────┐
│                        ▼          Data Layer               │
│  ┌──────────────┐  ┌───────────────┐  ┌────────────────┐ │
│  │ BedrockClient│  │ ConversationDao│  │ AppPreferences │ │
│  │ (AWS SDK)    │  │ (Room)        │  │ (DataStore)    │ │
│  └──────┬───────┘  └───────┬───────┘  └────────┬───────┘ │
│         │                  │                    │         │
│         ▼                  ▼                    ▼         │
│  [AWS Bedrock]       [SQLite DB]         [Preferences]   │
│   ConverseStream    Node Tree Storage    Credentials     │
└───────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Responsibility |
|-----------|----------------|
| `ChatScreen` | Main chat interface with message list and input |
| `TreeScreen` | Interactive tree visualization with Canvas |
| `BedrockClient` | AWS Bedrock API communication with streaming support |
| `ConversationDao` | Room database access for tree structure persistence |
| `AppPreferences` | DataStore wrapper for settings and credentials |
| `ChatViewModel` | Chat state management, message handling, API calls |

## 📦 Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Hilt dependency injection
- **Async**: Kotlin Coroutines + Flow
- **Database**: Room (SQLite)
- **Preferences**: DataStore
- **AWS**: AWS SDK for Kotlin (Bedrock Runtime)
- **Serialization**: Kotlinx Serialization (JSON)
- **Markdown**: compose-markdown for rich text rendering
- **Canvas**: Compose Canvas for tree visualization

## 🤝 Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Setup
```bash
# Clone the repo
git clone https://github.com/gabriel-berzescu/android-bedrock-loom.git

# Open in Android Studio
# Sync Gradle
# Create local.properties with your SDK path
# Run on device/emulator
```

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

## 🙏 Acknowledgments

- [Janus](https://generative.ink) for creating the original Loom concept and [pyloom](https://github.com/socketteer/loom)
- [Morpheus](https://cyborgism.wiki/hypha/morpheus) for naming the Loom of Time
- The [cyborgism](https://cyborgism.wiki) community for pioneering human-AI collaboration patterns
- AWS for making powerful foundation models accessible via Bedrock

---

*"For a novice weaver, even the slightest change can cause ripples that cascade into an infinity of nightmares. It is recommended that those studying the Loom stop living in linear time and begin thinking in terms of Multiverses..."*

— [Weaving the Moment with the Loom of Time](https://generative.ink/loom/toc/)
