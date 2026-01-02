import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useAgentStore, useFilteredAgents } from '../stores/agentStore';
import { AgentCard, CreateAgentModal, DeleteConfirmModal } from '../components/agents';
import AgentVersionHistory from '../components/agents/AgentVersionHistory';
import { LoadingSpinner } from '../components/ui';
import type { Agent } from '../services/agentService';

export default function AgentsPage() {
  const { t } = useTranslation();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [deleteAgentId, setDeleteAgentId] = useState<number | null>(null);
  const [versionHistoryAgent, setVersionHistoryAgent] = useState<Agent | null>(null);

  const {
    fetchAgents,
    deleteAgent,
    duplicateAgent,
    toggleAgentActive,
    isLoading,
    error,
    viewMode,
    setViewMode,
    searchQuery,
    setSearchQuery,
  } = useAgentStore();

  const filteredAgents = useFilteredAgents();

  useEffect(() => {
    fetchAgents();
  }, [fetchAgents]);

  const handleDelete = async () => {
    if (deleteAgentId) {
      try {
        await deleteAgent(deleteAgentId);
        setDeleteAgentId(null);
      } catch {
        // Error handled by store
      }
    }
  };

  const handleDuplicate = async (id: number) => {
    try {
      await duplicateAgent(id);
    } catch {
      // Error handled by store
    }
  };

  const handleToggleActive = async (id: number) => {
    try {
      await toggleAgentActive(id);
    } catch {
      // Error handled by store
    }
  };

  if (isLoading && filteredAgents.length === 0) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('agents.title')}</h1>
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <div className="relative flex-1 sm:flex-initial">
            <input
              type="text"
              placeholder={t('agents.searchAgents')}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full sm:w-64 px-4 py-2 pl-10 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <svg
              className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <div className="flex items-center border border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-2 ${viewMode === 'grid' ? 'bg-primary-100 dark:bg-primary-900 text-primary-600' : 'bg-white dark:bg-gray-700 text-gray-600 dark:text-gray-400'}`}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
              </svg>
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 ${viewMode === 'list' ? 'bg-primary-100 dark:bg-primary-900 text-primary-600' : 'bg-white dark:bg-gray-700 text-gray-600 dark:text-gray-400'}`}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" />
              </svg>
            </button>
          </div>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors whitespace-nowrap"
          >
            {t('agents.newAgent')}
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {filteredAgents.length === 0 ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow">
          <div className="p-6 text-center text-gray-500 dark:text-gray-400">
            <div className="text-5xl mb-4">🤖</div>
            <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
              {searchQuery ? t('agents.noAgentsFound') : t('agents.noAgentsYet')}
            </h3>
            <p className="mb-4">
              {searchQuery ? t('agents.tryDifferentSearch') : t('agents.createFirstToStart')}
            </p>
            {!searchQuery && (
              <button
                onClick={() => setIsCreateModalOpen(true)}
                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
              >
                {t('agents.createAgent')}
              </button>
            )}
          </div>
        </div>
      ) : (
        <div className={viewMode === 'grid' ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6' : 'space-y-4'}>
          {filteredAgents.map((agent) => (
            <AgentCard
              key={agent.id}
              agent={agent}
              onDelete={setDeleteAgentId}
              onDuplicate={handleDuplicate}
              onToggleActive={handleToggleActive}
              onVersionHistory={setVersionHistoryAgent}
            />
          ))}
        </div>
      )}

      <CreateAgentModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
      />

      <DeleteConfirmModal
        isOpen={deleteAgentId !== null}
        title={t('agents.deleteAgent')}
        message={t('agents.deleteWarning')}
        onConfirm={handleDelete}
        onCancel={() => setDeleteAgentId(null)}
        isLoading={isLoading}
      />

      {versionHistoryAgent && (
        <AgentVersionHistory
          agentId={versionHistoryAgent.id}
          agentName={versionHistoryAgent.name}
          isOpen={versionHistoryAgent !== null}
          onClose={() => setVersionHistoryAgent(null)}
          onRollback={() => fetchAgents()}
        />
      )}
    </div>
  );
}
