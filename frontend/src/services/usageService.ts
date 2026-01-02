import api from './api';

export type Period = 'DAY' | 'WEEK' | 'MONTH' | 'QUARTER' | 'YEAR';

export interface OverviewStats {
  totalApiCalls: number;
  totalConversations: number;
  totalMessages: number;
  totalTokens: number;
  inputTokens: number;
  outputTokens: number;
  errorCount: number;
  errorRate: number;
  avgResponseTimeMs: number;
}

export interface DailyStats {
  date: string;
  apiCalls: number;
  conversations: number;
  messages: number;
  tokens: number;
}

export interface AgentStats {
  agentId: number;
  agentName: string;
  apiCalls: number;
  conversations: number;
  tokens: number;
  avgResponseTimeMs: number;
}

export interface ModelStats {
  modelName: string;
  requestCount: number;
  totalTokens: number;
}

export interface UsageStatistics {
  overview: OverviewStats;
  dailyStats: DailyStats[];
  agentStats: AgentStats[];
  modelStats: ModelStats[];
}

export const usageService = {
  getStatistics: async (period: Period = 'WEEK'): Promise<UsageStatistics> => {
    const response = await api.get<UsageStatistics>('/usage/statistics', {
      params: { period },
    });
    return response.data;
  },

  getOverview: async (): Promise<OverviewStats> => {
    const response = await api.get<OverviewStats>('/usage/overview');
    return response.data;
  },

  getAgentRanking: async (period: Period = 'WEEK', limit: number = 10): Promise<AgentStats[]> => {
    const response = await api.get<AgentStats[]>('/usage/agents/ranking', {
      params: { period, limit },
    });
    return response.data;
  },

  exportUsageData: async (period: Period = 'MONTH'): Promise<Blob> => {
    const response = await api.get('/usage/export', {
      params: { period },
      responseType: 'blob',
    });
    return response.data;
  },

  downloadExport: async (period: Period = 'MONTH'): Promise<void> => {
    const blob = await usageService.exportUsageData(period);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `usage-stats-${period.toLowerCase()}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },
};

export default usageService;
