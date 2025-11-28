# 🌌 Android Bedrock Loom

> *Weave realities through probabilistic space, anywhere you go.*

**Android Bedrock Loom** is a mobile implementation of the [Loom interface](https://cyborgism.wiki/hypha/loom) for Android, powered by AWS Bedrock's foundation models. It brings the full multiverse navigation paradigm to your pocket—generate, branch, explore, and curate infinite possibilities from Claude, Titan, and other Bedrock models.

## ✨ Features

### 🌳 Full Tree Navigation
- **Branch anywhere**: Tap any node to spawn new realities
- **Visual multiverse map**: See your entire exploration tree at a glance
- **Swipe navigation**: Fluid gesture-based timeline traversal
- **Pinch to zoom**: Explore vast multiverses with intuitive controls

### 🎲 Probabilistic Generation
- **Multi-completion spawning**: Generate N branches simultaneously
- **Temperature control**: Fine-tune the chaos dial from deterministic to wildly creative
- **Token probability inspection**: See the logprobs behind each choice
- **Beam search exploration**: Systematically explore high-probability paths

### 🎯 Selection & Curation
- **Favorites & bookmarks**: Mark golden branches for later
- **Tag system**: Organize your multiverse with custom labels
- **Search across timelines**: Find that perfect branch you saw three realities ago
- **Smart filtering**: Hide branches by criteria, focus on what matters

### ☁️ AWS Bedrock Integration
- **Multiple models**: Claude 3.5 Sonnet, Claude 3 Opus, Amazon Titan, and more
- **Streaming responses**: Watch realities unfold in real-time
- **Cost tracking**: Monitor your token usage across explorations
- **Offline queue**: Draft prompts offline, generate when connected

### 💾 Persistence & Sync
- **Local-first**: Your multiverses live on your device
- **Export formats**: JSON tree, Markdown linear, HTML visualization
- **Cloud backup**: Optional sync to S3 for cross-device weaving
- **Share branches**: Export specific paths as shareable snippets

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
./gradlew assembleDebug
```

### Configuration

1. Launch the app and tap **Settings** ⚙️
2. Enter your AWS credentials:
   - **Region**: Your Bedrock-enabled region (e.g., `us-east-1`)
   - **Access Key ID**: Your AWS access key
   - **Secret Access Key**: Your AWS secret key
3. Select your preferred default model
4. Start weaving! 🧵

## 🎮 Usage

### Basic Weaving

1. **Start a thread**: Tap the ✏️ button and write your prompt
2. **Generate branches**: Tap 🌳 to spawn N completions (configurable)
3. **Navigate**: Tap any node to view its content, swipe to traverse
4. **Branch deeper**: From any node, generate more continuations
5. **Curate**: Star ⭐ the branches worth keeping

### Power User Features

#### Keyboard Shortcuts (with physical keyboard)
| Key | Action |
|-----|--------|
| `Space` | Generate from current node |
| `←/→` | Navigate siblings |
| `↑/↓` | Navigate parent/child |
| `S` | Star current node |
| `E` | Edit current node |
| `/` | Search |

#### Custom Generation Parameters
```
Temperature: 0.0 ════●════ 2.0
Top P:       0.0 ═══════●═ 1.0  
Max Tokens:  [256] [512] [1024] [2048] [Custom]
Branches:    [1] [3] [5] [8] [13]
```

#### LLM Programs (Advanced)
Define reusable generation patterns:
```yaml
name: "Character Voice Explorer"
system: "You are exploring different character voices."
generations:
  - prompt_suffix: " [speaking formally]"
    temperature: 0.3
  - prompt_suffix: " [speaking casually]"  
    temperature: 0.7
  - prompt_suffix: " [speaking poetically]"
    temperature: 1.2
```

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                        Presentation Layer                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ TreeView    │  │ NodeEditor  │  │ SettingsScreen      │  │
│  │ (Compose)   │  │ (Compose)   │  │ (Compose)           │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
└─────────┼────────────────┼────────────────────┼──────────────┘
          │                │                    │
┌─────────┼────────────────┼────────────────────┼──────────────┐
│         ▼                ▼                    ▼              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                    ViewModels                        │    │
│  │  LoomViewModel  │  NodeViewModel  │  SettingsVM     │    │
│  └─────────────────────────┬───────────────────────────┘    │
│                            │          Domain Layer           │
│  ┌─────────────────────────┼───────────────────────────┐    │
│  │                         ▼                           │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  │    │
│  │  │ LoomEngine   │  │ TreeManager  │  │ Exporter │  │    │
│  │  └───────┬──────┘  └──────────────┘  └──────────┘  │    │
│  └──────────┼──────────────────────────────────────────┘    │
└─────────────┼────────────────────────────────────────────────┘
              │
┌─────────────┼────────────────────────────────────────────────┐
│             ▼                  Data Layer                    │
│  ┌──────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │ BedrockClient    │  │ TreeRepository  │  │ PrefsStore  │ │
│  │ (AWS SDK)        │  │ (Room DB)       │  │ (DataStore) │ │
│  └────────┬─────────┘  └────────┬────────┘  └──────┬──────┘ │
│           │                     │                   │        │
│           ▼                     ▼                   ▼        │
│     [AWS Bedrock]         [SQLite]           [SharedPrefs]  │
└──────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Responsibility |
|-----------|----------------|
| `LoomEngine` | Core branching logic, generation orchestration |
| `TreeManager` | Tree data structure, navigation, manipulation |
| `BedrockClient` | AWS Bedrock API communication, streaming |
| `TreeRepository` | Persistence, caching, query optimization |
| `TreeView` | Interactive tree visualization (Canvas + Compose) |

## 📦 Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Canvas for tree rendering
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Async**: Kotlin Coroutines + Flow
- **Database**: Room
- **Preferences**: DataStore
- **AWS**: AWS SDK for Kotlin (Bedrock)
- **Serialization**: Kotlinx Serialization

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

### Roadmap

- [x] Core tree data structure
- [x] Bedrock API integration
- [x] Basic tree visualization
- [x] Node editing
- [ ] Multi-model support
- [ ] Logprobs visualization  
- [ ] LLM programs
- [ ] S3 cloud sync
- [ ] Collaborative weaving (multiplayer)
- [ ] Voice input for mobile weaving
- [ ] Wear OS companion (quick branch from watch)

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
