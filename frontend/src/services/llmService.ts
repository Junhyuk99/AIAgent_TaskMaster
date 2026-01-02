import api from './api';

export type LlmType = 'OLLAMA' | 'VLLM' | 'OPENAI_COMPATIBLE' | 'CUSTOM';

export interface LlmServer {
  id: number;
  name: string;
  type: LlmType;
  baseUrl: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LlmServerRequest {
  name: string;
  type: LlmType;
  baseUrl: string;
  apiKey?: string;
}

export interface ConnectionTestResponse {
  success: boolean;
  message: string;
  responseTimeMs: number;
}

export interface ModelInfo {
  name: string;
  displayName: string;
  size?: number;
  modifiedAt?: string;
}

export const llmService = {
  // Get all LLM servers
  getAllServers: async (): Promise<LlmServer[]> => {
    const response = await api.get('/llm-servers');
    return response.data;
  },

  // Get active LLM servers
  getActiveServers: async (): Promise<LlmServer[]> => {
    const response = await api.get('/llm-servers/active');
    return response.data;
  },

  // Get single server
  getServer: async (id: number): Promise<LlmServer> => {
    const response = await api.get(`/llm-servers/${id}`);
    return response.data;
  },

  // Create new server
  createServer: async (data: LlmServerRequest): Promise<LlmServer> => {
    const response = await api.post('/llm-servers', data);
    return response.data;
  },

  // Update server
  updateServer: async (id: number, data: LlmServerRequest): Promise<LlmServer> => {
    const response = await api.put(`/llm-servers/${id}`, data);
    return response.data;
  },

  // Delete server
  deleteServer: async (id: number): Promise<void> => {
    await api.delete(`/llm-servers/${id}`);
  },

  // Toggle server active status
  toggleActive: async (id: number): Promise<LlmServer> => {
    const response = await api.patch(`/llm-servers/${id}/toggle-active`);
    return response.data;
  },

  // Test connection
  testConnection: async (id: number): Promise<ConnectionTestResponse> => {
    const response = await api.post(`/llm-servers/${id}/test`);
    return response.data;
  },

  // Get models from server
  getModels: async (id: number): Promise<ModelInfo[]> => {
    const response = await api.get(`/llm-servers/${id}/models`);
    return response.data;
  },
};

export default llmService;
