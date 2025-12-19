import { useState, useEffect } from 'react'
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
  Menu,
  MenuItem,
  Divider,
} from '@mui/material'
import {
  ArrowBack as BackIcon,
  MoreVert as MoreVertIcon,
  Restore as RestoreIcon,
  DeleteForever as DeleteForeverIcon,
} from '@mui/icons-material'
import {
  type Conversation,
  getDeletedConversations,
  restoreConversation,
  permanentlyDeleteConversation,
} from '../data/db'

export default function RecycleBinScreen() {
  const navigate = useNavigate()
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null)
  const [selectedConversation, setSelectedConversation] = useState<Conversation | null>(null)

  const loadConversations = async () => {
    const convs = await getDeletedConversations()
    setConversations(convs)
  }

  useEffect(() => {
    loadConversations()
  }, [])

  const handleOpenMenu = (event: React.MouseEvent<HTMLElement>, conv: Conversation) => {
    event.stopPropagation()
    setSelectedConversation(conv)
    setMenuAnchor(event.currentTarget)
  }

  const handleCloseMenu = () => {
    setMenuAnchor(null)
    setSelectedConversation(null)
  }

  const handleRestore = async () => {
    if (selectedConversation) {
      await restoreConversation(selectedConversation.id)
      await loadConversations()
    }
    handleCloseMenu()
  }

  const handlePermanentDelete = async () => {
    if (selectedConversation) {
      await permanentlyDeleteConversation(selectedConversation.id)
      await loadConversations()
    }
    handleCloseMenu()
  }

  const getDaysRemaining = (deletedAt: number | null) => {
    if (!deletedAt) return 0
    const sevenDays = 7 * 24 * 60 * 60 * 1000
    const remaining = deletedAt + sevenDays - Date.now()
    return Math.max(0, Math.ceil(remaining / (24 * 60 * 60 * 1000)))
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <AppBar position="static">
        <Toolbar>
          <IconButton edge="start" color="inherit" onClick={() => navigate('/')}>
            <BackIcon />
          </IconButton>
          <Typography variant="h6" sx={{ flexGrow: 1, ml: 1 }}>
            Recycle Bin
          </Typography>
        </Toolbar>
      </AppBar>

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
            <Typography>Recycle bin is empty</Typography>
          </Box>
        ) : (
          <>
            <Typography variant="body2" color="text.secondary" sx={{ p: 2 }}>
              Deleted conversations are automatically removed after 7 days.
            </Typography>
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
                  <ListItemButton>
                    <ListItemText
                      primary={conv.title}
                      secondary={`Deleted ${new Date(conv.deletedAt!).toLocaleDateString()} - ${getDaysRemaining(conv.deletedAt)} days remaining`}
                    />
                  </ListItemButton>
                </ListItem>
              ))}
            </List>
          </>
        )}
      </Box>

      <Menu anchorEl={menuAnchor} open={Boolean(menuAnchor)} onClose={handleCloseMenu}>
        <MenuItem onClick={handleRestore}>
          <RestoreIcon sx={{ mr: 1 }} /> Restore
        </MenuItem>
        <Divider />
        <MenuItem onClick={handlePermanentDelete} sx={{ color: 'error.main' }}>
          <DeleteForeverIcon sx={{ mr: 1 }} /> Delete Forever
        </MenuItem>
      </Menu>
    </Box>
  )
}
