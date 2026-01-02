import api from './api';

export interface FunctionSummary {
  id: number;
  name: string;
}

export interface KnowledgeBaseSummary {
  id: number;
  name: string;
}

export interface Agent {
  id: number;
  name: string;
  slug: string | null;
  description: string | null;
  systemPrompt: string | null;
  llmServerId: number | null;
  llmServerName: string | null;
  modelName: string | null;
  temperature: number;
  maxTokens: number;
  isActive: boolean;
  functions?: FunctionSummary[];
  knowledgeBases?: KnowledgeBaseSummary[];
  createdAt: string;
  updatedAt: string;
}

export interface AgentCreateRequest {
  name: string;
  description?: string;
  systemPrompt?: string;
  llmServerId?: number;
  modelName?: string;
  temperature?: number;
  maxTokens?: number;
  functionIds?: number[];
  knowledgeBaseIds?: number[];
}

export interface AgentUpdateRequest extends AgentCreateRequest {
  isActive?: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const agentService = {
  getAll: async (): Promise<Agent[]> => {
    const response = await api.get<Agent[]>('/agents');
    return response.data;
  },

  getPaged: async (page = 0, size = 10): Promise<PageResponse<Agent>> => {
    const response = await api.get<PageResponse<Agent>>('/agents/paged', {
      params: { page, size },
    });
    return response.data;
  },

  getById: async (id: number): Promise<Agent> => {
    const response = await api.get<Agent>(`/agents/${id}`);
    return response.data;
  },

  search: async (query: string): Promise<Agent[]> => {
    const response = await api.get<Agent[]>('/agents/search', {
      params: { q: query },
    });
    return response.data;
  },

  create: async (data: AgentCreateRequest): Promise<Agent> => {
    const response = await api.post<Agent>('/agents', data);
    return response.data;
  },

  update: async (id: number, data: AgentUpdateRequest): Promise<Agent> => {
    const response = await api.put<Agent>(`/agents/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/agents/${id}`);
  },

  duplicate: async (id: number): Promise<Agent> => {
    const response = await api.post<Agent>(`/agents/${id}/duplicate`);
    return response.data;
  },

  toggleActive: async (id: number): Promise<Agent> => {
    const response = await api.patch<Agent>(`/agents/${id}/toggle-active`);
    return response.data;
  },

  generateSlug: async (id: number): Promise<Agent> => {
    const response = await api.post<Agent>(`/agents/${id}/generate-slug`);
    return response.data;
  },

  getAgent: async (id: number): Promise<Agent> => {
    const response = await api.get<Agent>(`/agents/${id}`);
    return response.data;
  },

  updateFunctions: async (id: number, functionIds: number[]): Promise<Agent> => {
    const response = await api.put<Agent>(`/agents/${id}/functions`, functionIds);
    return response.data;
  },

  updateKnowledgeBases: async (id: number, knowledgeBaseIds: number[]): Promise<Agent> => {
    const response = await api.put<Agent>(`/agents/${id}/knowledge-bases`, knowledgeBaseIds);
    return response.data;
  },
};

export { agentService };
export default agentService;
