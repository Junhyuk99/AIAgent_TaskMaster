import api from './api';

export interface Message {
  id: number;
  role: 'user' | 'assistant' | 'system' | 'tool';
  content: string;
  tokenCount?: number;
  modelUsed?: string;
  functionCalls?: string;
  sources?: string;
  executionTimeMs?: number;
  createdAt: string;
}

export interface Conversation {
  id: number;
  externalId: string;
  title: string;
  summary?: string;
  agentId: number;
  agentName: string;
  messageCount: number;
  isArchived: boolean;
  createdAt: string;
  updatedAt: string;
  messages?: Message[];
}

export interface ConversationListResponse {
  conversations: Conversation[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const conversationService = {
  // Get all conversations for current user
  getAll: async (): Promise<Conversation[]> => {
    const response = await api.get('/conversations');
    return response.data;
  },

  // Get conversations for a specific agent
  getByAgent: async (agentId: number): Promise<Conversation[]> => {
    const response = await api.get(`/agents/${agentId}/conversations`);
    return response.data;
  },

  // Get paginated conversations for an agent
  getByAgentPaged: async (
    agentId: number,
    page: number = 0,
    size: number = 20
  ): Promise<ConversationListResponse> => {
    const response = await api.get(`/agents/${agentId}/conversations/paged`, {
      params: { page, size },
    });
    return response.data;
  },

  // Get a specific conversation with messages
  getById: async (externalId: string): Promise<Conversation> => {
    const response = await api.get(`/conversations/${externalId}`);
    return response.data;
  },

  // Update conversation title
  updateTitle: async (externalId: string, title: string): Promise<Conversation> => {
    const response = await api.patch(`/conversations/${externalId}`, { title });
    return response.data;
  },

  // Archive a conversation
  archive: async (externalId: string): Promise<void> => {
    await api.post(`/conversations/${externalId}/archive`);
  },

  // Delete a conversation
  delete: async (externalId: string): Promise<void> => {
    await api.delete(`/conversations/${externalId}`);
  },

  // Export conversation as JSON
  export: async (externalId: string): Promise<string> => {
    const response = await api.get(`/conversations/${externalId}/export`);
    return response.data;
  },

  // Download conversation as file
  downloadExport: async (externalId: string): Promise<void> => {
    const json = await conversationService.export(externalId);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `conversation-${externalId}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  },
};

export default conversationService;
