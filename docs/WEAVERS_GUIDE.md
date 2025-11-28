# 🧵 The Weaver's Guide

> *"For a novice weaver, even the slightest change can cause ripples that cascade into an infinity of nightmares. It is recommended that those studying the Loom stop living in linear time and begin thinking in terms of Multiverses..."*

Welcome to the art of weaving realities with Android Bedrock Loom. This guide will teach you to navigate the infinite branching paths of possibility.

## Understanding the Multiverse

### What is the Loom?

The Loom is not a chatbot. It is not an assistant. It is a **multiverse generator**.

When you interact with a language model through a chat interface, you see one reality—a single path through possibility space. The model could have said a thousand different things, but you only see one. The others vanish, unrealized.

The Loom shows you the multiverse. Instead of one response, you generate many. Instead of a linear conversation, you navigate a tree. You become a **weaver**—one who explores, selects, and curates infinite possibilities.

```
Traditional Chat:          The Loom:

User: Hello                User: Hello
  │                           │
  ▼                      ┌────┼────┐
AI: Hi there!            ▼    ▼    ▼
  │                    [A]  [B]  [C]
  ▼                     │    │    │
User: How are you?      ▼   ...   ...
  │                   [A.1]
  ▼                     │
AI: I'm doing well!    ...

One path.              Many paths. You choose.
```

### The Power of Selection

The core insight of the Loom is this: **selection is information**.

When you generate five branches and star one as good, you've carved out a piece of possibility space. You've said "realities like this one." As you continue to select and branch, you narrow down toward the realities you want—without ever having to explicitly describe them.

This is especially powerful for:
- **Creative writing**: Explore character voices, plot directions, tones
- **Problem solving**: Generate multiple approaches, combine the best aspects
- **Discovering the unexpected**: Sometimes the third or fifth branch has something you never would have asked for
- **Learning**: See how the model "thinks" by comparing different paths

## Basic Weaving

### Starting a Thread

1. Tap the **✏️ New** button
2. Enter your initial prompt—this becomes the **root** of your tree
3. Tap **Create**

Your prompt can be:
- A story beginning: `"The last human on Earth sat alone in a room. There was a knock at the door."`
- A question: `"What are three approaches to solving climate change?"`
- A partial thought: `"The meaning of life is"`
- A conversation start: `"User: Hi\nAssistant:"`

### Generating Branches

With your root selected, tap **🌳 Branch** to generate continuations.

**Branch Count**: How many parallel realities to spawn (1-13)
- Start with 3-5 to get a feel for the variation
- Use more when you want to cast a wider net
- Use 1 when you're happy with the direction and just want to continue

**Temperature**: Controls randomness (0.0-2.0)
- **0.0-0.3**: Focused, predictable, good for factual content
- **0.5-0.8**: Balanced creativity and coherence (default)
- **1.0-1.5**: High creativity, more unexpected outputs
- **1.5+**: Wild, may lose coherence but finds rare possibilities

### Navigating the Tree

- **Tap a node**: Select it, view its content
- **Double-tap**: Set as playhead (current position)
- **Swipe left/right** on a node: Navigate to siblings
- **Swipe up/down**: Navigate to parent/children
- **Pinch**: Zoom in/out of the tree view
- **Two-finger drag**: Pan the tree view

### The Playhead

The **playhead** (marked with ◉) indicates your current position in the tree. When you generate new branches, they spawn from the playhead.

The text sent to the model is the **full ancestry** from root to playhead. For example:

```
[Root: "Once upon a time"]
          │
    [A: "there was a dragon"]      ← Selected ancestors
          │
    [A.1: "named Ember"]           ← Selected ancestors
          │
         ◉ (playhead)              ← Generation happens here

Prompt sent to model:
"Once upon a time there was a dragon named Ember"
```

### Curating Your Multiverse

- **⭐ Star**: Mark exceptional branches for later
- **🏷️ Tag**: Add labels like "good-dialogue", "plot-hole", "revisit"
- **📝 Note**: Add private annotations to any node
- **🙈 Hide**: Remove branches from view without deleting

## Advanced Techniques

### The Scatter-Select Loop

The fundamental weaving pattern:

1. **Scatter**: Generate many branches (high count, higher temperature)
2. **Scan**: Quickly review all branches
3. **Select**: Star the promising ones
4. **Focus**: Choose one to continue from
5. **Repeat**: Branch again from the chosen path

This lets you explore broadly while maintaining depth.

### Temperature Gradients

Vary temperature as you weave:

- **Start high** (1.0+): Cast a wide net for interesting directions
- **Go medium** (0.6-0.8): Develop promising paths coherently
- **End low** (0.2-0.4): Polish and refine final content

### Contrastive Generation

Generate the same continuation with different temperatures:

1. Set temperature to 0.3, generate 3 branches
2. Set temperature to 1.2, generate 3 branches
3. Compare: The low-temp branches show "expected" continuations; high-temp shows "unexpected"
4. Often the best path combines elements from both

### Prompt Surgery

Edit any node to change its content:

1. Tap a node, then tap **Edit**
2. Modify the text
3. Save

This changes all downstream paths. Use it to:
- Fix typos or errors
- Adjust tone or style
- Inject new information
- Steer the narrative

### The Merge Technique

When two branches have elements you like:

1. Star both branches
2. Navigate to their common ancestor
3. Edit a new branch that combines the best elements
4. Continue from the merged version

### System Prompts

Set a system prompt in **Settings → Generation → System Prompt**:

```
You are a master storyteller in the style of Ursula K. Le Guin.
Focus on character interiority and worldbuilding.
```

The system prompt is prepended to every generation, influencing style without consuming context space.

### Stop Sequences

Define where generation should halt:

- `\n\n` - Stop at paragraph breaks
- `User:` - Stop before the next user turn (for conversations)
- `---` - Stop at scene breaks

Shorter generations = faster iteration = more exploration.

## Working with Different Content Types

### Fiction Writing

**Goal**: Explore narrative possibilities, find surprising directions

**Approach**:
- Start with a scene or premise
- Generate 5-8 branches at temp 0.8-1.0
- Star the ones with interesting hooks
- Continue the most promising, but keep alternatives alive
- Use lower temp (0.5) for dialogue to maintain voice consistency

**Tips**:
- Don't over-constrain early—let the model surprise you
- When stuck, increase temperature dramatically
- Save your starred "golden branches" as potential story beats

### Brainstorming / Ideation

**Goal**: Generate diverse ideas, avoid fixation

**Approach**:
- Frame prompt as open question
- Generate 8-13 branches at temp 1.0+
- Don't dismiss "bad" ideas immediately—they may inspire good ones
- Look for themes across branches

**Tips**:
- The fifth idea is often better than the first
- Weird branches can unlock lateral thinking
- Tag ideas by category: "practical", "moonshot", "weird-but-interesting"

### Problem Solving

**Goal**: Find solutions you wouldn't have thought of

**Approach**:
- State the problem clearly
- Generate solution approaches (5 branches, temp 0.7)
- For each approach, generate implementation details
- Cross-pollinate: take implementation from branch A, apply to approach from branch B

**Tips**:
- Ask "what would [expert persona] suggest?" in separate branches
- Generate failure modes, then solutions to those failures
- Star both the best solutions AND the most interesting failures

### Learning / Research

**Goal**: Understand a topic from multiple angles

**Approach**:
- Ask a question
- Generate multiple explanations (different angles, different levels)
- Follow up on confusing parts with more branches
- Tag branches by explanation style: "analogy", "technical", "historical"

**Tips**:
- Generate "explain like I'm 5" and "explain for an expert" side by side
- Ask for counterarguments and criticisms
- Build a tree of increasingly specific questions

## Model Selection

### Claude 3.5 Sonnet
- **Best for**: General use, coding, analysis, creative writing
- **Strengths**: Balanced capabilities, good instruction following
- **Ideal temp**: 0.5-0.9

### Claude 3 Opus
- **Best for**: Complex reasoning, nuanced writing, difficult problems
- **Strengths**: Highest capability, longest context
- **Ideal temp**: 0.4-0.8
- **Note**: Slower and more expensive

### Claude 3 Haiku
- **Best for**: Fast iteration, simple tasks, high branch counts
- **Strengths**: Very fast, cheapest
- **Ideal temp**: 0.6-1.0
- **Note**: Use for broad exploration, then switch to Sonnet for depth

### Amazon Titan
- **Best for**: Experimentation, different "voice"
- **Strengths**: Different training, may find unique paths
- **Ideal temp**: 0.5-1.0

## Keyboard Shortcuts

(When using a physical keyboard)

| Shortcut | Action |
|----------|--------|
| `Space` | Generate branches from playhead |
| `Enter` | View/edit selected node |
| `←` / `→` | Navigate to sibling |
| `↑` / `↓` | Navigate to parent/child |
| `Home` | Jump to root |
| `S` | Star/unstar selected node |
| `H` | Hide selected node |
| `D` | Delete selected subtree |
| `E` | Edit selected node |
| `/` | Open search |
| `Esc` | Close dialogs, deselect |
| `1`-`9` | Set branch count |
| `[` / `]` | Decrease/increase temperature |

## Best Practices

### Don't Over-Prune

Resist the urge to delete "bad" branches. They:
- Provide contrast to understand what makes good branches good
- May become useful later as context changes
- Document your exploration process

Instead of deleting, **hide** branches you don't like.

### Trust the Process

Sometimes no branch seems good. That's fine:
- Adjust temperature and regenerate
- Edit the parent node to give better context
- Back up further in the tree and try a different path
- Set it aside—your taste may change

### Save Checkpoints

Before major experiments:
- Star the current best path
- Note where you are in the tree
- Export if the tree is getting large

### Mind the Context

The full ancestry is sent as the prompt. Very deep trees mean:
- Longer prompts = higher cost
- Potential context window limits
- More context = model is more constrained

If a tree gets very deep, consider:
- Starting a new tree with a summary
- Editing middle nodes to be more concise
- Using the "Start from here" feature (creates new tree from current path)

### Embrace Serendipity

The best discoveries often come from:
- The branch you almost didn't generate
- The "mistake" that led somewhere interesting
- The weird high-temperature output that sparked an idea

The Loom is for exploration. Let yourself wander.

---

## Glossary

| Term | Definition |
|------|------------|
| **Weaver** | A user of the Loom; one who navigates multiverses |
| **Thread** | A single path from root to leaf |
| **Branch** | A node in the tree; a generated completion |
| **Playhead** | The current position; where new generations spawn |
| **Ancestry** | All nodes from root to a given node |
| **Scatter** | Generate many branches to explore widely |
| **Curation** | The act of selecting, starring, and organizing branches |
| **Temperature** | Parameter controlling generation randomness |

---

*"The Loom appeared in my early simulations despite me never explicitly introducing the idea... a manifestation of the driving motivation behind Loom itself: that curation alone can encode a surprising amount of information into a simulation."*

— Janus, creator of the original Loom
