# Contributing to Android Bedrock Loom

Thank you for your interest in contributing to Android Bedrock Loom! This project aims to bring the power of multiverse navigation to mobile devices, and we welcome contributions from the community.

## 🌳 Philosophy

Android Bedrock Loom is built on the principles of the [Loom interface](https://cyborgism.wiki/hypha/loom):

> "The stochasticity of simulators becomes a powerful advantage instead of a drawback when one can apply selection pressure to its outputs."

We believe in:
- **Exploration over optimization**: The goal is to explore possibility space, not just find the "best" answer
- **User agency**: The weaver should have full control over their multiverse
- **Transparency**: Show the mechanics (logprobs, costs, latency) to empower informed curation
- **Local-first**: Your data belongs to you

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- An AWS account with Bedrock access (for testing with real models)

### Setup

1. **Fork and clone**
   ```bash
   git clone https://github.com/YOUR_USERNAME/android-bedrock-loom.git
   cd android-bedrock-loom
   ```

2. **Open in Android Studio**
   - File → Open → Select the project directory
   - Wait for Gradle sync to complete

3. **Create `local.properties`** (if not auto-created)
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   ```

4. **Run the app**
   - Select a device/emulator
   - Click Run ▶️

### Testing Without AWS Credentials

For development without Bedrock access, use the mock provider:

```kotlin
// In AppModule.kt, uncomment:
@Provides
fun provideGenerationProvider(): GenerationProvider = MockGenerationProvider()
```

This provides deterministic "lorem ipsum" style completions for UI development.

## 📋 How to Contribute

### Reporting Issues

Before creating an issue, please:
1. Search existing issues to avoid duplicates
2. Use the issue templates when available
3. Include:
   - Device model and Android version
   - App version
   - Steps to reproduce
   - Expected vs actual behavior
   - Screenshots/logs if relevant

### Suggesting Features

We love feature suggestions! Please:
1. Check the [Roadmap](README.md#roadmap) first
2. Open a Discussion (not an Issue) for feature ideas
3. Describe the use case, not just the solution
4. Reference similar features in other Loom implementations if applicable

### Pull Requests

#### Before You Start

1. **Check existing PRs** to avoid duplicate work
2. **Open an issue first** for significant changes
3. **Discuss your approach** in the issue before coding

#### Development Process

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Follow the code style (see below)
   - Write tests for new functionality
   - Update documentation if needed

3. **Test thoroughly**
   ```bash
   ./gradlew test           # Unit tests
   ./gradlew connectedTest  # Instrumented tests
   ./gradlew lint           # Lint checks
   ```

4. **Commit with clear messages**
   ```
   feat: Add logprobs visualization to NodeSheet
   
   - Display token probabilities as colored bars
   - Add toggle to show/hide in settings
   - Includes unit tests for probability formatting
   
   Closes #42
   ```

5. **Push and create PR**
   ```bash
   git push origin feature/your-feature-name
   ```
   Then open a PR against `main`

#### PR Requirements

- [ ] All tests pass
- [ ] No new lint warnings
- [ ] Documentation updated (if applicable)
- [ ] Screenshots for UI changes
- [ ] Follows code style guidelines
- [ ] Commits are squashed/rebased cleanly

## 🎨 Code Style

### Kotlin

We follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) with some additions:

```kotlin
// Good: Descriptive names, explicit types for public API
class LoomEngine @Inject constructor(
    private val treeManager: TreeManager,
    private val generator: BranchGenerator,
) {
    fun generateBranches(
        parentId: NodeId,
        count: Int,
        params: GenerationParams,
    ): Flow<GenerationEvent> {
        // ...
    }
}

// Good: Use sealed classes for state
sealed class GenerationState {
    object Idle : GenerationState()
    data class Generating(val progress: Float, val branchIndex: Int) : GenerationState()
    data class Error(val message: String) : GenerationState()
}

// Good: Extension functions for clarity
fun LoomNode.isLeaf(): Boolean = childIds.isEmpty()
fun LoomNode.hasChildren(): Boolean = childIds.isNotEmpty()
```

### Compose

```kotlin
// Good: Preview with sample data
@Preview(showBackground = true)
@Composable
fun NodeCardPreview() {
    LoomTheme {
        NodeCard(
            node = SampleData.sampleNode,
            isSelected = true,
            onClick = {},
        )
    }
}

// Good: Hoist state, pass lambdas
@Composable
fun GenerationControls(
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    branchCount: Int,
    onBranchCountChange: (Int) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ...
}
```

### Formatting

- Use the provided `.editorconfig`
- Run `./gradlew spotlessApply` before committing
- Max line length: 120 characters

## 🧪 Testing

### Unit Tests

Located in `src/test/`. Run with:
```bash
./gradlew test
```

We aim for:
- **Domain layer**: 90%+ coverage
- **Data layer**: 80%+ coverage  
- **ViewModels**: 70%+ coverage

### Instrumented Tests

Located in `src/androidTest/`. Run with:
```bash
./gradlew connectedAndroidTest
```

Focus on:
- Database operations
- UI interactions
- Integration flows

### Mock Providers

Use `MockBedrockClient` for deterministic tests:

```kotlin
class LoomEngineTest {
    private val mockClient = MockBedrockClient(
        responses = mapOf(
            "test prompt" to listOf("response A", "response B", "response C")
        )
    )
    
    @Test
    fun `generateBranches creates correct number of children`() = runTest {
        val engine = LoomEngine(TreeManager(), BranchGenerator(mockClient))
        // ...
    }
}
```

## 📁 Project Structure

See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed structure. Key points:

- **`domain/`**: Pure Kotlin, no Android dependencies, fully unit testable
- **`data/`**: Android/AWS dependencies, repository pattern
- **`ui/`**: Compose screens and components, ViewModels

When adding new features:
1. Start with domain models and logic
2. Add repository/data source if needed
3. Build UI components
4. Wire up in ViewModel
5. Write tests at each layer

## 🔒 Security

### Credential Handling

- NEVER commit AWS credentials
- Use `local.properties` or environment variables for development
- Test credential storage uses Android Keystore

### Reporting Security Issues

Please email security concerns directly rather than opening public issues.

## 📚 Resources

### Understanding Loom

- [Loom: Interface to the Multiverse](https://generative.ink/posts/loom-interface-to-the-multiverse/)
- [Cyborgism Wiki - Loom](https://cyborgism.wiki/hypha/loom)
- [Language Models are Multiverse Generators](https://generative.ink/posts/language-models-are-multiverse-generators/)

### AWS Bedrock

- [AWS Bedrock Documentation](https://docs.aws.amazon.com/bedrock/)
- [AWS SDK for Kotlin](https://github.com/awslabs/aws-sdk-kotlin)
- [Bedrock Pricing](https://aws.amazon.com/bedrock/pricing/)

### Android Development

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Room Database](https://developer.android.com/training/data-storage/room)

## 🙏 Recognition

Contributors will be recognized in:
- The README acknowledgments section
- Release notes for their contributions
- The in-app "About" screen (for significant contributions)

## 📜 License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Questions? Open a Discussion or reach out to the maintainers. Happy weaving! 🧵
