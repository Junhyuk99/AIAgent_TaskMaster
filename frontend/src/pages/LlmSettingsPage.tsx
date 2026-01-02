import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { llmService } from '../services/llmService';
import type { LlmServer, LlmServerRequest, LlmType, ConnectionTestResponse, ModelInfo } from '../services/llmService';

interface ServerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: LlmServerRequest) => Promise<void>;
  server?: LlmServer | null;
  isLoading: boolean;
}

function ServerModal({ isOpen, onClose, onSubmit, server, isLoading }: ServerModalProps) {
  const { t } = useTranslation();
  const getInitialFormData = (): LlmServerRequest => ({
    name: server?.name ?? '',
    type: server?.type ?? 'OLLAMA',
    baseUrl: server?.baseUrl ?? '',
    apiKey: '',
  });

  const [formData, setFormData] = useState<LlmServerRequest>(getInitialFormData);

  // Reset form when modal opens/closes or server changes
  const formKey = `${isOpen}-${server?.id ?? 'new'}`;
  const [prevFormKey, setPrevFormKey] = useState(formKey);

  if (formKey !== prevFormKey) {
    setPrevFormKey(formKey);
    setFormData(getInitialFormData());
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSubmit(formData);
  };

  const getPlaceholderUrl = (type: LlmType) => {
    switch (type) {
      case 'OLLAMA':
        return 'http://localhost:11434';
      case 'VLLM':
        return 'http://localhost:8000';
      case 'OPENAI_COMPATIBLE':
        return 'https://api.openai.com';
      case 'CUSTOM':
        return 'http://localhost:8080';
      default:
        return '';
    }
  };

  const getTypeDescription = (type: LlmType) => {
    switch (type) {
      case 'OLLAMA':
        return t('llm.typeDescOllama');
      case 'VLLM':
        return t('llm.typeDescVllm');
      case 'OPENAI_COMPATIBLE':
        return t('llm.typeDescOpenai');
      case 'CUSTOM':
        return t('llm.typeDescCustom');
      default:
        return '';
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-md mx-4">
        <div className="p-6">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">
            {server ? t('llm.editServer') : t('llm.addServer')}
          </h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {t('llm.serverName')}
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
                placeholder="My LLM Server"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {t('llm.serverType')}
              </label>
              <select
                value={formData.type}
                onChange={(e) => setFormData({ ...formData, type: e.target.value as LlmType })}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
              >
                <option value="OLLAMA">Ollama</option>
                <option value="VLLM">vLLM</option>
                <option value="OPENAI_COMPATIBLE">OpenAI Compatible</option>
                <option value="CUSTOM">Custom API</option>
              </select>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {getTypeDescription(formData.type)}
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {t('llm.baseUrl')}
              </label>
              <input
                type="url"
                value={formData.baseUrl}
                onChange={(e) => setFormData({ ...formData, baseUrl: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
                placeholder={getPlaceholderUrl(formData.type)}
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {t('llm.apiKeyOptional')}
              </label>
              <input
                type="password"
                value={formData.apiKey}
                onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
                placeholder={t('llm.apiKey')}
              />
            </div>

            <div className="flex justify-end gap-3 pt-4">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                disabled={isLoading}
              >
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50"
                disabled={isLoading}
              >
                {isLoading ? t('llm.saving') : server ? t('llm.update') : t('llm.addServer')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

interface ModelsModalProps {
  isOpen: boolean;
  onClose: () => void;
  server: LlmServer | null;
  models: ModelInfo[];
  isLoading: boolean;
}

function ModelsModal({ isOpen, onClose, server, models, isLoading }: ModelsModalProps) {
  const { t } = useTranslation();
  if (!isOpen || !server) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-lg mx-4 max-h-[80vh] flex flex-col">
        <div className="p-6 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
            {t('llm.availableModels')} - {server.name}
          </h2>
        </div>
        <div className="p-6 overflow-y-auto flex-1">
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : models.length === 0 ? (
            <p className="text-center text-gray-500 dark:text-gray-400 py-8">
              {t('llm.noModels')}
            </p>
          ) : (
            <ul className="space-y-2">
              {models.map((model) => (
                <li
                  key={model.name}
                  className="p-3 bg-gray-50 dark:bg-gray-700 rounded-lg"
                >
                  <div className="font-medium text-gray-900 dark:text-white">
                    {model.displayName || model.name}
                  </div>
                  {model.size && (
                    <div className="text-sm text-gray-500 dark:text-gray-400">
                      {t('llm.size')}: {(model.size / 1024 / 1024 / 1024).toFixed(1)} GB
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="p-6 border-t border-gray-200 dark:border-gray-700">
          <button
            onClick={onClose}
            className="w-full px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
          >
            {t('common.close')}
          </button>
        </div>
      </div>
    </div>
  );
}

function ServerCard({
  server,
  onEdit,
  onDelete,
  onToggleActive,
  onViewModels,
}: {
  server: LlmServer;
  onEdit: () => void;
  onDelete: () => void;
  onToggleActive: () => void;
  onViewModels: () => void;
}) {
  const { t } = useTranslation();
  const [testResult, setTestResult] = useState<ConnectionTestResponse | null>(null);
  const [isTesting, setIsTesting] = useState(false);

  const handleTest = async () => {
    setIsTesting(true);
    setTestResult(null);
    try {
      const result = await llmService.testConnection(server.id);
      setTestResult(result);
    } catch {
      setTestResult({
        success: false,
        message: 'Failed to test connection',
        responseTimeMs: 0,
      });
    } finally {
      setIsTesting(false);
    }
  };

  const getTypeLabel = (type: LlmType) => {
    switch (type) {
      case 'OLLAMA':
        return 'Ollama';
      case 'VLLM':
        return 'vLLM';
      case 'OPENAI_COMPATIBLE':
        return 'OpenAI Compatible';
      case 'CUSTOM':
        return 'Custom API';
      default:
        return type;
    }
  };

  const getTypeColor = (type: LlmType) => {
    switch (type) {
      case 'OLLAMA':
        return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      case 'VLLM':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
      case 'OPENAI_COMPATIBLE':
        return 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-300';
      case 'CUSTOM':
        return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
      <div className="flex items-start justify-between mb-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              {server.name}
            </h3>
            <span
              className={`px-2 py-0.5 text-xs font-medium rounded-full ${getTypeColor(server.type)}`}
            >
              {getTypeLabel(server.type)}
            </span>
          </div>
          <p className="text-sm text-gray-500 dark:text-gray-400 font-mono">
            {server.baseUrl}
          </p>
        </div>
        <button
          onClick={onToggleActive}
          className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
            server.isActive ? 'bg-primary-600' : 'bg-gray-300 dark:bg-gray-600'
          }`}
        >
          <span
            className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
              server.isActive ? 'translate-x-6' : 'translate-x-1'
            }`}
          />
        </button>
      </div>

      {testResult && (
        <div
          className={`mb-4 p-3 rounded-lg ${
            testResult.success
              ? 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300'
              : 'bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300'
          }`}
        >
          <div className="flex items-center gap-2">
            {testResult.success ? (
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                  clipRule="evenodd"
                />
              </svg>
            ) : (
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                  clipRule="evenodd"
                />
              </svg>
            )}
            <span className="text-sm font-medium">{testResult.message}</span>
            {testResult.success && (
              <span className="text-sm">({testResult.responseTimeMs}ms)</span>
            )}
          </div>
        </div>
      )}

      <div className="flex flex-wrap gap-2">
        <button
          onClick={handleTest}
          disabled={isTesting}
          className="px-3 py-1.5 text-sm bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors disabled:opacity-50"
        >
          {isTesting ? t('llm.testing') : t('llm.testConnection')}
        </button>
        <button
          onClick={onViewModels}
          className="px-3 py-1.5 text-sm bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
        >
          {t('llm.viewModels')}
        </button>
        <button
          onClick={onEdit}
          className="px-3 py-1.5 text-sm bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 rounded-lg hover:bg-primary-200 dark:hover:bg-primary-900/50 transition-colors"
        >
          {t('common.edit')}
        </button>
        <button
          onClick={onDelete}
          className="px-3 py-1.5 text-sm bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 rounded-lg hover:bg-red-200 dark:hover:bg-red-900/50 transition-colors"
        >
          {t('common.delete')}
        </button>
      </div>
    </div>
  );
}

export default function LlmSettingsPage() {
  const { t } = useTranslation();
  const [servers, setServers] = useState<LlmServer[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isModelsModalOpen, setIsModelsModalOpen] = useState(false);
  const [editingServer, setEditingServer] = useState<LlmServer | null>(null);
  const [selectedServer, setSelectedServer] = useState<LlmServer | null>(null);
  const [models, setModels] = useState<ModelInfo[]>([]);
  const [isLoadingModels, setIsLoadingModels] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchServers = async () => {
    try {
      setError(null);
      const data = await llmService.getAllServers();
      setServers(data);
    } catch (err) {
      setError('Failed to load LLM servers');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchServers();
  }, []);

  const handleAddServer = () => {
    setEditingServer(null);
    setIsModalOpen(true);
  };

  const handleEditServer = (server: LlmServer) => {
    setEditingServer(server);
    setIsModalOpen(true);
  };

  const handleSubmit = async (data: LlmServerRequest) => {
    setIsSaving(true);
    try {
      if (editingServer) {
        await llmService.updateServer(editingServer.id, data);
      } else {
        await llmService.createServer(data);
      }
      await fetchServers();
      setIsModalOpen(false);
    } catch (err) {
      console.error(err);
      setError('Failed to save server');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteServer = async (server: LlmServer) => {
    if (!confirm(`Are you sure you want to delete "${server.name}"?`)) {
      return;
    }
    try {
      await llmService.deleteServer(server.id);
      await fetchServers();
    } catch (err) {
      console.error(err);
      setError('Failed to delete server');
    }
  };

  const handleToggleActive = async (server: LlmServer) => {
    try {
      await llmService.toggleActive(server.id);
      await fetchServers();
    } catch (err) {
      console.error(err);
      setError('Failed to toggle server status');
    }
  };

  const handleViewModels = async (server: LlmServer) => {
    setSelectedServer(server);
    setIsModelsModalOpen(true);
    setIsLoadingModels(true);
    try {
      const modelList = await llmService.getModels(server.id);
      setModels(modelList);
    } catch (err) {
      console.error(err);
      setModels([]);
    } finally {
      setIsLoadingModels(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('llm.title')}</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            {t('llm.subtitle')}
          </p>
        </div>
        <button
          onClick={handleAddServer}
          className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors flex items-center gap-2"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          {t('llm.addServer')}
        </button>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 rounded-lg">
          {error}
        </div>
      )}

      {servers.length === 0 ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow">
          <div className="p-12 text-center">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gray-100 dark:bg-gray-700 mb-4">
              <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
              </svg>
            </div>
            <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
              {t('llm.noServers')}
            </h3>
            <p className="text-gray-500 dark:text-gray-400 mb-6 max-w-md mx-auto">
              {t('llm.noServersDesc')}
            </p>
            <button
              onClick={handleAddServer}
              className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
            >
              {t('llm.addFirst')}
            </button>
          </div>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {servers.map((server) => (
            <ServerCard
              key={server.id}
              server={server}
              onEdit={() => handleEditServer(server)}
              onDelete={() => handleDeleteServer(server)}
              onToggleActive={() => handleToggleActive(server)}
              onViewModels={() => handleViewModels(server)}
            />
          ))}
        </div>
      )}

      <ServerModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleSubmit}
        server={editingServer}
        isLoading={isSaving}
      />

      <ModelsModal
        isOpen={isModelsModalOpen}
        onClose={() => setIsModelsModalOpen(false)}
        server={selectedServer}
        models={models}
        isLoading={isLoadingModels}
      />
    </div>
  );
}
