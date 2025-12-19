import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  TextField,
  Slider,
  Switch,
  FormControlLabel,
  Button,
  Alert,
  Paper,
} from '@mui/material'
import { ArrowBack as BackIcon } from '@mui/icons-material'
import { getSettings, saveSettings, type Settings, DEFAULT_SETTINGS } from '../data/db'

export default function SettingsScreen() {
  const navigate = useNavigate()
  const [settings, setSettings] = useState<Settings>(DEFAULT_SETTINGS)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const load = async () => {
      const s = await getSettings()
      setSettings(s)
    }
    load()
  }, [])

  const handleSave = async () => {
    setError('')
    setSaved(false)

    // Validate credentials format
    if (
      !settings.credentials.includes('aws_access_key_id') ||
      !settings.credentials.includes('aws_secret_access_key')
    ) {
      setError('Invalid credentials format. Please use INI format with aws_access_key_id and aws_secret_access_key.')
      return
    }

    if (!settings.region.trim()) {
      setError('Region is required.')
      return
    }

    if (!settings.modelId.trim()) {
      setError('Model ID is required.')
      return
    }

    if (settings.maxTokens < 1) {
      setError('Max tokens must be at least 1.')
      return
    }

    await saveSettings(settings)
    setSaved(true)
    setTimeout(() => setSaved(false), 3000)
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <AppBar position="static">
        <Toolbar>
          <IconButton edge="start" color="inherit" onClick={() => navigate('/')}>
            <BackIcon />
          </IconButton>
          <Typography variant="h6" sx={{ flexGrow: 1, ml: 1 }}>
            Settings
          </Typography>
        </Toolbar>
      </AppBar>

      <Box sx={{ flexGrow: 1, overflow: 'auto', p: 2 }}>
        <Paper sx={{ p: 2, mb: 2 }}>
          <Typography variant="h6" gutterBottom>
            AWS Credentials
          </Typography>
          <TextField
            fullWidth
            multiline
            rows={6}
            value={settings.credentials}
            onChange={(e) => setSettings({ ...settings, credentials: e.target.value })}
            placeholder={`[default]
aws_access_key_id=YOUR_ACCESS_KEY
aws_secret_access_key=YOUR_SECRET_KEY
aws_session_token=OPTIONAL_SESSION_TOKEN`}
            variant="outlined"
            sx={{ fontFamily: 'monospace' }}
          />
        </Paper>

        <Paper sx={{ p: 2, mb: 2 }}>
          <Typography variant="h6" gutterBottom>
            AWS Configuration
          </Typography>
          <TextField
            fullWidth
            label="Region"
            value={settings.region}
            onChange={(e) => setSettings({ ...settings, region: e.target.value })}
            variant="outlined"
            sx={{ mb: 2 }}
            helperText="e.g., us-east-1, eu-west-1"
          />
          <TextField
            fullWidth
            label="Model ID"
            value={settings.modelId}
            onChange={(e) => setSettings({ ...settings, modelId: e.target.value })}
            variant="outlined"
            helperText="e.g., us.anthropic.claude-sonnet-4-20250514-v1:0"
          />
        </Paper>

        <Paper sx={{ p: 2, mb: 2 }}>
          <Typography variant="h6" gutterBottom>
            Inference Configuration
          </Typography>

          <Typography gutterBottom>
            Temperature: {settings.temperature.toFixed(2)}
          </Typography>
          <Slider
            value={settings.temperature}
            onChange={(_, value) => setSettings({ ...settings, temperature: value as number })}
            min={0}
            max={1}
            step={0.01}
            valueLabelDisplay="auto"
            sx={{ mb: 3 }}
            disabled={settings.extendedThinkingEnabled}
          />
          {settings.extendedThinkingEnabled && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
              Temperature is fixed at 1.0 when extended thinking is enabled.
            </Typography>
          )}

          <TextField
            fullWidth
            type="number"
            label="Max Tokens"
            value={settings.maxTokens}
            onChange={(e) => setSettings({ ...settings, maxTokens: parseInt(e.target.value) || 256 })}
            variant="outlined"
            sx={{ mb: 2 }}
            helperText="Maximum number of tokens in the response"
          />

          <TextField
            fullWidth
            multiline
            rows={4}
            label="System Prompt"
            value={settings.systemPrompt}
            onChange={(e) => setSettings({ ...settings, systemPrompt: e.target.value })}
            variant="outlined"
            helperText="Instructions for the AI assistant"
          />
        </Paper>

        <Paper sx={{ p: 2, mb: 2 }}>
          <Typography variant="h6" gutterBottom>
            Extended Thinking
          </Typography>
          <FormControlLabel
            control={
              <Switch
                checked={settings.extendedThinkingEnabled}
                onChange={(e) =>
                  setSettings({ ...settings, extendedThinkingEnabled: e.target.checked })
                }
              />
            }
            label="Enable Extended Thinking"
          />
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            When enabled, Claude will show its reasoning process before responding.
          </Typography>

          {settings.extendedThinkingEnabled && (
            <TextField
              fullWidth
              type="number"
              label="Thinking Budget (tokens)"
              value={settings.thinkingBudget}
              onChange={(e) =>
                setSettings({ ...settings, thinkingBudget: parseInt(e.target.value) || 4096 })
              }
              variant="outlined"
              helperText="Maximum tokens for thinking process"
            />
          )}
        </Paper>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {saved && (
          <Alert severity="success" sx={{ mb: 2 }}>
            Settings saved successfully!
          </Alert>
        )}

        <Button variant="contained" fullWidth size="large" onClick={handleSave}>
          Save Settings
        </Button>
      </Box>
    </Box>
  )
}
