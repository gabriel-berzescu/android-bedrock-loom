# Bedrock Loom Web

A web clone of the Android Bedrock Loom app - a "multiverse" conversation interface for AWS Bedrock Claude models.

## Features

- **Loom/Multiverse Navigation** - Branch conversations at any point and explore multiple paths
- **Real-time Streaming** - Live text updates as Claude responds
- **Tree Visualization** - Canvas-based view of your conversation branches with pan/drag
- **Markdown Rendering** - Full markdown support with syntax highlighting for code
- **Message Actions** - Copy, edit messages, regenerate responses
- **Extended Thinking** - Toggle Claude's reasoning process visibility
- **Conversation Management** - Create, rename, delete, import/export conversations
- **Recycle Bin** - Soft delete with 7-day retention before permanent deletion
- **Local Storage** - All data stored in IndexedDB (browser)

## Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Start development server:
   ```bash
   npm run dev
   ```

3. Open http://localhost:5173

4. Go to **Settings** and configure:
   - AWS credentials (INI format)
   - AWS region (e.g., `us-east-1`)
   - Model ID (e.g., `us.anthropic.claude-sonnet-4-20250514-v1:0`)
   - Max tokens, temperature, system prompt

## AWS Credentials Format

```ini
[default]
aws_access_key_id=YOUR_ACCESS_KEY
aws_secret_access_key=YOUR_SECRET_KEY
aws_session_token=OPTIONAL_SESSION_TOKEN
```

## Tech Stack

- React 18 + TypeScript
- Vite
- Material UI (MUI)
- Dexie (IndexedDB wrapper)
- AWS SDK for JavaScript (Bedrock Runtime)
- React Markdown + Syntax Highlighter

## Build

```bash
npm run build
```

Output will be in `dist/`.
