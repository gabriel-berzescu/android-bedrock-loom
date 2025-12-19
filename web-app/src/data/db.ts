import Dexie, { type EntityTable } from 'dexie'

export type ChatRole = 'user' | 'assistant'

export interface Conversation {
  id: string
  title: string
  createdAt: number
  updatedAt: number
  activeNodeId: string | null
  deletedAt: number | null
}

export interface Node {
  id: string
  conversationId: string
  parentId: string | null
  role: ChatRole
  content: string
  thinkingContent: string
  createdAt: number
  branchIndex: number
}

export interface Settings {
  id: string
  credentials: string
  region: string
  modelId: string
  maxTokens: number
  temperature: number
  systemPrompt: string
  extendedThinkingEnabled: boolean
  thinkingBudget: number
}

class LoomDatabase extends Dexie {
  conversations!: EntityTable<Conversation, 'id'>
  nodes!: EntityTable<Node, 'id'>
  settings!: EntityTable<Settings, 'id'>

  constructor() {
    super('BedrocKLoomDB')
    this.version(1).stores({
      conversations: 'id, createdAt, updatedAt, deletedAt',
      nodes: 'id, conversationId, parentId, createdAt, branchIndex',
      settings: 'id',
    })
  }
}

export const db = new LoomDatabase()

// Default settings
export const DEFAULT_SETTINGS: Settings = {
  id: 'default',
  credentials: `[default]
aws_access_key_id=YOUR_ACCESS_KEY
aws_secret_access_key=YOUR_SECRET_KEY`,
  region: 'us-east-1',
  modelId: 'us.anthropic.claude-sonnet-4-20250514-v1:0',
  maxTokens: 4096,
  temperature: 1.0,
  systemPrompt: 'You are Claude, a helpful AI assistant.',
  extendedThinkingEnabled: false,
  thinkingBudget: 4096,
}

export async function getSettings(): Promise<Settings> {
  const settings = await db.settings.get('default')
  if (!settings) {
    await db.settings.put(DEFAULT_SETTINGS)
    return DEFAULT_SETTINGS
  }
  return settings
}

export async function saveSettings(settings: Partial<Settings>): Promise<void> {
  const current = await getSettings()
  await db.settings.put({ ...current, ...settings, id: 'default' })
}

// Conversation helpers
export async function createConversation(title: string): Promise<Conversation> {
  const now = Date.now()
  const conversation: Conversation = {
    id: crypto.randomUUID(),
    title,
    createdAt: now,
    updatedAt: now,
    activeNodeId: null,
    deletedAt: null,
  }
  await db.conversations.put(conversation)
  return conversation
}

export async function getActiveConversations(): Promise<Conversation[]> {
  return db.conversations
    .filter((c) => c.deletedAt === null)
    .reverse()
    .sortBy('updatedAt')
}

export async function getDeletedConversations(): Promise<Conversation[]> {
  return db.conversations
    .filter((c) => c.deletedAt !== null)
    .reverse()
    .sortBy('deletedAt')
}

export async function softDeleteConversation(id: string): Promise<void> {
  await db.conversations.update(id, { deletedAt: Date.now() })
}

export async function restoreConversation(id: string): Promise<void> {
  await db.conversations.update(id, { deletedAt: null })
}

export async function permanentlyDeleteConversation(id: string): Promise<void> {
  await db.transaction('rw', [db.conversations, db.nodes], async () => {
    await db.nodes.where('conversationId').equals(id).delete()
    await db.conversations.delete(id)
  })
}

export async function renameConversation(id: string, title: string): Promise<void> {
  await db.conversations.update(id, { title, updatedAt: Date.now() })
}

export async function updateConversationActiveNode(id: string, activeNodeId: string): Promise<void> {
  await db.conversations.update(id, { activeNodeId, updatedAt: Date.now() })
}

// Node helpers
export async function createNode(
  conversationId: string,
  parentId: string | null,
  role: ChatRole,
  content: string,
  thinkingContent: string = ''
): Promise<Node> {
  // Get max branch index for this parent
  let branchIndex = 0
  if (parentId) {
    const siblings = await db.nodes
      .where('parentId')
      .equals(parentId)
      .toArray()
    if (siblings.length > 0) {
      branchIndex = Math.max(...siblings.map((s) => s.branchIndex)) + 1
    }
  }

  const node: Node = {
    id: crypto.randomUUID(),
    conversationId,
    parentId,
    role,
    content,
    thinkingContent,
    createdAt: Date.now(),
    branchIndex,
  }
  await db.nodes.put(node)
  return node
}

export async function getConversationNodes(conversationId: string): Promise<Node[]> {
  return db.nodes.where('conversationId').equals(conversationId).toArray()
}

export async function getChildNodes(parentId: string): Promise<Node[]> {
  return db.nodes
    .where('parentId')
    .equals(parentId)
    .sortBy('branchIndex')
}

export async function updateNodeContent(id: string, content: string): Promise<void> {
  await db.nodes.update(id, { content })
}

export async function updateNodeThinking(id: string, thinkingContent: string): Promise<void> {
  await db.nodes.update(id, { thinkingContent })
}

// Build linear path from root to a specific node or to the deepest leaf
export async function buildLinearPath(
  conversationId: string,
  targetNodeId: string | null
): Promise<Node[]> {
  const allNodes = await getConversationNodes(conversationId)
  if (allNodes.length === 0) return []

  const nodeMap = new Map(allNodes.map((n) => [n.id, n]))

  // Find root node (no parent)
  const rootNode = allNodes.find((n) => n.parentId === null)
  if (!rootNode) return []

  if (targetNodeId) {
    // Trace back from target to root
    const path: Node[] = []
    let currentId: string | null = targetNodeId
    while (currentId) {
      const node = nodeMap.get(currentId)
      if (!node) break
      path.unshift(node)
      currentId = node.parentId
    }
    return path
  } else {
    // Follow first children to deepest leaf
    const path: Node[] = [rootNode]
    let current = rootNode
    while (true) {
      const children = allNodes
        .filter((n) => n.parentId === current.id)
        .sort((a, b) => a.branchIndex - b.branchIndex)
      if (children.length === 0) break
      current = children[0]
      path.push(current)
    }
    return path
  }
}

// Export conversation as JSON
export async function exportConversation(conversationId: string): Promise<string> {
  const conversation = await db.conversations.get(conversationId)
  const nodes = await getConversationNodes(conversationId)
  return JSON.stringify({ conversation, nodes }, null, 2)
}

// Import conversation from JSON
export async function importConversation(json: string): Promise<Conversation | null> {
  try {
    const data = JSON.parse(json)
    const { conversation, nodes } = data as { conversation: Conversation; nodes: Node[] }

    // Generate new IDs
    const oldToNewId = new Map<string, string>()
    const newConversationId = crypto.randomUUID()
    oldToNewId.set(conversation.id, newConversationId)

    for (const node of nodes) {
      oldToNewId.set(node.id, crypto.randomUUID())
    }

    const newConversation: Conversation = {
      ...conversation,
      id: newConversationId,
      createdAt: Date.now(),
      updatedAt: Date.now(),
      activeNodeId: conversation.activeNodeId ? oldToNewId.get(conversation.activeNodeId) || null : null,
      deletedAt: null,
    }

    const newNodes: Node[] = nodes.map((node) => ({
      ...node,
      id: oldToNewId.get(node.id)!,
      conversationId: newConversationId,
      parentId: node.parentId ? oldToNewId.get(node.parentId) || null : null,
    }))

    await db.transaction('rw', [db.conversations, db.nodes], async () => {
      await db.conversations.put(newConversation)
      await db.nodes.bulkPut(newNodes)
    })

    return newConversation
  } catch {
    return null
  }
}

// Cleanup old deleted conversations (older than 7 days)
export async function cleanupOldDeleted(): Promise<void> {
  const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
  const oldDeleted = await db.conversations
    .filter((c) => c.deletedAt !== null && c.deletedAt < sevenDaysAgo)
    .toArray()

  for (const conv of oldDeleted) {
    await permanentlyDeleteConversation(conv.id)
  }
}
