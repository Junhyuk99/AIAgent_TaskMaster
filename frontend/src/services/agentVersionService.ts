import api from './api';

export interface AgentVersion {
  id: number;
  agentId: number;
  versionNumber: number;
  name: string;
  description: string | null;
  systemPrompt: string | null;
  modelName: string | null;
  temperature: number | null;
  maxTokens: number | null;
  llmServerId: number | null;
  functionIds: number[];
  knowledgeBaseIds: number[];
  changeSummary: string | null;
  createdByName: string | null;
  createdAt: string;
  isCurrent: boolean;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const agentVersionService = {
  getVersionHistory: async (agentId: number): Promise<AgentVersion[]> => {
    const response = await api.get<AgentVersion[]>(`/agents/${agentId}/versions`);
    return response.data;
  },

  getVersionHistoryPaged: async (
    agentId: number,
    page: number = 0,
    size: number = 10
  ): Promise<PagedResponse<AgentVersion>> => {
    const response = await api.get<PagedResponse<AgentVersion>>(
      `/agents/${agentId}/versions/paged`,
      { params: { page, size } }
    );
    return response.data;
  },

  getVersion: async (agentId: number, versionNumber: number): Promise<AgentVersion> => {
    const response = await api.get<AgentVersion>(`/agents/${agentId}/versions/${versionNumber}`);
    return response.data;
  },

  rollbackToVersion: async (agentId: number, versionNumber: number): Promise<void> => {
    await api.post(`/agents/${agentId}/versions/${versionNumber}/rollback`);
  },

  compareVersions: async (
    agentId: number,
    v1: number,
    v2: number
  ): Promise<{ version1: AgentVersion; version2: AgentVersion }> => {
    const response = await api.get<{ version1: AgentVersion; version2: AgentVersion }>(
      `/agents/${agentId}/versions/compare`,
      { params: { v1, v2 } }
    );
    return response.data;
  },

  getVersionCount: async (agentId: number): Promise<number> => {
    const response = await api.get<{ count: number }>(`/agents/${agentId}/versions/count`);
    return response.data.count;
  },
};
