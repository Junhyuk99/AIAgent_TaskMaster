import api from './api';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface AgentChatRequest {
  message: string;
  conversationId?: string;
  history?: ChatMessage[];
}

export interface DocumentSource {
  documentId: string;
  documentName: string;
  score: number;
}

export interface AgentChatResponse {
  conversationId: string;
  response: string;
  model: string;
  agentId: number;
  agentName: string;
  sources?: DocumentSource[];
}

const chatService = {
  chat: async (agentId: number, request: AgentChatRequest): Promise<AgentChatResponse> => {
    const response = await api.post<AgentChatResponse>(`/chat/agent/${agentId}`, request);
    return response.data;
  },

  chatStream: async (
    agentId: number,
    request: AgentChatRequest,
    onChunk: (chunk: string) => void,
    onError?: (error: Error) => void,
    onComplete?: () => void
  ): Promise<void> => {
    const token = localStorage.getItem('token');
    // Use relative path for Docker nginx proxy, fallback to localhost for local dev
    const baseUrl = import.meta.env.VITE_API_URL || '/api';

    try {
      const response = await fetch(
        `${baseUrl}/chat/agent/${agentId}/stream`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(request),
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('Response body is not readable');
      }

      const decoder = new TextDecoder();

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        // Parse SSE format
        const lines = chunk.split('\n');
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim();
            if (data) {
              onChunk(data);
            }
          }
        }
      }

      onComplete?.();
    } catch (error) {
      onError?.(error as Error);
    }
  },
};

export default chatService;
