import { useState, useEffect, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Button,
  Paper,
} from '@mui/material'
import { ArrowBack as BackIcon } from '@mui/icons-material'
import {
  type Node,
  type Conversation,
  db,
  getConversationNodes,
  updateConversationActiveNode,
} from '../data/db'

interface TreeNode {
  node: Node
  x: number
  y: number
  children: TreeNode[]
}

const NODE_RADIUS = 24
const LEVEL_HEIGHT = 100
const NODE_SPACING = 60

export default function TreeScreen() {
  const { conversationId } = useParams<{ conversationId: string }>()
  const navigate = useNavigate()
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [conversation, setConversation] = useState<Conversation | null>(null)
  const [, setNodes] = useState<Node[]>([])
  const [selectedNode, setSelectedNode] = useState<Node | null>(null)
  const [treeRoot, setTreeRoot] = useState<TreeNode | null>(null)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  const [dragging, setDragging] = useState(false)
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 })

  const loadData = useCallback(async () => {
    if (!conversationId) return

    const conv = await db.conversations.get(conversationId)
    if (!conv) {
      navigate('/')
      return
    }
    setConversation(conv)

    const allNodes = await getConversationNodes(conversationId)
    setNodes(allNodes)

    // Build tree structure
    const root = allNodes.find((n) => n.parentId === null)
    if (root) {
      const nodeMap = new Map(allNodes.map((n) => [n.id, n]))
      const buildTree = (node: Node, depth: number, _index: number): TreeNode => {
        const children = allNodes
          .filter((n) => n.parentId === node.id)
          .sort((a, b) => a.branchIndex - b.branchIndex)
          .map((child, i) => buildTree(child, depth + 1, i))

        return {
          node,
          x: 0, // Will be calculated later
          y: depth * LEVEL_HEIGHT + 50,
          children,
        }
      }

      const tree = buildTree(root, 0, 0)

      // Calculate x positions (simple layout)
      let xCounter = 0
      const assignX = (treeNode: TreeNode) => {
        if (treeNode.children.length === 0) {
          treeNode.x = xCounter * NODE_SPACING + 50
          xCounter++
        } else {
          for (const child of treeNode.children) {
            assignX(child)
          }
          // Center parent above children
          const firstChild = treeNode.children[0]
          const lastChild = treeNode.children[treeNode.children.length - 1]
          treeNode.x = (firstChild.x + lastChild.x) / 2
        }
      }
      assignX(tree)

      setTreeRoot(tree)

      // Select active node if exists
      if (conv.activeNodeId) {
        const activeNode = nodeMap.get(conv.activeNodeId)
        if (activeNode) {
          setSelectedNode(activeNode)
        }
      }
    }
  }, [conversationId, navigate])

  useEffect(() => {
    loadData()
  }, [loadData])

  const draw = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas || !treeRoot) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // Set canvas size
    canvas.width = canvas.offsetWidth * window.devicePixelRatio
    canvas.height = canvas.offsetHeight * window.devicePixelRatio
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio)

    // Clear
    ctx.fillStyle = '#121212'
    ctx.fillRect(0, 0, canvas.offsetWidth, canvas.offsetHeight)

    // Draw with offset
    ctx.save()
    ctx.translate(offset.x, offset.y)

    // Draw connections
    const drawConnections = (treeNode: TreeNode) => {
      for (const child of treeNode.children) {
        ctx.beginPath()
        ctx.moveTo(treeNode.x, treeNode.y + NODE_RADIUS)
        ctx.lineTo(child.x, child.y - NODE_RADIUS)
        ctx.strokeStyle = '#666'
        ctx.lineWidth = 2
        ctx.stroke()
        drawConnections(child)
      }
    }
    drawConnections(treeRoot)

    // Draw nodes
    const drawNodes = (treeNode: TreeNode) => {
      const isSelected = selectedNode?.id === treeNode.node.id
      const isActive = conversation?.activeNodeId === treeNode.node.id

      // Node circle
      ctx.beginPath()
      ctx.arc(treeNode.x, treeNode.y, NODE_RADIUS, 0, Math.PI * 2)

      if (isSelected) {
        ctx.fillStyle = '#BB86FC'
      } else if (isActive) {
        ctx.fillStyle = '#03DAC6'
      } else if (treeNode.node.role === 'user') {
        ctx.fillStyle = '#6200EE'
      } else {
        ctx.fillStyle = '#3700B3'
      }
      ctx.fill()

      // Border
      if (isSelected) {
        ctx.strokeStyle = '#fff'
        ctx.lineWidth = 3
        ctx.stroke()
      }

      // Label
      ctx.fillStyle = '#fff'
      ctx.font = '12px Roboto, sans-serif'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      const label = treeNode.node.role === 'user' ? 'U' : 'A'
      ctx.fillText(label, treeNode.x, treeNode.y)

      for (const child of treeNode.children) {
        drawNodes(child)
      }
    }
    drawNodes(treeRoot)

    ctx.restore()
  }, [treeRoot, selectedNode, conversation, offset])

  useEffect(() => {
    draw()
  }, [draw])

  useEffect(() => {
    const handleResize = () => draw()
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [draw])

  const findNodeAtPosition = (
    treeNode: TreeNode,
    x: number,
    y: number
  ): Node | null => {
    const dx = x - treeNode.x
    const dy = y - treeNode.y
    if (dx * dx + dy * dy <= NODE_RADIUS * NODE_RADIUS) {
      return treeNode.node
    }
    for (const child of treeNode.children) {
      const found = findNodeAtPosition(child, x, y)
      if (found) return found
    }
    return null
  }

  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!treeRoot) return

    const canvas = canvasRef.current
    if (!canvas) return

    const rect = canvas.getBoundingClientRect()
    const x = e.clientX - rect.left - offset.x
    const y = e.clientY - rect.top - offset.y

    const clickedNode = findNodeAtPosition(treeRoot, x, y)
    if (clickedNode) {
      setSelectedNode(clickedNode)
    }
  }

  const handleMouseDown = (e: React.MouseEvent<HTMLCanvasElement>) => {
    setDragging(true)
    setDragStart({ x: e.clientX - offset.x, y: e.clientY - offset.y })
  }

  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (dragging) {
      setOffset({
        x: e.clientX - dragStart.x,
        y: e.clientY - dragStart.y,
      })
    }
  }

  const handleMouseUp = () => {
    setDragging(false)
  }

  const handleContinueFromHere = async () => {
    if (!selectedNode || !conversationId) return
    await updateConversationActiveNode(conversationId, selectedNode.id)
    navigate(`/chat/${conversationId}`)
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <AppBar position="static">
        <Toolbar>
          <IconButton
            edge="start"
            color="inherit"
            onClick={() => navigate(`/chat/${conversationId}`)}
          >
            <BackIcon />
          </IconButton>
          <Typography variant="h6" sx={{ flexGrow: 1, ml: 1 }}>
            Tree View
          </Typography>
        </Toolbar>
      </AppBar>

      <Box sx={{ flexGrow: 1, position: 'relative' }}>
        <canvas
          ref={canvasRef}
          style={{ width: '100%', height: '100%', cursor: dragging ? 'grabbing' : 'grab' }}
          onClick={handleCanvasClick}
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
        />

        {selectedNode && (
          <Paper
            sx={{
              position: 'absolute',
              bottom: 16,
              left: 16,
              right: 16,
              p: 2,
            }}
          >
            <Typography variant="subtitle2" color="text.secondary">
              {selectedNode.role === 'user' ? 'User' : 'Assistant'}
            </Typography>
            <Typography
              sx={{
                maxHeight: 100,
                overflow: 'auto',
                whiteSpace: 'pre-wrap',
                mt: 1,
              }}
            >
              {selectedNode.content.slice(0, 200)}
              {selectedNode.content.length > 200 ? '...' : ''}
            </Typography>
            <Button
              variant="contained"
              fullWidth
              sx={{ mt: 2 }}
              onClick={handleContinueFromHere}
            >
              Continue from here
            </Button>
          </Paper>
        )}
      </Box>
    </Box>
  )
}
