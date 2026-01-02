import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ErrorBoundary } from './components/ui';
import { ProtectedRoute } from './components/auth';
import { MainLayout } from './components/layout';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import OAuth2CallbackPage from './pages/OAuth2CallbackPage';
import DashboardPage from './pages/DashboardPage';
import AgentsPage from './pages/AgentsPage';
import AgentEditPage from './pages/AgentEditPage';
import EmbedWidgetPage from './pages/EmbedWidgetPage';
import FunctionsPage from './pages/FunctionsPage';
import KnowledgePage from './pages/KnowledgePage';
import KnowledgeBaseDetailPage from './pages/KnowledgeBaseDetailPage';
import LlmSettingsPage from './pages/LlmSettingsPage';
import UsageDashboardPage from './pages/UsageDashboardPage';

function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          {/* Public routes */}
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />

          {/* Protected routes with layout */}
          <Route
            element={
              <ProtectedRoute>
                <MainLayout />
              </ProtectedRoute>
            }
          >
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/agents" element={<AgentsPage />} />
            <Route path="/agents/:id/edit" element={<AgentEditPage />} />
            <Route path="/agents/:agentId/embed" element={<EmbedWidgetPage />} />
            <Route path="/functions" element={<FunctionsPage />} />
            <Route path="/knowledge" element={<KnowledgePage />} />
            <Route path="/knowledge/:id" element={<KnowledgeBaseDetailPage />} />
            <Route path="/settings/llm" element={<LlmSettingsPage />} />
            <Route path="/usage" element={<UsageDashboardPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;
