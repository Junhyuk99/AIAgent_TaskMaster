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

export interface FunctionExecution {
  functionName: string;
  arguments: Record<string, unknown>;
  result?: string;
  error?: string;
  executionTimeMs: number;
}

export interface AgentChatResponse {
  conversationId: string;
  response: string;
  model: string;
  agentId: number;
  agentName: string;
  sources?: DocumentSource[];
  functionExecutions?: FunctionExecution[];
}

export interface StreamEvent {
  type: 'text' | 'function_call' | 'function_result' | 'sources' | 'function_executions' | 'done';
  content?: string;
  name?: string;
  arguments?: Record<string, unknown>;
  success?: boolean;
  executionTimeMs?: number;
  data?: DocumentSource[] | FunctionExecution[];
}

export interface StreamCallbacks {
  onText?: (text: string) => void;
  onFunctionCall?: (name: string, args: Record<string, unknown>) => void;
  onFunctionResult?: (name: string, success: boolean, executionTimeMs: number) => void;
  onSources?: (sources: DocumentSource[]) => void;
  onFunctionExecutions?: (executions: FunctionExecution[]) => void;
  onError?: (error: Error) => void;
  onComplete?: () => void;
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
    // Legacy simple streaming - just extract text
    return chatService.chatStreamWithEvents(agentId, request, {
      onText: onChunk,
      onError,
      onComplete,
    });
  },

  chatStreamWithEvents: async (
    agentId: number,
    request: AgentChatRequest,
    callbacks: StreamCallbacks
  ): Promise<void> => {
    const token = localStorage.getItem('token');
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
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        buffer += chunk;

        // Process complete lines from buffer
        const lines = buffer.split('\n');
        // Keep the last incomplete line in buffer
        buffer = lines.pop() || '';

        for (const line of lines) {
          const trimmedLine = line.trim();
          if (!trimmedLine) continue;

          let data = '';

          if (trimmedLine.startsWith('data:')) {
            data = trimmedLine.slice(5).trim();
          } else if (trimmedLine.startsWith('{') && trimmedLine.endsWith('}')) {
            // Handle case where JSON comes on its own line (after empty data:)
            data = trimmedLine;
          }

          if (!data) continue;

          // Try to parse as JSON event
          try {
            const event = JSON.parse(data) as StreamEvent;

            switch (event.type) {
              case 'text':
                if (event.content && callbacks.onText) {
                  callbacks.onText(event.content);
                }
                break;
              case 'function_call':
                if (event.name && callbacks.onFunctionCall) {
                  callbacks.onFunctionCall(event.name, event.arguments || {});
                }
                break;
              case 'function_result':
                if (event.name && callbacks.onFunctionResult) {
                  callbacks.onFunctionResult(event.name, event.success || false, event.executionTimeMs || 0);
                }
                break;
              case 'sources':
                if (event.data && callbacks.onSources) {
                  callbacks.onSources(event.data as DocumentSource[]);
                }
                break;
              case 'function_executions':
                if (event.data && callbacks.onFunctionExecutions) {
                  callbacks.onFunctionExecutions(event.data as FunctionExecution[]);
                }
                break;
              case 'done':
                callbacks.onComplete?.();
                return;
            }
          } catch {
            // Not JSON - treat as plain text (legacy format)
            if (callbacks.onText) {
              callbacks.onText(data);
            }
          }
        }
      }

      callbacks.onComplete?.();
    } catch (error) {
      callbacks.onError?.(error as Error);
    }
  },
};

export default chatService;
