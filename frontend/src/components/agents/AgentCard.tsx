import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import type { Agent } from '../../services/agentService';

interface AgentCardProps {
  agent: Agent;
  onDelete: (id: number) => void;
  onDuplicate: (id: number) => void;
  onToggleActive: (id: number) => void;
  onVersionHistory: (agent: Agent) => void;
}

export default function AgentCard({ agent, onDelete, onDuplicate, onToggleActive, onVersionHistory }: AgentCardProps) {
  const { t } = useTranslation();
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow hover:shadow-md transition-shadow overflow-hidden">
      <div className="p-5">
        <div className="flex items-start justify-between">
          <div className="flex items-center">
            <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900 rounded-lg flex items-center justify-center">
              <span className="text-2xl">🤖</span>
            </div>
            <div className="ml-4">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                {agent.name}
              </h3>
              {agent.modelName && (
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  {agent.modelName}
                </p>
              )}
            </div>
          </div>
          <button
            onClick={() => onToggleActive(agent.id)}
            className={`px-2 py-1 rounded-full text-xs font-medium ${
              agent.isActive
                ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
            }`}
          >
            {agent.isActive ? t('common.active') : t('common.inactive')}
          </button>
        </div>

        {agent.description && (
          <p className="mt-3 text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
            {agent.description}
          </p>
        )}

        <div className="mt-4 flex flex-wrap gap-2">
          {agent.functions && agent.functions.length > 0 && (
            <span className="inline-flex items-center px-2 py-1 rounded-md text-xs bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
              ⚡ {agent.functions.length} function{agent.functions.length > 1 ? 's' : ''}
            </span>
          )}
          {agent.knowledgeBases && agent.knowledgeBases.length > 0 && (
            <span className="inline-flex items-center px-2 py-1 rounded-md text-xs bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200">
              📚 {agent.knowledgeBases.length} KB{agent.knowledgeBases.length > 1 ? 's' : ''}
            </span>
          )}
        </div>
      </div>

      <div className="bg-gray-50 dark:bg-gray-700/50 px-5 py-3 flex justify-between items-center">
        <Link
          to={`/agents/${agent.id}/edit`}
          className="inline-flex items-center px-3 py-1.5 bg-primary-600 text-white text-sm font-medium rounded-md hover:bg-primary-700 transition-colors"
        >
          <svg className="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          {t('agents.agentSettings')}
        </Link>
        <div className="flex items-center space-x-3">
          <button
            onClick={() => onVersionHistory(agent)}
            className="text-sm text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white"
            title={t('agents.version.history')}
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </button>
          <button
            onClick={() => onDuplicate(agent.id)}
            className="text-sm text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white"
            title={t('agents.duplicateAgent')}
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
            </svg>
          </button>
          <button
            onClick={() => onDelete(agent.id)}
            className="text-sm text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
            title={t('agents.deleteAgent')}
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
}
