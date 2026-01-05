import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { ChatMessage, DocumentSource, FunctionExecution } from '../services/chatService';

interface DisplayMessage extends ChatMessage {
  sources?: DocumentSource[];
  functionExecutions?: FunctionExecution[];
}

interface AgentChatState {
  messages: DisplayMessage[];
  conversationId: string | null;
}

interface ChatState {
  // Map of agentId -> chat state
  chats: Record<number, AgentChatState>;

  // Actions
  getMessages: (agentId: number) => DisplayMessage[];
  getConversationId: (agentId: number) => string | null;
  addMessage: (agentId: number, message: DisplayMessage) => void;
  setConversationId: (agentId: number, conversationId: string) => void;
  clearChat: (agentId: number) => void;
  clearAllChats: () => void;
}

export const useChatStore = create<ChatState>()(
  persist(
    (set, get) => ({
  chats: {},

  getMessages: (agentId: number) => {
    return get().chats[agentId]?.messages || [];
  },

  getConversationId: (agentId: number) => {
    return get().chats[agentId]?.conversationId || null;
  },

  addMessage: (agentId: number, message: DisplayMessage) => {
    set((state) => {
      const currentChat = state.chats[agentId] || { messages: [], conversationId: null };
      return {
        chats: {
          ...state.chats,
          [agentId]: {
            ...currentChat,
            messages: [...currentChat.messages, message],
          },
        },
      };
    });
  },

  setConversationId: (agentId: number, conversationId: string) => {
    set((state) => {
      const currentChat = state.chats[agentId] || { messages: [], conversationId: null };
      return {
        chats: {
          ...state.chats,
          [agentId]: {
            ...currentChat,
            conversationId,
          },
        },
      };
    });
  },

  clearChat: (agentId: number) => {
    set((state) => {
      const newChats = { ...state.chats };
      delete newChats[agentId];
      return { chats: newChats };
    });
  },

  clearAllChats: () => {
    set({ chats: {} });
  },
    }),
    {
      name: 'chat-storage',
      partialize: (state) => ({ chats: state.chats }),
    }
  )
);
