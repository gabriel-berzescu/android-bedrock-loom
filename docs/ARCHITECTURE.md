# 🏗️ Architecture Documentation

## Overview

Android Bedrock Loom follows Clean Architecture principles with MVVM presentation pattern, optimized for the unique requirements of a multiverse navigation interface.

## Core Concepts

### The Loom Tree

The fundamental data structure is a **directed rooted tree** where:
- Each **node** contains text content (a completion or user input)
- Each **edge** represents a generation event
- The **root** is the initial prompt
- **Leaves** are unexpanded terminal states
- The **playhead** marks the current navigation position

```
                    [Root: "Once upon a time..."]
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
         [Branch A]      [Branch B]      [Branch C]
        "...there was"  "...in a land"  "...the world"
              │               │
              ▼          ┌────┴────┐
         [A.1]          ▼         ▼
       "a dragon"    [B.1]      [B.2]
                   "far away"  "of magic"
                                  │
                             ◉ ←──┘ (playhead)
```

### Node Anatomy

```kotlin
data class LoomNode(
    val id: NodeId,                    // UUID
    val content: String,               // The actual text
    val parentId: NodeId?,             // null for root
    val childIds: List<NodeId>,        // Ordered children
    val metadata: NodeMetadata,        // Generation info
    val userAnnotations: Annotations,  // Stars, tags, notes
    val timestamp: Instant,            // Creation time
)

data class NodeMetadata(
    val modelId: String,               // e.g., "anthropic.claude-3-sonnet"
    val temperature: Float,
    val topP: Float,
    val maxTokens: Int,
    val tokenCount: Int,               // Actual tokens generated
    val logprobs: List<TokenLogprob>?, // If available
    val latencyMs: Long,
    val inputTokens: Int,              // For cost tracking
    val outputTokens: Int,
)

data class Annotations(
    val starred: Boolean = false,
    val tags: Set<String> = emptySet(),
    val note: String? = null,
    val hidden: Boolean = false,
)
```

## Layer Architecture

### Presentation Layer

```
┌──────────────────────────────────────────────────────────────────┐
│                        UI Components                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────┐   │
│  │   TreeCanvas   │  │  NodeSheet     │  │  ControlPanel    │   │
│  │                │  │                │  │                  │   │
│  │ • Pan/Zoom     │  │ • View content │  │ • Gen params     │   │
│  │ • Node render  │  │ • Edit mode    │  │ • Model select   │   │
│  │ • Edge drawing │  │ • Annotations  │  │ • Branch count   │   │
│  │ • Selection    │  │ • Actions      │  │ • Quick actions  │   │
│  └───────┬────────┘  └───────┬────────┘  └────────┬─────────┘   │
│          │                   │                     │             │
│          └───────────────────┼─────────────────────┘             │
│                              │                                   │
│                              ▼                                   │
│                    ┌──────────────────┐                          │
│                    │   LoomScreen     │                          │
│                    │   (Scaffold)     │                          │
│                    └────────┬─────────┘                          │
│                             │                                    │
└─────────────────────────────┼────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  LoomViewModel   │
                    └──────────────────┘
```

#### LoomViewModel

Central state holder for the Loom interface:

```kotlin
class LoomViewModel @Inject constructor(
    private val loomEngine: LoomEngine,
    private val treeRepository: TreeRepository,
    private val bedrockClient: BedrockClient,
) : ViewModel() {

    // UI State
    val uiState: StateFlow<LoomUiState>
    
    // Tree state
    val tree: StateFlow<LoomTree>
    val playhead: StateFlow<NodeId>
    val selectedNodes: StateFlow<Set<NodeId>>
    
    // Generation state
    val generationState: StateFlow<GenerationState>
    val generationParams: StateFlow<GenerationParams>
    
    // Actions
    fun navigateTo(nodeId: NodeId)
    fun generateBranches(count: Int)
    fun editNode(nodeId: NodeId, newContent: String)
    fun toggleStar(nodeId: NodeId)
    fun addTag(nodeId: NodeId, tag: String)
    fun deleteSubtree(nodeId: NodeId)
    fun exportTree(format: ExportFormat): Flow<ExportResult>
}
```

### Domain Layer

#### LoomEngine

Core business logic for tree operations:

```kotlin
class LoomEngine @Inject constructor(
    private val treeManager: TreeManager,
    private val generator: BranchGenerator,
) {
    // Tree operations
    fun createTree(rootContent: String): LoomTree
    fun addBranch(parentId: NodeId, content: String, metadata: NodeMetadata): LoomNode
    fun removeBranch(nodeId: NodeId): LoomTree
    fun reorderChildren(parentId: NodeId, newOrder: List<NodeId>)
    
    // Path operations
    fun getAncestry(nodeId: NodeId): List<LoomNode>
    fun getFullText(nodeId: NodeId, separator: String = "\n"): String
    fun findPath(fromId: NodeId, toId: NodeId): List<NodeId>?
    
    // Generation orchestration
    suspend fun generateBranches(
        parentId: NodeId,
        count: Int,
        params: GenerationParams,
    ): Flow<GenerationEvent>
}

sealed class GenerationEvent {
    data class Started(val parentId: NodeId, val count: Int) : GenerationEvent()
    data class BranchStarted(val index: Int) : GenerationEvent()
    data class TokenReceived(val index: Int, val token: String) : GenerationEvent()
    data class BranchCompleted(val index: Int, val node: LoomNode) : GenerationEvent()
    data class AllCompleted(val nodes: List<LoomNode>) : GenerationEvent()
    data class Error(val index: Int, val error: Throwable) : GenerationEvent()
}
```

#### TreeManager

Immutable tree data structure operations:

```kotlin
class TreeManager {
    fun createNode(content: String, parentId: NodeId?, metadata: NodeMetadata): LoomNode
    fun insertNode(tree: LoomTree, node: LoomNode): LoomTree
    fun removeNode(tree: LoomTree, nodeId: NodeId): LoomTree
    fun updateNode(tree: LoomTree, nodeId: NodeId, transform: (LoomNode) -> LoomNode): LoomTree
    
    // Queries
    fun getNode(tree: LoomTree, nodeId: NodeId): LoomNode?
    fun getChildren(tree: LoomTree, nodeId: NodeId): List<LoomNode>
    fun getParent(tree: LoomTree, nodeId: NodeId): LoomNode?
    fun getAncestors(tree: LoomTree, nodeId: NodeId): List<LoomNode>
    fun getDescendants(tree: LoomTree, nodeId: NodeId): List<LoomNode>
    
    // Search
    fun search(tree: LoomTree, query: String): List<SearchResult>
    fun filterByTag(tree: LoomTree, tag: String): List<LoomNode>
    fun getStarred(tree: LoomTree): List<LoomNode>
}
```

### Data Layer

#### BedrockClient

AWS Bedrock API wrapper with streaming support:

```kotlin
class BedrockClient @Inject constructor(
    private val credentialsProvider: CredentialsProvider,
    private val config: BedrockConfig,
) {
    // Model info
    suspend fun listAvailableModels(): List<BedrockModel>
    fun getModel(modelId: String): BedrockModel?
    
    // Generation
    suspend fun generate(request: GenerationRequest): GenerationResponse
    
    // Streaming generation
    fun generateStream(request: GenerationRequest): Flow<StreamChunk>
    
    // Batch generation (parallel branches)
    fun generateBatch(
        requests: List<GenerationRequest>,
        parallelism: Int = 3,
    ): Flow<IndexedStreamChunk>
}

data class GenerationRequest(
    val modelId: String,
    val prompt: String,
    val systemPrompt: String? = null,
    val maxTokens: Int,
    val temperature: Float,
    val topP: Float,
    val stopSequences: List<String> = emptyList(),
)

sealed class StreamChunk {
    data class Token(val text: String, val logprob: Float? = null) : StreamChunk()
    data class Metrics(val inputTokens: Int, val outputTokens: Int, val latencyMs: Long) : StreamChunk()
    object Done : StreamChunk()
}
```

#### TreeRepository

Persistence layer using Room:

```kotlin
@Dao
interface TreeDao {
    @Query("SELECT * FROM trees")
    fun getAllTrees(): Flow<List<TreeEntity>>
    
    @Query("SELECT * FROM trees WHERE id = :treeId")
    suspend fun getTree(treeId: String): TreeEntity?
    
    @Query("SELECT * FROM nodes WHERE treeId = :treeId")
    suspend fun getNodesForTree(treeId: String): List<NodeEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTree(tree: TreeEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: NodeEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<NodeEntity>)
    
    @Delete
    suspend fun deleteNode(node: NodeEntity)
    
    @Query("DELETE FROM nodes WHERE treeId = :treeId AND id IN (:nodeIds)")
    suspend fun deleteNodes(treeId: String, nodeIds: List<String>)
}

class TreeRepository @Inject constructor(
    private val treeDao: TreeDao,
    private val nodeDao: NodeDao,
) {
    fun getAllTrees(): Flow<List<LoomTree>>
    suspend fun getTree(treeId: TreeId): LoomTree?
    suspend fun saveTree(tree: LoomTree)
    suspend fun saveNode(treeId: TreeId, node: LoomNode)
    suspend fun deleteTree(treeId: TreeId)
    suspend fun deleteSubtree(treeId: TreeId, rootNodeId: NodeId)
}
```

## Data Flow

### Generation Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Generation Flow                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  User taps "Generate 5 branches"                                    │
│         │                                                           │
│         ▼                                                           │
│  ┌─────────────────┐                                                │
│  │  LoomViewModel  │                                                │
│  │  generateBranches(5)                                             │
│  └────────┬────────┘                                                │
│           │                                                         │
│           ▼                                                         │
│  ┌─────────────────┐                                                │
│  │   LoomEngine    │                                                │
│  │  • Get ancestry │───► Build full prompt from root to playhead   │
│  │  • Prepare reqs │                                                │
│  └────────┬────────┘                                                │
│           │                                                         │
│           ▼                                                         │
│  ┌─────────────────┐     ┌─────────────────────────────────────┐   │
│  │  BedrockClient  │────►│  AWS Bedrock (5 parallel requests)  │   │
│  │  generateBatch()│◄────│  Streaming responses                │   │
│  └────────┬────────┘     └─────────────────────────────────────┘   │
│           │                                                         │
│           │ Flow<IndexedStreamChunk>                                │
│           ▼                                                         │
│  ┌─────────────────┐                                                │
│  │   LoomEngine    │                                                │
│  │  • Create nodes │                                                │
│  │  • Update tree  │                                                │
│  └────────┬────────┘                                                │
│           │                                                         │
│           ▼                                                         │
│  ┌─────────────────┐                                                │
│  │ TreeRepository  │                                                │
│  │  • Persist nodes│                                                │
│  └────────┬────────┘                                                │
│           │                                                         │
│           ▼                                                         │
│  ┌─────────────────┐     ┌─────────────────┐                       │
│  │  LoomViewModel  │────►│    TreeCanvas    │                       │
│  │  emit new state │     │  Animate new     │                       │
│  └─────────────────┘     │  branches in     │                       │
│                          └─────────────────┘                       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Navigation Flow

```
User taps node in TreeCanvas
        │
        ▼
┌─────────────────┐
│  TreeCanvas     │
│  onNodeClick()  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  LoomViewModel  │
│  navigateTo()   │
│  • Update playhead
│  • Load full ancestry text
│  • Update UI state
└────────┬────────┘
         │
         ├──────────────────┐
         ▼                  ▼
┌─────────────────┐  ┌─────────────────┐
│  TreeCanvas     │  │  NodeSheet      │
│  • Highlight    │  │  • Show content │
│  • Center view  │  │  • Show metadata│
└─────────────────┘  └─────────────────┘
```

## Module Structure

```
app/
├── src/main/kotlin/com/loom/bedrock/
│   ├── LoomApplication.kt
│   ├── MainActivity.kt
│   │
│   ├── di/                          # Hilt modules
│   │   ├── AppModule.kt
│   │   ├── BedrockModule.kt
│   │   └── DatabaseModule.kt
│   │
│   ├── ui/                          # Presentation layer
│   │   ├── theme/
│   │   │   ├── Theme.kt
│   │   │   ├── Color.kt
│   │   │   └── Typography.kt
│   │   │
│   │   ├── screens/
│   │   │   ├── loom/
│   │   │   │   ├── LoomScreen.kt
│   │   │   │   ├── LoomViewModel.kt
│   │   │   │   └── LoomUiState.kt
│   │   │   ├── settings/
│   │   │   │   ├── SettingsScreen.kt
│   │   │   │   └── SettingsViewModel.kt
│   │   │   └── treelist/
│   │   │       ├── TreeListScreen.kt
│   │   │       └── TreeListViewModel.kt
│   │   │
│   │   └── components/
│   │       ├── TreeCanvas.kt        # Main tree visualization
│   │       ├── NodeSheet.kt         # Bottom sheet for node details
│   │       ├── ControlPanel.kt      # Generation controls
│   │       ├── NodeRenderer.kt      # Individual node drawing
│   │       └── EdgeRenderer.kt      # Connection line drawing
│   │
│   ├── domain/                      # Business logic
│   │   ├── model/
│   │   │   ├── LoomTree.kt
│   │   │   ├── LoomNode.kt
│   │   │   ├── NodeMetadata.kt
│   │   │   └── GenerationParams.kt
│   │   │
│   │   ├── engine/
│   │   │   ├── LoomEngine.kt
│   │   │   ├── TreeManager.kt
│   │   │   └── BranchGenerator.kt
│   │   │
│   │   └── export/
│   │       ├── Exporter.kt
│   │       ├── JsonExporter.kt
│   │       ├── MarkdownExporter.kt
│   │       └── HtmlExporter.kt
│   │
│   └── data/                        # Data layer
│       ├── bedrock/
│       │   ├── BedrockClient.kt
│       │   ├── BedrockConfig.kt
│       │   ├── models/
│       │   │   ├── BedrockModel.kt
│       │   │   ├── GenerationRequest.kt
│       │   │   └── GenerationResponse.kt
│       │   └── CredentialsProvider.kt
│       │
│       ├── db/
│       │   ├── LoomDatabase.kt
│       │   ├── TreeDao.kt
│       │   ├── NodeDao.kt
│       │   └── entities/
│       │       ├── TreeEntity.kt
│       │       └── NodeEntity.kt
│       │
│       ├── repository/
│       │   ├── TreeRepository.kt
│       │   └── SettingsRepository.kt
│       │
│       └── preferences/
│           └── PreferencesDataStore.kt
│
├── src/main/res/
│   ├── values/
│   │   ├── strings.xml
│   │   └── themes.xml
│   └── drawable/
│
└── src/test/
    └── kotlin/com/loom/bedrock/
        ├── domain/
        │   ├── TreeManagerTest.kt
        │   └── LoomEngineTest.kt
        └── data/
            └── BedrockClientTest.kt
```

## Key Design Decisions

### 1. Immutable Tree Structure

The tree is treated as an immutable data structure. All modifications return a new tree instance. This enables:
- Easy undo/redo implementation
- Predictable state management
- Safe concurrent access

### 2. Streaming-First Generation

All generation operations use Kotlin Flow for streaming:
- Real-time token display as they arrive
- Early cancellation support
- Memory-efficient for long generations
- Natural parallel branch handling

### 3. Canvas-Based Tree Rendering

Using Compose Canvas instead of nested composables:
- Better performance with large trees (1000+ nodes)
- Smooth pan/zoom with arbitrary transforms
- Custom node/edge rendering
- Efficient hit testing for selection

### 4. Local-First Persistence

All data stored locally first:
- Works offline
- Fast access
- User owns their data
- Cloud sync is additive, not required

### 5. Model-Agnostic Core

The core Loom logic is independent of Bedrock:
- `BedrockClient` implements a `GenerationProvider` interface
- Could swap in other providers (OpenAI, local models)
- Makes testing easier with mock providers

## Performance Considerations

### Large Tree Handling

For trees with 1000+ nodes:
- **Virtualized rendering**: Only render visible nodes
- **Level-of-detail**: Collapse distant subtrees to single nodes
- **Async loading**: Load node content on-demand
- **Indexed search**: Pre-built search index for fast queries

### Memory Management

- **Node content streaming**: For very long completions, store reference to file
- **LRU cache**: Keep recently accessed nodes in memory
- **Weak references**: For non-playhead nodes

### Network Efficiency

- **Request batching**: Parallel branch generation in single logical operation
- **Response caching**: Cache identical prompts (with same params)
- **Retry with backoff**: Handle transient Bedrock errors gracefully
