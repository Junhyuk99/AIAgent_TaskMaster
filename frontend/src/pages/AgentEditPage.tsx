import { useState, useEffect } from 'react';
import { useParams, Link, useSearchParams } from 'react-router-dom';
import { useAgentStore } from '../stores/agentStore';
import { ChatInterface } from '../components/chat';
import { LoadingSpinner } from '../components/ui';
import functionService from '../services/functionService';
import type { Function } from '../services/functionService';
import { agentService } from '../services/agentService';
import type { FunctionSummary, KnowledgeBaseSummary } from '../services/agentService';
import { knowledgeService } from '../services/knowledgeService';
import type { KnowledgeBase } from '../services/knowledgeService';

type TabType = 'settings' | 'functions' | 'knowledge' | 'test';

const tabs: { id: TabType; label: string }[] = [
  { id: 'settings', label: 'Settings' },
  { id: 'functions', label: 'Functions' },
  { id: 'knowledge', label: 'Knowledge' },
  { id: 'test', label: 'Test' },
];

interface FunctionTabProps {
  agentId: number;
  connectedFunctions: FunctionSummary[];
  onUpdate: () => void;
}

function FunctionsTab({ agentId, connectedFunctions, onUpdate }: FunctionTabProps) {
  const [allFunctions, setAllFunctions] = useState<Function[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadFunctions();
  }, []);

  useEffect(() => {
    setSelectedIds(new Set(connectedFunctions.map(f => f.id)));
  }, [connectedFunctions]);

  const loadFunctions = async () => {
    try {
      setIsLoading(true);
      const functions = await functionService.getAll();
      setAllFunctions(functions);
    } catch {
      setError('Failed to load functions');
    } finally {
      setIsLoading(false);
    }
  };

  const handleToggle = (id: number) => {
    setSelectedIds(prev => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  };

  const handleSave = async () => {
    setIsSaving(true);
    setError(null);
    try {
      await agentService.updateFunctions(agentId, Array.from(selectedIds));
      onUpdate();
    } catch {
      setError('Failed to update functions');
    } finally {
      setIsSaving(false);
    }
  };

  const hasChanges = () => {
    const currentIds = new Set(connectedFunctions.map(f => f.id));
    if (currentIds.size !== selectedIds.size) return true;
    for (const id of selectedIds) {
      if (!currentIds.has(id)) return true;
    }
    return false;
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-8">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
      <div className="flex justify-between items-center mb-4">
        <div>
          <h3 className="text-lg font-medium text-gray-900 dark:text-white">
            Connected Functions
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Select functions this agent can call during conversations
          </p>
        </div>
        {hasChanges() && (
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {isSaving ? 'Saving...' : 'Save Changes'}
          </button>
        )}
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 rounded-lg">
          {error}
        </div>
      )}

      {allFunctions.length === 0 ? (
        <div className="text-center py-8 text-gray-500 dark:text-gray-400">
          <div className="text-4xl mb-2">⚡</div>
          <p className="mb-4">No functions available</p>
          <Link
            to="/functions"
            className="text-primary-600 hover:text-primary-700"
          >
            Create your first function
          </Link>
        </div>
      ) : (
        <div className="space-y-2">
          {allFunctions.map((func) => (
            <div
              key={func.id}
              onClick={() => handleToggle(func.id)}
              className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                selectedIds.has(func.id)
                  ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                  : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
              }`}
            >
              <div className="flex items-start">
                <div className="flex-shrink-0">
                  <div
                    className={`w-5 h-5 rounded border-2 flex items-center justify-center ${
                      selectedIds.has(func.id)
                        ? 'border-primary-600 bg-primary-600'
                        : 'border-gray-300 dark:border-gray-600'
                    }`}
                  >
                    {selectedIds.has(func.id) && (
                      <svg className="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                        <path
                          fillRule="evenodd"
                          d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                          clipRule="evenodd"
                        />
                      </svg>
                    )}
                  </div>
                </div>
                <div className="ml-3 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-900 dark:text-white">
                      {func.name}
                    </span>
                    <span
                      className={`px-2 py-0.5 text-xs rounded-full ${
                        func.implementationType === 'HTTP_API'
                          ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300'
                          : func.implementationType === 'CODE'
                          ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
                          : 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300'
                      }`}
                    >
                      {func.implementationType}
                    </span>
                    {!func.isActive && (
                      <span className="px-2 py-0.5 text-xs rounded-full bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400">
                        Inactive
                      </span>
                    )}
                  </div>
                  {func.description && (
                    <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                      {func.description}
                    </p>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

interface KnowledgeTabProps {
  agentId: number;
  connectedKnowledgeBases: KnowledgeBaseSummary[];
  onUpdate: () => void;
}

function KnowledgeTab({ agentId, connectedKnowledgeBases, onUpdate }: KnowledgeTabProps) {
  const [allKnowledgeBases, setAllKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadKnowledgeBases();
  }, []);

  useEffect(() => {
    setSelectedIds(new Set(connectedKnowledgeBases.map(kb => kb.id)));
  }, [connectedKnowledgeBases]);

  const loadKnowledgeBases = async () => {
    try {
      setIsLoading(true);
      const kbs = await knowledgeService.getAll();
      setAllKnowledgeBases(kbs);
    } catch {
      setError('Failed to load knowledge bases');
    } finally {
      setIsLoading(false);
    }
  };

  const handleToggle = (id: number) => {
    setSelectedIds(prev => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  };

  const handleSave = async () => {
    setIsSaving(true);
    setError(null);
    try {
      await agentService.updateKnowledgeBases(agentId, Array.from(selectedIds));
      onUpdate();
    } catch {
      setError('Failed to update knowledge bases');
    } finally {
      setIsSaving(false);
    }
  };

  const hasChanges = () => {
    const currentIds = new Set(connectedKnowledgeBases.map(kb => kb.id));
    if (currentIds.size !== selectedIds.size) return true;
    for (const id of selectedIds) {
      if (!currentIds.has(id)) return true;
    }
    return false;
  };

  if (isLoading) {
    return (
      <div className="flex justify-center py-8">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
      <div className="flex justify-between items-center mb-4">
        <div>
          <h3 className="text-lg font-medium text-gray-900 dark:text-white">
            Connected Knowledge Bases
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Select knowledge bases for RAG-enhanced responses
          </p>
        </div>
        {hasChanges() && (
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {isSaving ? 'Saving...' : 'Save Changes'}
          </button>
        )}
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 rounded-lg">
          {error}
        </div>
      )}

      {allKnowledgeBases.length === 0 ? (
        <div className="text-center py-8 text-gray-500 dark:text-gray-400">
          <div className="text-4xl mb-2">📚</div>
          <p className="mb-4">No knowledge bases available</p>
          <Link
            to="/knowledge"
            className="text-primary-600 hover:text-primary-700"
          >
            Create your first knowledge base
          </Link>
        </div>
      ) : (
        <div className="space-y-2">
          {allKnowledgeBases.map((kb) => (
            <div
              key={kb.id}
              onClick={() => handleToggle(kb.id)}
              className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                selectedIds.has(kb.id)
                  ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                  : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
              }`}
            >
              <div className="flex items-start">
                <div className="flex-shrink-0">
                  <div
                    className={`w-5 h-5 rounded border-2 flex items-center justify-center ${
                      selectedIds.has(kb.id)
                        ? 'border-primary-600 bg-primary-600'
                        : 'border-gray-300 dark:border-gray-600'
                    }`}
                  >
                    {selectedIds.has(kb.id) && (
                      <svg className="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                        <path
                          fillRule="evenodd"
                          d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                          clipRule="evenodd"
                        />
                      </svg>
                    )}
                  </div>
                </div>
                <div className="ml-3 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-900 dark:text-white">
                      {kb.name}
                    </span>
                    <span className="px-2 py-0.5 text-xs rounded-full bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400">
                      {kb.documentCount || 0} documents
                    </span>
                  </div>
                  {kb.description && (
                    <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                      {kb.description}
                    </p>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function AgentEditPage() {
  const { id } = useParams<{ id: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = (searchParams.get('tab') as TabType) || 'settings';

  const { selectedAgent, fetchAgent, updateAgent, isLoading, error } = useAgentStore();

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    systemPrompt: '',
    temperature: 0.7,
    maxTokens: 2048,
  });
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (id) {
      fetchAgent(parseInt(id));
    }
  }, [id, fetchAgent]);

  useEffect(() => {
    if (selectedAgent) {
      setFormData({
        name: selectedAgent.name || '',
        description: selectedAgent.description || '',
        systemPrompt: selectedAgent.systemPrompt || '',
        temperature: selectedAgent.temperature || 0.7,
        maxTokens: selectedAgent.maxTokens || 2048,
      });
    }
  }, [selectedAgent]);

  const handleTabChange = (tab: TabType) => {
    setSearchParams({ tab });
  };

  const handleSave = async () => {
    if (!id || !selectedAgent) return;
    setIsSaving(true);
    try {
      await updateAgent(parseInt(id), formData);
    } catch {
      // Error handled by store
    } finally {
      setIsSaving(false);
    }
  };

  const handleRefresh = () => {
    if (id) {
      fetchAgent(parseInt(id));
    }
  };

  if (isLoading && !selectedAgent) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (!selectedAgent) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-500 dark:text-gray-400">Agent not found</p>
        <Link to="/agents" className="text-primary-600 hover:text-primary-700 mt-2 inline-block">
          Back to Agents
        </Link>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center">
          <Link
            to="/agents"
            className="mr-4 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
              {selectedAgent.name}
            </h1>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {selectedAgent.modelName || 'No model configured'}
            </p>
          </div>
        </div>
        {activeTab === 'settings' && (
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {isSaving ? 'Saving...' : 'Save Changes'}
          </button>
        )}
      </div>

      {error && (
        <div className="mb-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {/* Tabs */}
      <div className="border-b border-gray-200 dark:border-gray-700 mb-6">
        <nav className="flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => handleTabChange(tab.id)}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === tab.id
                  ? 'border-primary-500 text-primary-600 dark:text-primary-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              {tab.label}
              {tab.id === 'functions' && selectedAgent.functions && selectedAgent.functions.length > 0 && (
                <span className="ml-2 px-2 py-0.5 text-xs rounded-full bg-primary-100 text-primary-600 dark:bg-primary-900/30 dark:text-primary-400">
                  {selectedAgent.functions.length}
                </span>
              )}
              {tab.id === 'knowledge' && selectedAgent.knowledgeBases && selectedAgent.knowledgeBases.length > 0 && (
                <span className="ml-2 px-2 py-0.5 text-xs rounded-full bg-primary-100 text-primary-600 dark:bg-primary-900/30 dark:text-primary-400">
                  {selectedAgent.knowledgeBases.length}
                </span>
              )}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      <div className="flex-1 overflow-hidden">
        {activeTab === 'settings' && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Name
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Description
              </label>
              <input
                type="text"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                System Prompt
              </label>
              <textarea
                rows={6}
                value={formData.systemPrompt}
                onChange={(e) => setFormData({ ...formData, systemPrompt: e.target.value })}
                className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="You are a helpful AI assistant..."
              />
            </div>

            <div className="grid grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Temperature: {formData.temperature}
                </label>
                <input
                  type="range"
                  min="0"
                  max="2"
                  step="0.1"
                  value={formData.temperature}
                  onChange={(e) => setFormData({ ...formData, temperature: parseFloat(e.target.value) })}
                  className="w-full"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Max Tokens
                </label>
                <input
                  type="number"
                  value={formData.maxTokens}
                  onChange={(e) => setFormData({ ...formData, maxTokens: parseInt(e.target.value) })}
                  className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>
            </div>
          </div>
        )}

        {activeTab === 'functions' && (
          <FunctionsTab
            agentId={selectedAgent.id}
            connectedFunctions={selectedAgent.functions || []}
            onUpdate={handleRefresh}
          />
        )}

        {activeTab === 'knowledge' && (
          <KnowledgeTab
            agentId={selectedAgent.id}
            connectedKnowledgeBases={selectedAgent.knowledgeBases || []}
            onUpdate={handleRefresh}
          />
        )}

        {activeTab === 'test' && (
          <div className="h-full min-h-[500px]">
            {selectedAgent.llmServerId && selectedAgent.modelName ? (
              <ChatInterface
                agentId={selectedAgent.id}
                agentName={selectedAgent.name}
              />
            ) : (
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                <div className="text-center py-8 text-gray-500 dark:text-gray-400">
                  <div className="text-4xl mb-2">⚠️</div>
                  <p className="mb-4">Please configure an LLM server and model in the Settings tab first.</p>
                  <button
                    onClick={() => handleTabChange('settings')}
                    className="text-primary-600 hover:text-primary-700"
                  >
                    Go to Settings
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
