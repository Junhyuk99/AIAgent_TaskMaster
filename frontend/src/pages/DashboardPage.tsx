import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAgentStore } from '../stores/agentStore';
import { useAuthStore } from '../stores/authStore';

export default function DashboardPage() {
  const { t } = useTranslation();
  const { agents, fetchAgents } = useAgentStore();
  const { user } = useAuthStore();

  useEffect(() => {
    fetchAgents();
  }, [fetchAgents]);

  const activeAgents = agents.filter((a) => a.isActive).length;

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          {t('dashboard.welcomeBack')}{user?.name ? `, ${user.name}` : ''}!
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          {t('dashboard.overviewDescription')}
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <Link to="/agents" className="bg-white dark:bg-gray-800 overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900 rounded-lg flex items-center justify-center">
                  <span className="text-2xl">🤖</span>
                </div>
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 dark:text-gray-400 truncate">
                    {t('dashboard.totalAgents')}
                  </dt>
                  <dd className="text-lg font-semibold text-gray-900 dark:text-white">{agents.length}</dd>
                </dl>
              </div>
            </div>
          </div>
        </Link>

        <div className="bg-white dark:bg-gray-800 overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <div className="w-12 h-12 bg-green-100 dark:bg-green-900 rounded-lg flex items-center justify-center">
                  <span className="text-2xl">✅</span>
                </div>
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 dark:text-gray-400 truncate">
                    {t('dashboard.activeAgents')}
                  </dt>
                  <dd className="text-lg font-semibold text-gray-900 dark:text-white">{activeAgents}</dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <Link to="/functions" className="bg-white dark:bg-gray-800 overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <div className="w-12 h-12 bg-blue-100 dark:bg-blue-900 rounded-lg flex items-center justify-center">
                  <span className="text-2xl">⚡</span>
                </div>
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 dark:text-gray-400 truncate">
                    {t('nav.functions')}
                  </dt>
                  <dd className="text-lg font-semibold text-gray-900 dark:text-white">0</dd>
                </dl>
              </div>
            </div>
          </div>
        </Link>

        <Link to="/knowledge" className="bg-white dark:bg-gray-800 overflow-hidden shadow rounded-lg hover:shadow-md transition-shadow">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <div className="w-12 h-12 bg-purple-100 dark:bg-purple-900 rounded-lg flex items-center justify-center">
                  <span className="text-2xl">📚</span>
                </div>
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 dark:text-gray-400 truncate">
                    {t('nav.knowledge')}
                  </dt>
                  <dd className="text-lg font-semibold text-gray-900 dark:text-white">0</dd>
                </dl>
              </div>
            </div>
          </div>
        </Link>
      </div>

      {agents.length > 0 ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">{t('dashboard.recentAgents')}</h2>
            <Link to="/agents" className="text-sm text-primary-600 hover:text-primary-700 dark:text-primary-400">
              {t('dashboard.viewAll')}
            </Link>
          </div>
          <div className="space-y-3">
            {agents.slice(0, 5).map((agent) => (
              <Link
                key={agent.id}
                to={`/agents/${agent.id}/edit`}
                className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
              >
                <div className="flex items-center">
                  <div className="w-10 h-10 bg-primary-100 dark:bg-primary-900 rounded-lg flex items-center justify-center mr-3">
                    <span className="text-xl">🤖</span>
                  </div>
                  <div>
                    <h3 className="font-medium text-gray-900 dark:text-white">{agent.name}</h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {agent.modelName || t('dashboard.noModelConfigured')}
                    </p>
                  </div>
                </div>
                <span
                  className={`px-2 py-1 rounded-full text-xs font-medium ${
                    agent.isActive
                      ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                      : 'bg-gray-100 text-gray-600 dark:bg-gray-600 dark:text-gray-400'
                  }`}
                >
                  {agent.isActive ? t('common.active') : t('common.inactive')}
                </span>
              </Link>
            ))}
          </div>
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">{t('dashboard.quickStart')}</h2>
          <div className="space-y-3">
            <Link
              to="/settings/llm"
              className="flex items-center text-gray-600 dark:text-gray-400 hover:text-primary-600 dark:hover:text-primary-400"
            >
              <span className="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900 text-primary-600 dark:text-primary-400 flex items-center justify-center text-sm mr-3">
                1
              </span>
              <span>{t('dashboard.quickStart1')}</span>
            </Link>
            <Link
              to="/agents"
              className="flex items-center text-gray-600 dark:text-gray-400 hover:text-primary-600 dark:hover:text-primary-400"
            >
              <span className="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900 text-primary-600 dark:text-primary-400 flex items-center justify-center text-sm mr-3">
                2
              </span>
              <span>{t('dashboard.quickStart2')}</span>
            </Link>
            <Link
              to="/functions"
              className="flex items-center text-gray-600 dark:text-gray-400 hover:text-primary-600 dark:hover:text-primary-400"
            >
              <span className="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900 text-primary-600 dark:text-primary-400 flex items-center justify-center text-sm mr-3">
                3
              </span>
              <span>{t('dashboard.quickStart3')}</span>
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
