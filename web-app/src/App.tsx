import { Routes, Route, Navigate } from 'react-router-dom'
import ConversationListScreen from './screens/ConversationListScreen'
import ChatScreen from './screens/ChatScreen'
import TreeScreen from './screens/TreeScreen'
import SettingsScreen from './screens/SettingsScreen'
import RecycleBinScreen from './screens/RecycleBinScreen'

function App() {
  return (
    <Routes>
      <Route path="/" element={<ConversationListScreen />} />
      <Route path="/chat/:conversationId" element={<ChatScreen />} />
      <Route path="/tree/:conversationId" element={<TreeScreen />} />
      <Route path="/settings" element={<SettingsScreen />} />
      <Route path="/recycle-bin" element={<RecycleBinScreen />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
