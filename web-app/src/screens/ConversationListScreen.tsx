import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Fab,
  Menu,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Divider,
} from '@mui/material'
import {
  Add as AddIcon,
  Settings as SettingsIcon,
  Delete as DeleteIcon,
  MoreVert as MoreVertIcon,
  FileUpload as ImportIcon,
  FileDownload as ExportIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import {
  type Conversation,
  getActiveConversations,
  createConversation,
  softDeleteConversation,
  renameConversation,
  exportConversation,
  importConversation,
  cleanupOldDeleted,
} from '../data/db'

export default function ConversationListScreen() {
  const navigate = useNavigate()
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null)
  const [selectedConversation, setSelectedConversation] = useState<Conversation | null>(null)
  const [renameDialogOpen, setRenameDialogOpen] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  const loadConversations = async () => {
    await cleanupOldDeleted()
    const convs = await getActiveConversations()
    setConversations(convs)
  }

  useEffect(() => {
    loadConversations()
  }, [])

  const handleNewConversation = async () => {
    const conv = await createConversation('New Conversation')
    navigate(`/chat/${conv.id}`)
  }

  const handleOpenMenu = (event: React.MouseEvent<HTMLElement>, conv: Conversation) => {
    event.stopPropagation()
    setSelectedConversation(conv)
    setMenuAnchor(event.currentTarget)
  }

  const handleCloseMenu = () => {
    setMenuAnchor(null)
    setSelectedConversation(null)
  }

  const handleDelete = async () => {
    if (selectedConversation) {
      await softDeleteConversation(selectedConversation.id)
      await loadConversations()
    }
    handleCloseMenu()
  }

  const handleRenameOpen = () => {
    if (selectedConversation) {
      setNewTitle(selectedConversation.title)
      setRenameDialogOpen(true)
    }
    setMenuAnchor(null)
  }

  const handleRenameClose = () => {
    setRenameDialogOpen(false)
    setNewTitle('')
    setSelectedConversation(null)
  }

  const handleRenameSave = async () => {
    if (selectedConversation && newTitle.trim()) {
      await renameConversation(selectedConversation.id, newTitle.trim())
      await loadConversations()
    }
    handleRenameClose()
  }

  const handleExport = async () => {
    if (selectedConversation) {
      const json = await exportConversation(selectedConversation.id)
      const blob = new Blob([json], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${selectedConversation.title}.json`
      a.click()
      URL.revokeObjectURL(url)
    }
    handleCloseMenu()
  }

  const handleImportClick = () => {
    fileInputRef.current?.click()
  }

  const handleImportFile = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file) {
      const text = await file.text()
      const conv = await importConversation(text)
      if (conv) {
        await loadConversations()
      }
    }
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Bedrock Loom
          </Typography>
          <IconButton color="inherit" onClick={handleImportClick}>
            <ImportIcon />
          </IconButton>
          <IconButton color="inherit" onClick={loadConversations}>
            <RefreshIcon />
          </IconButton>
          <IconButton color="inherit" onClick={() => navigate('/recycle-bin')}>
            <DeleteIcon />
          </IconButton>
          <IconButton color="inherit" onClick={() => navigate('/settings')}>
            <SettingsIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <input
        type="file"
        ref={fileInputRef}
        style={{ display: 'none' }}
        accept=".json"
        onChange={handleImportFile}
      />

      <Box sx={{ flexGrow: 1, overflow: 'auto' }}>
        {conversations.length === 0 ? (
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              height: '100%',
              color: 'text.secondary',
            }}
          >
            <Typography>No conversations yet. Tap + to start one!</Typography>
          </Box>
        ) : (
          <List>
            {conversations.map((conv) => (
              <ListItem
                key={conv.id}
                disablePadding
                secondaryAction={
                  <IconButton edge="end" onClick={(e) => handleOpenMenu(e, conv)}>
                    <MoreVertIcon />
                  </IconButton>
                }
              >
                <ListItemButton onClick={() => navigate(`/chat/${conv.id}`)}>
                  <ListItemText
                    primary={conv.title}
                    secondary={new Date(conv.updatedAt).toLocaleString()}
                  />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        )}
      </Box>

      <Fab
        color="primary"
        sx={{ position: 'fixed', bottom: 24, right: 24 }}
        onClick={handleNewConversation}
      >
        <AddIcon />
      </Fab>

      <Menu anchorEl={menuAnchor} open={Boolean(menuAnchor)} onClose={handleCloseMenu}>
        <MenuItem onClick={handleRenameOpen}>Rename</MenuItem>
        <MenuItem onClick={handleExport}>
          <ExportIcon sx={{ mr: 1 }} /> Export
        </MenuItem>
        <Divider />
        <MenuItem onClick={handleDelete} sx={{ color: 'error.main' }}>
          <DeleteIcon sx={{ mr: 1 }} /> Delete
        </MenuItem>
      </Menu>

      <Dialog open={renameDialogOpen} onClose={handleRenameClose} maxWidth="sm" fullWidth>
        <DialogTitle>Rename Conversation</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            label="Title"
            variant="outlined"
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleRenameClose}>Cancel</Button>
          <Button onClick={handleRenameSave} variant="contained">
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
