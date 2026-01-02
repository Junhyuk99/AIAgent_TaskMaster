import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { usageService } from '../services/usageService';
import type { UsageStatistics, Period, DailyStats, AgentStats, ModelStats } from '../services/usageService';

const PERIOD_OPTIONS: { value: Period; labelKey: string }[] = [
  { value: 'DAY', labelKey: 'usage.period.day' },
  { value: 'WEEK', labelKey: 'usage.period.week' },
  { value: 'MONTH', labelKey: 'usage.period.month' },
  { value: 'QUARTER', labelKey: 'usage.period.quarter' },
  { value: 'YEAR', labelKey: 'usage.period.year' },
];

function StatCard({ title, value, subtitle, icon, trend }: {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  trend?: { value: number; isPositive: boolean };
}) {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500 dark:text-gray-400">{title}</p>
          <p className="text-2xl font-bold text-gray-900 dark:text-white mt-1">{value}</p>
          {subtitle && (
            <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">{subtitle}</p>
          )}
        </div>
        <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900/30 rounded-lg flex items-center justify-center text-primary-600 dark:text-primary-400">
          {icon}
        </div>
      </div>
      {trend && (
        <div className={`mt-2 flex items-center text-sm ${trend.isPositive ? 'text-green-600' : 'text-red-600'}`}>
          <span>{trend.isPositive ? '↑' : '↓'} {Math.abs(trend.value)}%</span>
          <span className="text-gray-400 ml-1">vs previous period</span>
        </div>
      )}
    </div>
  );
}

function SimpleBarChart({ data, valueKey, labelKey, maxBars = 7 }: {
  data: DailyStats[];
  valueKey: keyof DailyStats;
  labelKey: keyof DailyStats;
  maxBars?: number;
}) {
  const displayData = data.slice(-maxBars);
  const maxValue = Math.max(...displayData.map(d => Number(d[valueKey]) || 0), 1);

  return (
    <div className="h-48 flex items-end justify-between gap-2">
      {displayData.map((item, index) => {
        const value = Number(item[valueKey]) || 0;
        const height = (value / maxValue) * 100;
        const date = new Date(item[labelKey] as string);
        const label = date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });

        return (
          <div key={index} className="flex-1 flex flex-col items-center">
            <div className="w-full flex flex-col items-center justify-end h-40">
              <span className="text-xs text-gray-500 dark:text-gray-400 mb-1">
                {value.toLocaleString()}
              </span>
              <div
                className="w-full bg-primary-500 rounded-t transition-all duration-300 min-h-[4px]"
                style={{ height: `${Math.max(height, 2)}%` }}
              />
            </div>
            <span className="text-xs text-gray-500 dark:text-gray-400 mt-2">{label}</span>
          </div>
        );
      })}
    </div>
  );
}

function AgentRankingTable({ agents }: { agents: AgentStats[] }) {
  if (agents.length === 0) {
    return (
      <p className="text-gray-500 dark:text-gray-400 text-center py-8">
        No agent usage data available
      </p>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-gray-700">
            <th className="pb-3 font-medium">Agent</th>
            <th className="pb-3 font-medium text-right">API Calls</th>
            <th className="pb-3 font-medium text-right">Conversations</th>
            <th className="pb-3 font-medium text-right">Tokens</th>
            <th className="pb-3 font-medium text-right">Avg Response</th>
          </tr>
        </thead>
        <tbody>
          {agents.map((agent, index) => (
            <tr key={agent.agentId} className="border-b border-gray-100 dark:border-gray-700/50">
              <td className="py-3">
                <div className="flex items-center gap-3">
                  <span className="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400 flex items-center justify-center text-xs font-medium">
                    {index + 1}
                  </span>
                  <span className="text-gray-900 dark:text-white font-medium">{agent.agentName}</span>
                </div>
              </td>
              <td className="py-3 text-right text-gray-600 dark:text-gray-400">
                {agent.apiCalls.toLocaleString()}
              </td>
              <td className="py-3 text-right text-gray-600 dark:text-gray-400">
                {agent.conversations.toLocaleString()}
              </td>
              <td className="py-3 text-right text-gray-600 dark:text-gray-400">
                {agent.tokens.toLocaleString()}
              </td>
              <td className="py-3 text-right text-gray-600 dark:text-gray-400">
                {agent.avgResponseTimeMs.toFixed(0)}ms
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ModelUsageChart({ models }: { models: ModelStats[] }) {
  if (models.length === 0) {
    return (
      <p className="text-gray-500 dark:text-gray-400 text-center py-8">
        No model usage data available
      </p>
    );
  }

  const totalRequests = models.reduce((sum, m) => sum + m.requestCount, 0);

  return (
    <div className="space-y-4">
      {models.map((model) => {
        const percentage = totalRequests > 0 ? (model.requestCount / totalRequests) * 100 : 0;
        return (
          <div key={model.modelName}>
            <div className="flex justify-between text-sm mb-1">
              <span className="text-gray-900 dark:text-white font-medium">{model.modelName}</span>
              <span className="text-gray-500 dark:text-gray-400">
                {model.requestCount.toLocaleString()} requests ({percentage.toFixed(1)}%)
              </span>
            </div>
            <div className="h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
              <div
                className="h-full bg-primary-500 rounded-full transition-all duration-300"
                style={{ width: `${percentage}%` }}
              />
            </div>
            <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">
              {model.totalTokens.toLocaleString()} tokens
            </p>
          </div>
        );
      })}
    </div>
  );
}

export default function UsageDashboardPage() {
  const { t } = useTranslation();
  const [statistics, setStatistics] = useState<UsageStatistics | null>(null);
  const [period, setPeriod] = useState<Period>('WEEK');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isExporting, setIsExporting] = useState(false);

  useEffect(() => {
    loadStatistics();
  }, [period]);

  const loadStatistics = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const stats = await usageService.getStatistics(period);
      setStatistics(stats);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load statistics');
    } finally {
      setIsLoading(false);
    }
  };

  const handleExport = async () => {
    try {
      setIsExporting(true);
      await usageService.downloadExport(period);
    } catch (err) {
      console.error('Export failed:', err);
    } finally {
      setIsExporting(false);
    }
  };

  const formatNumber = (num: number): string => {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toLocaleString();
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <div className="bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 p-4 rounded-lg">
          {error}
        </div>
      </div>
    );
  }

  const overview = statistics?.overview;

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{t('usage.title')}</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            {t('usage.subtitle')}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <select
            value={period}
            onChange={(e) => setPeriod(e.target.value as Period)}
            className="px-4 py-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg text-sm focus:ring-2 focus:ring-primary-500"
          >
            {PERIOD_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{t(opt.labelKey)}</option>
            ))}
          </select>
          <button
            onClick={handleExport}
            disabled={isExporting}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm hover:bg-primary-700 disabled:opacity-50 flex items-center gap-2"
          >
            {isExporting ? (
              <>
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                {t('usage.exporting')}
              </>
            ) : (
              <>
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
                {t('usage.exportCsv')}
              </>
            )}
          </button>
        </div>
      </div>

      {/* Overview Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title={t('usage.totalApiCalls')}
          value={formatNumber(overview?.totalApiCalls || 0)}
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          }
        />
        <StatCard
          title={t('usage.totalConversations')}
          value={formatNumber(overview?.totalConversations || 0)}
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
          }
        />
        <StatCard
          title={t('usage.totalTokens')}
          value={formatNumber(overview?.totalTokens || 0)}
          subtitle={`${t('usage.inputTokens')}: ${formatNumber(overview?.inputTokens || 0)} / ${t('usage.outputTokens')}: ${formatNumber(overview?.outputTokens || 0)}`}
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
          }
        />
        <StatCard
          title={t('usage.errorRate')}
          value={`${overview?.errorRate?.toFixed(2) || 0}%`}
          subtitle={`${overview?.errorCount || 0} ${t('usage.errors')}`}
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          }
        />
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Daily Usage Chart */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            {t('usage.dailyMessages')}
          </h3>
          {statistics?.dailyStats && statistics.dailyStats.length > 0 ? (
            <SimpleBarChart
              data={statistics.dailyStats}
              valueKey="messages"
              labelKey="date"
            />
          ) : (
            <p className="text-gray-500 dark:text-gray-400 text-center py-8">
              {t('usage.noData')}
            </p>
          )}
        </div>

        {/* Token Usage Chart */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            {t('usage.dailyTokens')}
          </h3>
          {statistics?.dailyStats && statistics.dailyStats.length > 0 ? (
            <SimpleBarChart
              data={statistics.dailyStats}
              valueKey="tokens"
              labelKey="date"
            />
          ) : (
            <p className="text-gray-500 dark:text-gray-400 text-center py-8">
              {t('usage.noData')}
            </p>
          )}
        </div>
      </div>

      {/* Agent Ranking & Model Usage */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Agent Ranking */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            {t('usage.topAgents')}
          </h3>
          <AgentRankingTable agents={statistics?.agentStats || []} />
        </div>

        {/* Model Usage */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            {t('usage.modelUsage')}
          </h3>
          <ModelUsageChart models={statistics?.modelStats || []} />
        </div>
      </div>
    </div>
  );
}
